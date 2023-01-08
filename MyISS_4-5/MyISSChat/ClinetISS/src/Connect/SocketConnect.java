package Connect;


import Authentication.AuthenticationService;
import Chat.ViewUsersUI;
import Security.AES;
import Security.CA;
import Security.DigitalSignature;
import Security.RSA;
import com.company.Singleton;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SocketConnect extends Thread {

    private Socket MySocket ;

    private BufferedReader ResponseServer;

    private BufferedWriter RequestClient;

    private JSONObject DataResponseForRequest;

    private Thread ThreadRequest ;

    private X509Certificate ServerCertificate ;

    private boolean IsConnect ;

    public SocketConnect(String ServerName ,int PortNumber) {
        try {
            this.MySocket = new Socket(ServerName , PortNumber) ;
            this.RequestClient = new
                    BufferedWriter(new OutputStreamWriter(this.MySocket.getOutputStream())) ;
            this.ResponseServer = new
                    BufferedReader(new InputStreamReader(this.MySocket.getInputStream()));
            this.DataResponseForRequest = null ;
            this.IsConnect = true ;
            this.GetServerCertificate();
        }catch (Exception e) {
            System.out.println("There is Wrong in Connection Socket");
            this.IsConnect = false ;
        }
    }

    public boolean IsConnected() {
        return this.IsConnect ;
    }

    public JSONObject SendRequest(JSONObject JsonData) {
        if(!this.IsConnect)
            return null ;
        try {
            /* Send Request */
            JSONObject DataSend = null ;
            if(AuthenticationService.IsUserRegister()) {
                byte[] MessageEncryption = AES.encrypt(JsonData.toString() ,
                        AuthenticationService.GetUserData().DataSecureAES.GetKey() ,
                        AuthenticationService.GetUserData().DataSecureAES.GetIV());
                DataSend = new JSONObject()
                        .put("EncryptionData" , new JSONObject()
                                .put("Data" , Base64.getEncoder().
                                        encodeToString(MessageEncryption)));
            } else {
                DataSend = new JSONObject().put("EncryptionData" , new JSONObject()
                    .put("Data" , RSA.Encrypt(JsonData.toString() , this.ServerCertificate.getPublicKey())));
                this.RequestClient.write(DataSend.toString());
            }
            if(AuthenticationService.IsUserRegister() &&
                    AuthenticationService.GetUserData() != null &&
                    AuthenticationService.GetUserData().GetKeys() != null &&
                    AuthenticationService.GetUserData().GetKeys().GetPrivateKey() != null) {
                String DataEncrypt = DataSend.getJSONObject("EncryptionData").getString("Data") ;
                String DataMac = DigitalSignature.GenerateSignature(Base64.getDecoder().decode(DataEncrypt),
                        AuthenticationService.GetUserData().GetKeys().GetPrivateKey());
                DataSend.getJSONObject("EncryptionData")
                        .put("DigitalSignature" , DataMac) ;
            }
            this.RequestClient.write(DataSend.toString());
            this.RequestClient.newLine();
            this.RequestClient.flush();
            synchronized (this) {
                this.ThreadRequest = this ;
                wait();
            }
            /* Receive Response */
            JSONObject ResponseData = this.DataResponseForRequest ;
            this.DataResponseForRequest = null ;
            return ResponseData ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    @Override
    public void run() {
        while(this.IsConnect) {
            try {
                String ResponseData = this.ResponseServer.readLine() ;
                if(AuthenticationService.IsUserRegister())
                    ResponseData = this.ReceiveEncryption(ResponseData);
                this.ProcessResponse(new JSONObject(ResponseData));
            } catch (Exception e) {
                e.printStackTrace();
                if(this.IsConnect) {
                    System.out.println("\nThe Connection has been disconnected by the server");
                    System.exit(0);
                }
            }
        }
    }

    public void Disconnect() {
        try {
            this.IsConnect = false ;
            this.MySocket.shutdownInput() ;
            this.MySocket.shutdownOutput() ;
            this.MySocket.close() ;
            this.RequestClient.close();
            this.ResponseServer.close();
        } catch (Exception e) {
            System.out.println("Something Wrong With Disconnect Socket");
        }
    }

    private void ProcessResponse(JSONObject DataResponse) {
        if(DataResponse.has("MessageArrived")) {
            JSONObject DataReceive = DataResponse
                    .getJSONObject("MessageArrived") ;
            ViewUsersUI.MessageReceive(DataReceive);
        } else if(DataResponse.has("ReceiveSessionFromSender")) {
            JSONObject DataReceive = DataResponse
                    .getJSONObject("ReceiveSessionFromSender") ;
            boolean IsSetUser = ViewUsersUI.SetUserInBack(DataReceive) ;
            if(!IsSetUser)
                System.out.println("Server Try Send Session Key For You " +
                        "Put We Can't Save It");
        } else {
            synchronized (this) {
                this.DataResponseForRequest = DataResponse ;
                this.ThreadRequest.notify();
            }
        }
    }

    private String ReceiveEncryption(String DataEncryption) {
        String ResultData = "" ;
        System.out.println(DataEncryption);
        byte[] ListByteMessage = Base64.getDecoder().decode(new JSONObject(DataEncryption)
                .getJSONObject("EncryptionData").getString("Data"));
        try {
            ResultData = AES.decrypt(ListByteMessage ,
                    AuthenticationService.GetUserData().DataSecureAES.GetKey() ,
                    AuthenticationService.GetUserData().DataSecureAES.GetIV());
        } catch (Exception e) {
            System.out.println("Error In ReceiveEncryption");
        }
        return ResultData ;
    }

    private void GetServerCertificate() {
        try {
            String CertificateData = this.ResponseServer.readLine() ;
            CertificateData = new JSONObject(CertificateData).getString("CertificateServerResponse");
            X509Certificate Certificate = CA.String2Certificate(CertificateData) ;
            if(CA.VerifyCertificate(Certificate , Singleton.ServerDomain)) {
                this.ServerCertificate = Certificate ;
            } else
                System.out.println("The Certificate Is Not Correct ") ;
        }catch (Exception e) {
            System.out.println("There Is Error In GetCertificate Func") ;
        }
    }

}
