package ClientClasses;


import Model.ClientModel;
import Processes.FactoryProcess;
import Processes.ProcessRequest;
import Security.AES;
import Security.RSA;
import ServerClasses.Server;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class ClientHandle extends Thread {

    private final Socket ClientSocket;

    private final Server ClientServer;

    private BufferedReader CommandCome;

    private BufferedWriter ClientSend;

    private ClientModel ClientInfo;

    private boolean IsConnected ;

    private boolean IsHandCheck ;

    public ClientHandle(Socket SocketObject, Server ServerObject) {
        this.ClientSocket = SocketObject;
        this.ClientServer = ServerObject;
        this.ClientInfo = null;
        this.IsHandCheck = false ;
        try {
            this.ClientSend = new BufferedWriter(new OutputStreamWriter(this.ClientSocket.getOutputStream()));
            this.CommandCome = new BufferedReader(new InputStreamReader(this.ClientSocket.getInputStream()));
            System.out.println("Client Connect With Socket : " + this.ClientSocket) ;
            this.IsConnected = true ;
            this.SendPublicKey();
        } catch (IOException ignored) {
            this.IsConnected = false ;
        }
    }

    public void SendResponse(JSONObject DataResponse) {
        try {
            if(this.IsHandCheck) {
                byte[] MessageEncryption = AES.encrypt(DataResponse.toString() ,
                        this.ClientInfo.DataSecure.GetKey() ,
                        this.ClientInfo.DataSecure.GetIV()) ;
                this.ClientSend.write(new JSONObject().put("EncryptionData" , new JSONObject()
                        .put("Data" , Base64.getEncoder()
                                .encodeToString(MessageEncryption))).toString());
            } else {
                this.ClientSend.write(DataResponse.toString());
                this.IsHandCheck = this.ClientInfo != null ;
            }
            this.ClientSend.newLine();
            this.ClientSend.flush();
            this.Logger(LoggerSate.Response , DataResponse.toString());
            System.out.println("\n\n\n");
        } catch (Exception e) {
            System.out.println("Error In SendResponse");
        }
    }

    public void SendMessage(JSONObject MSG) {
        JSONObject JsonData = new JSONObject() ;
        JsonData.put("MessageArrived" , MSG);
        try {
            byte[] MessageEncryption = AES.encrypt(JsonData.toString() ,
                    this.ClientInfo.DataSecure.GetKey() ,
                    this.ClientInfo.DataSecure.GetIV()) ;
            this.ClientSend.write(new JSONObject().put("EncryptionData" , new JSONObject()
                    .put("Data" , Base64.getEncoder()
                            .encodeToString(MessageEncryption))).toString());
            this.ClientSend.newLine();
            this.ClientSend.flush();
            this.Logger(LoggerSate.Message , JsonData.toString());
            System.out.println("\n\n\n");
        } catch (Exception e) {
            System.out.println("Error In SendMessage");
        }
    }

    @Override
    public void run() {
        String RequestReceive = "";
        while (this.IsConnected) {
            try {
                RequestReceive = this.CommandCome.readLine();
                System.out.println("\n\n\n");
                if(this.IsHandCheck) {
                    System.out.println("Symmetric" + RequestReceive);
                    RequestReceive = this.ReceiveEncryptionWithAccount(RequestReceive) ;
                }
                else {
                    System.out.println("ASymmetric" + RequestReceive);
                    RequestReceive = this.ReceiveEncryptionWithoutAccount(RequestReceive) ;
                }
                this.Logger(LoggerSate.Request , RequestReceive);
                ProcessRequest ProcessApply =
                        FactoryProcess.GetProcess(new JSONObject(RequestReceive), this.ClientServer , this);
                JSONObject DataReceive = ProcessApply.Process();
                this.SendResponse(DataReceive);
            } catch (Exception e) {
                this.Disconnect();
            }
        }
        System.out.println("Client Disconnect With Socket : " + this.ClientSocket) ;
    }

    public ClientModel Info() {
        return this.ClientInfo;
    }

    public void SetClientInfo(ClientModel ClientData) {
        if(this.ClientInfo != null)
            return;
        this.ClientInfo = ClientData ;
    }

    private void Logger(LoggerSate logState , String JsonData) {
        JSONObject JsonLog = new JSONObject(JsonData) ;
        switch (logState) {
            case Request :
                System.out.println("Receive Request Process : " + JsonLog.toString());
                break ;
            case Response :
                System.out.println("Send Response Process : " + JsonLog.toString());
                break ;
            case Message :
                System.out.println("Receive Message : " + JsonLog.toString());
                break ;
        }
    }

    private String ReceiveEncryptionWithAccount(String DataEncryption) {
        String ResultData = "" ;
        JSONObject DataJson = new JSONObject(DataEncryption).getJSONObject("EncryptionData") ;
        String HMacRequest = DataJson.getString("HMac");
        byte[] ListByteMessage = Base64.getDecoder().decode(DataJson.getString("Data"));
        try {
            ResultData = AES.decrypt(ListByteMessage , HMacRequest ,
                    this.ClientInfo.DataSecure.GetKey() ,
                    this.ClientInfo.DataSecure.GetIV());
            if(ResultData == null)
                ResultData = "" ;
        } catch (Exception e) {
            System.out.println("Error In ReceiveEncryption");
        }
        return ResultData ;
    }

    private String ReceiveEncryptionWithoutAccount(String DataEncryption) {
        JSONObject DataJson = new JSONObject(DataEncryption).getJSONObject("EncryptionData") ;
        String KK = RSA.Decrypt(DataJson.getString("Data")
                , this.ClientServer.GetRSASecure().GetPrivateKey()) ;
        return KK ;
    }

    private void SendPublicKey() {
        JSONObject PublicKeyData = new JSONObject()
                .put("PublicKeyResponse" ,
                        RSA.ConvertPublicKey2String(this.ClientServer.GetRSASecure().GetPublicKey()));
        this.SendResponse(PublicKeyData);
    }

    private void Disconnect() {
        try {
            this.IsConnected = false ;
            this.ClientSocket.shutdownInput();
            this.ClientSocket.shutdownOutput();
            this.ClientSocket.close();
            this.CommandCome.close();
            this.ClientSend.close();
            this.ClientServer.GetClientManagement()
                    .DeleteClient(this);
        } catch (Exception e) {
            System.out.println("Something Wrong With Disconnect Socket");
        }
    }

}

enum LoggerSate {
    Request ,
    Response ,
    Message
}