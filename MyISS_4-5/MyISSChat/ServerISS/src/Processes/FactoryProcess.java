package Processes;

import ClientClasses.ClientHandle;
import Model.AESSecureModel;
import Model.CSRSecureModel;
import Model.ClientModel;
import Security.AES;
import Security.RSA;
import ServerClasses.CA;
import ServerClasses.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class FactoryProcess {
    public static ProcessRequest GetProcess(JSONObject JsonData, Server ProcessServer ,
                                            ClientHandle ProcessRequester) {
        JSONObject RealData ;
        if(JsonData.has("SignUpRequest")) {
            RealData = JsonData.getJSONObject("SignUpRequest") ;
            return new SignUpRequest(ProcessServer , ProcessRequester , RealData) ;
        } else if(JsonData.has("SignInRequest")) {
            RealData = JsonData.getJSONObject("SignInRequest") ;
            return new SignInRequest(ProcessServer , ProcessRequester , RealData);
        } else if(JsonData.has("SendMessageRequest")) {
            RealData = JsonData.getJSONObject("SendMessageRequest") ;
            return new SendMessageRequest(ProcessServer , ProcessRequester , RealData);
        } else if(JsonData.has("GetUsersRequest")) {
            RealData = JsonData.getJSONObject("GetUsersRequest") ;
            return new GetUsers(ProcessServer , ProcessRequester , RealData);
        } else if(JsonData.has("GetMessagesRequest")) {
            RealData = JsonData.getJSONObject("GetMessagesRequest") ;
            return new ViewMessages(ProcessServer , ProcessRequester , RealData);
        } else if(JsonData.has("SetKeysRequest")) {
            RealData = JsonData.getJSONObject("SetKeysRequest") ;
            return new SetKeysRequest(ProcessServer , ProcessRequester , RealData);
        } else if(JsonData.has("CertificateReceiverRequest")) {
            RealData = JsonData.getJSONObject("CertificateReceiverRequest") ;
            return new CertificateReceiverRequest(ProcessServer
                    , ProcessRequester , RealData) ;
        } else if(JsonData.has("SendSession2ReceiverRequest")) {
            RealData = JsonData.getJSONObject("SendSession2ReceiverRequest") ;
            return new SendSession2ReceiverRequest(ProcessServer
                    , ProcessRequester , RealData) ;
        }
        return new UnknownRequest(ProcessServer , ProcessRequester , null) ;
    }
}

class GetUsers extends ProcessRequest {

    public GetUsers(Server ServerObject ,
            ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("ID")) {
            int ClientID = this.RequestNeed.getInt("ID") ;
            JSONObject JsonData = this.ProcessServer.GetDBServer().GetAllUsers(ClientID) ;
            if(JsonData.has("Data")) {
                JSONArray JsonList = JsonData.getJSONArray("Data") ;
                return new JSONObject()
                        .put("GetUsersResponse" , JsonList) ;
            } else if(JsonData.has("DB_Error"))
                return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}") ;
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }
}

class SendMessageRequest extends ProcessRequest {

    public SendMessageRequest(Server ServerObject ,
                    ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("PhoneTarget") &&
                this.RequestNeed.has("SenderID") &&
                this.RequestNeed.has("Message")) {
            long PhoneTarget = this.RequestNeed.getLong("PhoneTarget") ;
            int IDSender = this.RequestNeed.getInt("SenderID") ;
            String MessageSend = this.RequestNeed.getString("Message") ;
            JSONObject DBResult =
                    this.ProcessServer.GetDBServer().SetMessage(IDSender , PhoneTarget , MessageSend) ;
            if(DBResult.has("Data")) {
                DBResult = DBResult.getJSONObject("Data") ;
                ClientHandle ClientReceiver = this.ProcessServer.GetClientManagement().
                        GetClient(DBResult.getInt("ReceiverID")) ;
                if(ClientReceiver != null) {
                    JSONObject DataSend = new JSONObject() ;
                    DataSend.put("SenderPhone" , DBResult.getLong("SenderPhone")) ;
                    DataSend.put("SenderUserName" , DBResult.getString("SenderName")) ;
                    DataSend.put("MessageSender" , MessageSend) ;
                    ClientReceiver.SendMessage(DataSend);
                }
                return new JSONObject("{SendMessageResponse : {Message : \"the Message Is Send\"}}") ;
            } else if(DBResult.has("Data_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"This Info You Send Maybe Wrong\"}}") ;
            } else if(DBResult.has("Sending_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"the Message Is Not Sending\"}}") ;
            } else if(DBResult.has("DB_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}") ;
            }
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }
}

class SignInRequest extends ProcessRequest {

