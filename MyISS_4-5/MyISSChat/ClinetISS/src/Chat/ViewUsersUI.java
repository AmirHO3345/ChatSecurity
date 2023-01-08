package Chat;

import Authentication.AuthenticationService;
import Model.DataSecureModel;
import Model.Message;
import Model.User;
import Security.AES;
import Security.CA;
import Security.RSA;
import com.company.Main;
import com.company.Singleton;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

public class ViewUsersUI {

    public void Controller() {
        Scanner input = new Scanner(System.in) ;
        do {
            this.Tips();
            System.out.print("Enter Operation Number : ");
            int CommandNum = input.nextInt() ;
            switch (CommandNum) {
                case 1 :
                    this.GetUsers();
                    break ;
                case 2 :
                    if(!ChatService.IsUserListEmpty())
                        this.ChooseUser();
                    break ;
                case 3 :
                    Singleton.ClearScreen();
                    break ;
                case 4 :
                    this.Send2UnknownUser();
                    break ;
                case 5 :
                    return ;
                default :
                    System.out.println("-- Enter Correct Number Please --");
                    break;
            }
        } while (true) ;
    }

    private void GetUsers() {
        ArrayList<User> TempArray = ChatService.GetAllUser() ;
        if(!TempArray.isEmpty()) {
            System.out.println("\n\n\n");
            for (int i = 0 ; i < TempArray.size() ; i++) {
                System.out.println(i + "--- Name : " +
                        TempArray.get(i).UserName +
                        "\t Phone : " + TempArray.get(i).Phone) ;
            }
            System.out.println("\n\n\n");
        } else
            System.out.println("There are no users you have previously contacted");
    }

