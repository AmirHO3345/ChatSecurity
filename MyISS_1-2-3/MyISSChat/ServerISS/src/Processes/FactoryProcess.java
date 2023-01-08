package Processes;

import ClientClasses.ClientHandle;
import Model.AESSecureModel;
import Model.ClientModel;
import Security.AES;
import ServerClasses.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
                this.ProcessRequester.SetClientInfo(this.InitClient(id , Phone , Name ,
                        this.RequestNeed.getString("SessionKey") ,
                        this.RequestNeed.getJSONArray("ParameterIV")));
                return new JSONObject().put("SignInResponse" , new JSONObject()
                    .put("UserID" , id).put("UserName" , Name)) ;
            } else if(DBResult.has("Validation_Error")) {
                return new JSONObject("{ErrorMessage : {Message : \"this Info is invalid\"}}");
            } else if(DBResult.has("DB_Error"))
                return new JSONObject("{ErrorMessage : {Message : \"Something Wrong\"}}") ;
        }
        return new
                JSONObject("{ErrorMessage : {Message : \"The Data Sender Is Not Complete or Wrong\"}}") ;
    }

    private ClientModel InitClient(int Id , long Phone , String Name
            , String Key , JSONArray ivParameterByte) {
        SecretKeySpec keySecret = AES.ConvertKey2Object(Key) ;
        IvParameterSpec ivParameterObject = new IvParameterSpec(AES
                .ConvertIv2byte(ivParameterByte)) ;
        return new ClientModel(Id , Name , Phone ,
                new AESSecureModel(keySecret , ivParameterObject)) ;
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
                        this.RequestNeed.getJSONArray("ParameterIV")));
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
            , String Key , JSONArray ivParameterByte) {
        SecretKeySpec keySecret = AES.ConvertKey2Object(Key) ;
        IvParameterSpec ivParameterObject = new IvParameterSpec(AES
                .ConvertIv2byte(ivParameterByte)) ;
        return new ClientModel(Id , Name , Phone ,
                new AESSecureModel(keySecret , ivParameterObject)) ;
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