    public SignInRequest(Server ServerObject ,
                              ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("Phone") &&
                this.RequestNeed.has("Password") &&
                this.RequestNeed.has("SessionKey") &&
                this.RequestNeed.has("ParameterIV")) {
            long Phone =  this.RequestNeed.getLong("Phone") ;
            String Password = this.RequestNeed.getString("Password") ;
            JSONObject DBResult = this.ProcessServer.GetDBServer().GetUserData(Phone , Password) ;
            if(DBResult.has("Data")) {
                int id = DBResult.getJSONObject("Data").getInt("UserID") ;
                String Name = DBResult.getJSONObject("Data").getString("UserName") ;
                PublicKey publicKey = RSA.ConvertString2PublicKey(DBResult.getJSONObject("Data")
                        .getString("PublicKey"));
                JSONObject ResultData = new JSONObject().put("SignInResponse" , new JSONObject()
                        .put("UserID" , id).put("UserName" , Name)
                        .put("PublicKey" , DBResult.getJSONObject("Data").getString("PublicKey"))
                        .put("PrivateKey" , DBResult.getJSONObject("Data").getString("PrivateKey"))) ;
                this.ProcessRequester.SetClientInfo(this.InitClient(id , Phone , Name ,
                        this.RequestNeed.getString("SessionKey") ,
                        this.RequestNeed.getString("ParameterIV") ,
                        publicKey));
                return this.EncryptDataLoginResponse(ResultData.toString() ,
                        AES.ConvertKey2Object(this.RequestNeed.getString("SessionKey")) ,
                        AES.ConvertString2Iv(this.RequestNeed.getString("ParameterIV")));
            } else if(DBResult.has("Validation_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"this Info is invalid\"}}");
            } else if(DBResult.has("DB_Error"))
                return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}") ;
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }

    private ClientModel InitClient(int Id , long Phone , String Name
            , String Key , String iv , PublicKey publicKey) {
        SecretKeySpec keySecret = AES.ConvertKey2Object(Key) ;
        IvParameterSpec ivParameterObject = AES.ConvertString2Iv(iv) ;
        return new ClientModel(Id , Name , Phone ,
                new AESSecureModel(keySecret , ivParameterObject),
                publicKey) ;
    }

    private JSONObject EncryptDataLoginResponse(String DataSend
            , SecretKeySpec keySpec , IvParameterSpec iv) {
        try {
            byte[] MessageEncryption = AES.encrypt(DataSend ,
                    keySpec , iv) ;
            String DataEncrypt = Base64.getEncoder().encodeToString(MessageEncryption) ;
            return new JSONObject().put("LoginEncryption" , new JSONObject()
                .put("Data" , DataEncrypt));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}");
    }
}

class SignUpRequest extends ProcessRequest {

    public SignUpRequest(Server ServerObject ,
                         ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("Phone") &&
                this.RequestNeed.has("UserName") &&
                this.RequestNeed.has("Password") &&
                this.RequestNeed.has("SessionKey") &&
                this.RequestNeed.has("ParameterIV")) {
            long Phone =  this.RequestNeed.getLong("Phone") ;
            String UserName = this.RequestNeed.getString("UserName") ;
            String Password = this.RequestNeed.getString("Password") ;
            JSONObject DBResult = this.ProcessServer
                    .GetDBServer().SetUserData(Phone , Password , UserName) ;
            if(DBResult.has("Data")) {
                int id = DBResult.getJSONObject("Data").getInt("UserID") ;
                this.ProcessRequester.SetClientInfo(this.InitClient(id , Phone , UserName ,
                        this.RequestNeed.getString("SessionKey") ,
                        this.RequestNeed.getString("ParameterIV") ));
                return new JSONObject().put("SignUpResponse" , new JSONObject()
                    .put("UserID" , id)) ;
            } else if(DBResult.has("Data_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"this Info is invalid\"}}");
            } else if(DBResult.has("DB_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}") ;
            }
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }

    private ClientModel InitClient(int Id , long Phone , String Name
            , String Key , String iv ) {
        SecretKeySpec keySecret = AES.ConvertKey2Object(Key) ;
        IvParameterSpec ivParameterObject = AES.ConvertString2Iv(iv) ;
        return new ClientModel(Id , Name , Phone ,
                new AESSecureModel(keySecret , ivParameterObject),
                null) ;
    }

}

class SetKeysRequest extends ProcessRequest {

    public SetKeysRequest(Server ServerObject ,
                         ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("UserID") &&
                this.RequestNeed.has("PublicKey") &&
                this.RequestNeed.has("PrivateKey")) {
            int UserID = this.RequestNeed.getInt("UserID") ;
            JSONObject DBResult = this.ProcessServer.GetDBServer().SetUserKey(UserID ,
                    this.RequestNeed.getString("PublicKey") ,
                    this.RequestNeed.getString("PrivateKey"));
            if(DBResult.has("Data")) {
                PublicKey publicKey = RSA.ConvertString2PublicKey(this
                        .RequestNeed.getString("PublicKey"));
                ClientHandle Client = this.ProcessServer
                        .GetClientManagement().GetClient(UserID);
                Client.Info().SetPublicKey(publicKey);
                return new JSONObject().put("SetKeysResponse" , "Done");
            }
            else
                return new JSONObject().put("ErrorMessage" , new JSONObject()
                    .put("Message" , "Error Happen When Set Keys"));
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }
}