    private void ChooseUser() {
        try {
            Scanner input = new Scanner(System.in) ;
            System.out.print("Choose User Index : ");
            int UserIndex = input.nextInt() ;
            User UserSelect = ChatService.UserSelect(UserIndex);
            if(UserSelect == null) {
                System.out.println("There Is No Any User With This Index");
                return ;
            }
            System.out.println("\n\n\n");
            RoomUI RoomCreated = new RoomUI(UserSelect) ;
            RoomCreated.Controller();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void Send2UnknownUser() {
        try {
            Scanner input = new Scanner(System.in) ;
            System.out.print("Phone Receiver : ");
            long PhoneTarget = input.nextLong() ;
            input.nextLine();
            System.out.print("Typing Message : ");
            String MessageSender = input.nextLine() ;
            Integer UserIndex = ChatService
                    .SendMessage(PhoneTarget , MessageSender) ;
            if(UserIndex == null) {
                System.out.println("Something Wrong With Send Message");
                return ;
            }
            User UserReceive = ChatService.UserSelect(UserIndex) ;
            System.out.println("\n\n\n");
            RoomUI RoomCreated = new RoomUI(UserReceive) ;
            RoomCreated.Controller();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void Tips() {
        System.out.println("1) Get All Users");
        if(!ChatService.IsUserListEmpty())
            System.out.println("2) Go Chat With User");
        System.out.println("3) Clear Console");
        System.out.println("4) To Send Unknown User");
        System.out.println("5) Back to last Page");
    }

    public static boolean SetUserInBack(JSONObject DataReceive) {
        return ChatService.SetUser(DataReceive) ;

    }

    public static void MessageReceive(JSONObject DataSend) {
        ChatService.ReceiveMessage(DataSend);

    }

}

class RoomUI {

    private final User UserInfo ;

    public RoomUI(User userInfo) {
        this.UserInfo = userInfo ;
        System.out.println(this.UserInfo.toString());
        System.out.println("\n\n\n");
    }

    public void Controller() {
        Scanner Input = new Scanner(System.in);
        do {
            this.Tips() ;
            System.out.print("Enter Operation Number : ");
            int CommandN = Input.nextInt() ;
            switch (CommandN) {
                case 1 :
                    this.ShowMessages() ;
                    break ;
                case 2 :
                    this.SendMessage();
                    break ;
                case 3 :
                    return ;
                default :
                    System.out.println("-- Enter Correct Number Please --");
                    break ;
            }
        } while (Main.GetConnectSocket().IsConnected());
    }

    private void ShowMessages() {
        ArrayList<Message> Messages =
                ChatService.GetAllMessages(UserInfo.Phone) ;
        if(Messages != null) {
            if(Messages.size() > 0) {
                System.out.println("\n\n\n");
                for (Message MessageTemp : Messages)
                    System.out.println(MessageTemp.Sender + " : " +
                            MessageTemp.MessageSender) ;
                System.out.println("\n\n\n");
            } else
                System.out.println("You don't have any message with that user");
        }
    }

    private void SendMessage() {
        try {
            Scanner input = new Scanner(System.in) ;
            System.out.print("Typing Message : ");
            String MessageSender = input.nextLine() ;
            Integer UserIndex = ChatService
                    .SendMessage(this.UserInfo.Phone , MessageSender);
            if(UserIndex != null)
                System.out.println("The Message Is Send");
            else
                System.out.println("The Message Is Not Send , Something Wrong");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void Tips() {
        System.out.println("1) To View All Messages ");
        System.out.println("2) To Send Messages ");
        System.out.println("3) To Exit From this Room ");
    }

}

class ChatService {

    private static final ArrayList<User> UsersList = new ArrayList<>() ;

    public static ArrayList<User> GetAllUser() {
        int ID = AuthenticationService.GetUserData().UserID ;
        JSONObject JsonSender = new JSONObject("{GetUsersRequest : {" +
                "ID : " + ID + "}}") ;
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(JsonSender) ;
        if(DataResponse.has("GetUsersResponse")) {
            JSONArray JsonList = DataResponse.getJSONArray("GetUsersResponse") ;
            for (int i = 0 ; i < JsonList.length() ; i++) {
                JSONObject JsonData = JsonList.getJSONObject(i) ;
                String UserName = JsonData.getString("UserName");
                long UserPhone = JsonData.getLong("UserPhone");
                boolean IsExisted = false ;
                for (User UserTemp: ChatService.UsersList)
                    if(UserTemp.Phone == UserPhone) {
                        IsExisted = true ;
                        break;
                    }
                if(!IsExisted)
                    ChatService.UsersList.add(new User(UserName , UserPhone)) ;
            }
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
        }
        return ChatService.UsersList ;
    }

    public static ArrayList<Message> GetAllMessages(long UserPhone) {
        int ID = AuthenticationService.GetUserData().UserID ;
        ArrayList<Message> MessagesResult = new ArrayList<>() ;
        JSONObject JsonSender = new JSONObject("{GetMessagesRequest : {" +
                "UserID : " + ID +
                ", PhoneWanted : " + UserPhone + "} }");
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(JsonSender) ;
        if(DataResponse.has("GetMessagesResponse")) {
            JSONArray JsonList = DataResponse.getJSONArray("GetMessagesResponse") ;
            int UserIndex = -1 ;
            for (int i = 0; i < ChatService.UsersList.size() ; i++)
                if(ChatService.UsersList.get(i).Phone == UserPhone) {
                    UserIndex = i ;
                    break;
                }
            for(int i=0 ; i < JsonList.length() ; i++) {
                JSONObject JsonData = JsonList.getJSONObject(i) ;
                String Sender = JsonData.getString("SenderName") ;
                String Receiver = JsonData.getString("ReceiverName") ;
                String MessageEncrypt = JsonData.getString("Message") ;
                try {
                    String MessageDecrypt = AES.decrypt(Base64.getDecoder().decode(MessageEncrypt)
                            , ChatService.UsersList.get(UserIndex).GetSecureAES().GetKey()
                            , ChatService.UsersList.get(UserIndex).GetSecureAES().GetIV()) ;
                    Message MessageCreate = new Message(Sender , Receiver , MessageDecrypt) ;
                    MessagesResult.add(MessageCreate) ;
                } catch (Exception e) {
                    Message MessageCreate = new Message(Sender , Receiver , MessageEncrypt) ;
                    MessagesResult.add(MessageCreate) ;
                }
            }
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
            return null ;
        }
        return MessagesResult ;
    }

    public static Integer SendMessage(long PhoneTarget , String Message) {
        int UserIndex = -1 ;
        for (int i = 0; i < ChatService.UsersList.size() ; i++)
            if(ChatService.UsersList.get(i).Phone == PhoneTarget) {
                UserIndex = i ;
                break;
            }
        if(UserIndex == -1 || ChatService.UsersList
                .get(UserIndex).GetSecureAES() == null) {
            User UserData = ChatService.InitUser(PhoneTarget) ;
            if(UserData == null)
                return null ;
            if(UserIndex != -1) {
                ChatService.UsersList.set(UserIndex , UserData) ;
            } else {
                ChatService.UsersList.add(UserData) ;
                UserIndex = ChatService.UsersList.size() - 1 ;
            }
        }
        User UserReceiver = ChatService.UsersList.get(UserIndex) ;
        try {
            byte[] MessageByte = AES.encrypt(Message , UserReceiver.GetSecureAES().GetKey()
                    , UserReceiver.GetSecureAES().GetIV()) ;
            Message = Base64.getEncoder().encodeToString(MessageByte) ;
            JSONObject JsonSender = new JSONObject() ;
            JsonSender.put("SendMessageRequest" ,
                    new JSONObject().put("SenderID" , AuthenticationService.GetUserData().UserID)
                            .put("PhoneTarget" , PhoneTarget)
                            .put("Message" , Message)
            );
            JSONObject DataResponse = Main.GetConnectSocket().SendRequest(JsonSender) ;
            if(DataResponse.has("SendMessageResponse"))
                return UserIndex ;
            else if(DataResponse.has("ErrorMessage")) {
                JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
                System.out.println(ExportError.getString("Message"));
                return null ;
            }
        } catch (Exception e) {
            System.out.println("Error Happen When Send Message In SendMessage Func");
        }
        return null ;
    }

    public static boolean IsUserListEmpty() {
        return ChatService.UsersList.isEmpty() ;

    }

    public static User UserSelect(int UserIndex) {
        if(UserIndex < 0 || UserIndex >= ChatService.UsersList.size())
            return null ;
        if(ChatService.UsersList.get(UserIndex).GetSecureAES() == null) {
            User UserTemp = ChatService.InitUser(ChatService.UsersList
                    .get(UserIndex).Phone) ;
            if(UserTemp == null)
                return null ;
            ChatService.UsersList.set(UserIndex , UserTemp) ;
        }
        return ChatService.UsersList.get(UserIndex) ;
    }

    public static boolean SetUser(JSONObject DataReceive) {
        String SenderName = DataReceive.getString("SenderName") ;
        long SenderPhone = DataReceive.getLong("SenderPhone") ;
        String DataEncrypt = DataReceive.getString("Data") ;
        String DataDecrypt = RSA.Decrypt(DataEncrypt ,
                AuthenticationService.GetUserData().GetKeys().GetPrivateKey()) ;
        if(DataDecrypt != null) {
            JSONObject DataJsonDecrypt = new JSONObject(DataDecrypt) ;
            SecretKeySpec secretKeySpec = AES.ConvertKey2Object
                    (DataJsonDecrypt.getString("SessionKey"));
            IvParameterSpec iv = AES.ConvertString2Iv(DataJsonDecrypt
                    .getString("ParameterIV")) ;
            int UserIndex = -1 ;
            for (int i = 0; i < ChatService.UsersList.size() ; i++)
                if(SenderPhone == ChatService.UsersList.get(i).Phone) {
                    UserIndex = i ;
                    break;
                }
            if(UserIndex == -1) {
                ChatService.UsersList.add(new User(SenderName , SenderPhone)) ;
                UserIndex = ChatService.UsersList.size() -1 ;
            }
            ChatService.UsersList.get(UserIndex).SetSecureAES(
                    new DataSecureModel(secretKeySpec , iv));
            return true ;
        }
        System.out.println("Error In SetUser Func");
        return false ;
    }

    public static void ReceiveMessage(JSONObject DataSend) {
        long PhoneSender = DataSend.getLong("SenderPhone") ;
        String SenderName = DataSend.getString("SenderUserName") ;
        String MessageEncrypt = DataSend.getString("MessageSender") ;
        User UserSender = null ;
        for (User UserTemp:ChatService.UsersList)
            if(UserTemp.Phone == PhoneSender)
                UserSender = UserTemp ;
        if(UserSender == null) {
            System.out.println("There Is No Data For User " + SenderName + ":(");
            return ;
        }
        try {
            String MessageDecrypt = AES.decrypt(Base64.getDecoder().decode(MessageEncrypt) ,
                    UserSender.GetSecureAES().GetKey() ,
                    UserSender.GetSecureAES().GetIV()) ;
            System.out.println("You Receive Message From " + SenderName +
                    "\nAnd The Message Is : " + MessageDecrypt);
        } catch (Exception e) {
            System.out.println("You Receive Message From " + SenderName +
                    "But You Can't Decrypt The Message :(");
        }
    }

    private static User InitUser(long UserPhone) {
        JSONObject DataUserJson = ChatService.GetReceiverData(UserPhone) ;
        if(DataUserJson.has("Error")) {
            System.out.println(DataUserJson.getString("Error"));
            return null ;
        }
        String UserName = DataUserJson.getString("UserName") ;
        X509Certificate Certificate = CA.String2Certificate(DataUserJson
                .getString("Certificate")) ;
        if(Certificate == null || !CA.VerifyCertificate(Certificate ,
                UserName.concat(UserPhone+""))) {
            System.out.println("Error In Certificate");
            return null ;
        }
//        System.out.println(Certificate.toString());
        String EncryptDepend = AuthenticationService.GetUserData().UserName
                .concat(AuthenticationService.GetUserData().Phone +
                        UserName + UserPhone);
        JSONObject EncryptJsonData = AES.GenerateSymmetricData(EncryptDepend) ;
        DataSecureModel SecureAES = new DataSecureModel(
                AES.ConvertKey2Object(EncryptJsonData.getString("SessionKey")) ,
                AES.ConvertString2Iv(EncryptJsonData.getString("IvParameter")));
        boolean IsSendSession = ChatService.SendSessionKey2Receiver(UserPhone
                , Certificate.getPublicKey() , SecureAES) ;
        if(!IsSendSession) {
            System.out.println("There Is Wrong When Send Session Key");
            return null ;
        }
        User UserData = new User(UserName , UserPhone) ;
        UserData.SetSecureAES(new DataSecureModel(
                AES.ConvertKey2Object(EncryptJsonData.getString("SessionKey")) ,
                AES.ConvertString2Iv(EncryptJsonData.getString("IvParameter"))
        ));
        return UserData ;
    }

    private static JSONObject GetReceiverData(long PhoneTarget) {
        JSONObject JsonSender = new JSONObject("{CertificateReceiverRequest : {" +
                "PhoneTarget : " + PhoneTarget + "}}");
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(JsonSender) ;
        if(DataResponse.has("CertificateReceiverResponse")) {
            //Check About Certificate
            DataResponse = DataResponse.getJSONObject("CertificateReceiverResponse") ;
            return new JSONObject().put("UserName" , DataResponse.getString("UserName"))
                .put("Certificate" , DataResponse.getString("Certificate"))
                .put("UserPhone" , PhoneTarget) ;
        }
        return new JSONObject().put("Error" , "Something Wrong With Get Data Process") ;
    }

    private static boolean SendSessionKey2Receiver(long TargetPhone
            , PublicKey publicKey , DataSecureModel SecureAES ) {
        JSONObject DataSend = new JSONObject()
                .put("SessionKey" , AES.ConvertKey2String(SecureAES.GetKey()))
                .put("ParameterIV" , AES.ConvertIv2String(SecureAES.GetIV()));
        String DataEncrypt = RSA.Encrypt(DataSend.toString() , publicKey) ;
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(new JSONObject()
            .put("SendSession2ReceiverRequest" , new JSONObject()
            .put("PhoneSender" , AuthenticationService.GetUserData().Phone)
            .put("PhoneReceiver" , TargetPhone)
            .put("Data" , DataEncrypt))) ;
        if(DataResponse.has("SendSession2ReceiverResponse"))
            return true ;
        System.out.println(DataResponse
                .getJSONObject("ErrorMessage").getString("Message"));
        return false;
    }
}

