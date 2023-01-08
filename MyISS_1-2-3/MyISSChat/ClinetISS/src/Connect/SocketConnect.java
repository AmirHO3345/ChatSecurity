package Connect;


import Authentication.AuthenticationService;
import Security.AES;
import Security.HMac;
import Security.RSA;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;

public class SocketConnect extends Thread {

    private Socket MySocket ;

    private BufferedReader ResponseServer;

    private BufferedWriter RequestClient;

    private JSONObject DataResponseForRequest;

    private Thread ThreadRequest ;

    private PublicKey ServerPublicKey ;

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
            this.GetPublicKey();
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
            if(AuthenticationService.IsUserRegister()) {
                byte[] MessageEncryption = AES.encrypt(JsonData.toString() ,
                        AuthenticationService.GetUserData().DataSecure.GetKey() ,
                        AuthenticationService.GetUserData().DataSecure.GetIV());
                String DataMac = HMac.Hashing(JsonData.toString() ,
                        AuthenticationService.GetUserData().DataSecure.GetKey() ,
                        AuthenticationService.GetUserData().DataSecure.GetIV());
                JSONObject DataSend = new JSONObject()
                        .put("EncryptionData" , new JSONObject()
                                .put("HMac" , DataMac)
                                .put("Data" , Base64.getEncoder().
                                        encodeToString(MessageEncryption)));
                this.RequestClient.write(DataSend.toString());
            } else {
                JSONObject DataSend = new JSONObject().put("EncryptionData" , new JSONObject()
                    .put("Data" , RSA.Encrypt(JsonData.toString() , this.ServerPublicKey)));
                this.RequestClient.write(DataSend.toString());
            }
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
            System.out.println("\n\n\nYou Have Receive Message : " +
                    DataResponse.getJSONObject("MessageArrived").toString());
            System.out.println("\n\n\n");
        } else {
            synchronized (this) {
                this.DataResponseForRequest = DataResponse ;
                this.ThreadRequest.notify();
            }
        }
    }

    private String ReceiveEncryption(String DataEncryption) {
        String ResultData = "" ;
        byte[] ListByteMessage = Base64.getDecoder().decode(new JSONObject(DataEncryption)
                .getJSONObject("EncryptionData").getString("Data"));
        try {
            ResultData = AES.decrypt(ListByteMessage ,
                    AuthenticationService.GetUserData().DataSecure.GetKey() ,
                    AuthenticationService.GetUserData().DataSecure.GetIV());
        } catch (Exception e) {
            System.out.println("Error In ReceiveEncryption");
        }
        return ResultData ;
    }

    private void GetPublicKey() {
        try {
            String PublicKeyData = this.ResponseServer.readLine() ;
            PublicKeyData = new JSONObject(PublicKeyData).getString("PublicKeyResponse");
            this.ServerPublicKey = RSA
                    .ConvertString2PublicKey(PublicKeyData);
        }catch (Exception e) {
            System.out.println("There Is Error In GetPublicKey Func");
        }
    }

}