class CertificateReceiverRequest extends ProcessRequest {

    public CertificateReceiverRequest(Server ServerObject ,
                          ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("PhoneTarget")) {
            long PhoneTarget = this.RequestNeed.getLong("PhoneTarget") ;
            JSONObject DBResult = this.ProcessServer.GetDBServer()
                    .GetUserDataByPhone(PhoneTarget);
            if(DBResult.has("Data")) {
                DBResult = DBResult.getJSONObject("Data") ;
                String UserName = DBResult.getString("UserName") ;
                String PublicKeyUser = DBResult.getString("PublicKey") ;
                X509Certificate Certificate = this.GetCertificate(UserName ,
                        PhoneTarget , PublicKeyUser) ;
                if(Certificate == null)
                    return new JSONObject().put("ErrorMessage" , new JSONObject()
                            .put("Message" , "Error Happen When Get Certificate"));
                return new JSONObject().put("CertificateReceiverResponse" , new JSONObject()
                    .put("UserName" , UserName)
                    .put("Certificate" , CA.Certificate2String(Certificate)));
            } else
                return new JSONObject().put("ErrorMessage" , new JSONObject()
                        .put("Message" , "The Phone Maybe Wrong"));
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }

    private X509Certificate GetCertificate(String UserName
            , long UserPhone , String PublicKey) {
        try {
            Socket CASocket = new Socket("localhost" , 2000) ;
            BufferedReader FromCA = new
                    BufferedReader(new InputStreamReader(CASocket.getInputStream()));
            BufferedWriter ToCA = new BufferedWriter
                    (new OutputStreamWriter(CASocket.getOutputStream()));
            String Subject = UserName + "," + UserPhone ;
            CSRSecureModel CSR = new CSRSecureModel(Subject ,
                    RSA.ConvertString2PublicKey(PublicKey)) ;
            ToCA.write(new JSONObject().put("CertificateRequest" , new JSONObject()
                    .put("CSRClient" , CSR.toString())).toString());
            ToCA.newLine();
            ToCA.flush();
            String Certificate = new JSONObject(FromCA.readLine()).getJSONObject("CertificateResponse")
                    .getString("Certificate");
            return CA.String2Certificate(Certificate) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }
}

class SendSession2ReceiverRequest extends ProcessRequest {

    public SendSession2ReceiverRequest(Server ServerObject ,
                          ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("PhoneSender") &&
            this.RequestNeed.has("PhoneReceiver") &&
            this.RequestNeed.has("Data")) {
            long PhoneSender = this.RequestNeed.getLong("PhoneSender") ;
            JSONObject DataSender = this.ProcessServer.GetDBServer()
                .GetUserDataByPhone(PhoneSender);
            if(DataSender.has("Data")) {
                DataSender = DataSender.getJSONObject("Data") ;
                long PhoneTarget = this.RequestNeed.getLong("PhoneReceiver") ;
                String SenderName = DataSender.getString("UserName") ;
                String DataSend = this.RequestNeed.getString("Data") ;
                ClientHandle Client = this.ProcessServer.GetClientManagement()
                        .GetClient(PhoneTarget);
                if(Client != null) {
                    JSONObject Data2Receiver = new JSONObject()
                            .put("ReceiveSessionFromSender" , new JSONObject()
                                    .put("SenderName" , SenderName)
                                    .put("SenderPhone" , PhoneSender)
                                    .put("Data" , DataSend));
                    Client.SendSessionKey(Data2Receiver);
                    return new JSONObject().put("SendSession2ReceiverResponse" , new JSONObject()
                            .put("Data" , "The Session Key Is Send Success"));
                } else {
                    return new JSONObject().put("ErrorMessage" , new JSONObject()
                            .put("Message" , "The User Is Not Active Right Now"));
                }
            } else
                return new JSONObject().put("ErrorMessage" , new JSONObject()
                        .put("Message" , "The Data Sender Is Not Valid"));
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }
}

class ViewMessages extends ProcessRequest {

    public ViewMessages(Server ServerObject ,
                         ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        if(this.RequestNeed.has("UserID") &&
                this.RequestNeed.has("PhoneWanted")) {
            int UserID = this.RequestNeed.getInt("UserID") ;
            long UserPhone = this.RequestNeed.getLong("PhoneWanted") ;
            JSONObject JsonData = this.ProcessServer.
                    GetDBServer().GetAllMessage(UserID , UserPhone) ;
            if(JsonData.has("Data")) {
                JSONArray JsonList = JsonData.getJSONArray("Data") ;
                return new JSONObject()
                        .put("GetMessagesResponse" , JsonList);
            } else if(JsonData.has("DB_Error"))
                return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}") ;
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }
}

class UnknownRequest extends ProcessRequest {

    public UnknownRequest(Server ServerObject ,
                        ClientHandle Requester, JSONObject JsonData) {
        super(ServerObject , Requester , JsonData);
    }

    @Override
    public JSONObject Process() {
        return new JSONObject(
                "{UnknownResponse : {Message : \"Unknown Request\"}}") ;
    }
}
