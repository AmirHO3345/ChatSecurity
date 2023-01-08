package Chat;

import Authentication.AuthenticationService;
import Model.Message;
import Model.User;
import com.company.Main;
import com.company.Singleton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Scanner;

public class ViewUsersUI {

    private ArrayList<User> UsersList ;

    public ViewUsersUI() {
        this.UsersList = new ArrayList<>() ;
    }

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
                    if(this.UsersList.size() > 0)
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
        if(TempArray != null) {
            if(TempArray.size() > 0) {
                System.out.println("\n\n\n");
                this.UsersList = TempArray ;
                for (int i = 0 ; i < this.UsersList.size() ; i++) {
                    System.out.println(i + ") Name : " +
                            this.UsersList.get(i).UserName +
                            "\t Phone : " + this.UsersList.get(i).Phone) ;
                }
                System.out.println("\n\n\n");
            } else
                System.out.println("There are no users you have previously contacted");
        }
    }

    private void ChooseUser() {
        try {
            Scanner input = new Scanner(System.in) ;
            System.out.print("Choose User Index : ");
            int UserIndex = input.nextInt() ;
            if(UserIndex < 0 || UserIndex >= this.UsersList.size()) {
                System.out.println("Try Again Later :)");
                return ;
            }
            System.out.println("\n\n\n");
            RoomUI RoomCreated = new RoomUI(this.UsersList.get(UserIndex)) ;
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
            System.out.println(ChatService.
                    SendMessage(PhoneTarget , MessageSender));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void Tips() {
        System.out.println("1) Get All Users");
        if(this.UsersList.size() > 0)
            System.out.println("2) Go Chat With User");
        System.out.println("3) Clear Console");
        System.out.println("4) To Send Unknown User");
        System.out.println("5) Back to last Page");
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
            System.out.println(ChatService
                    .SendMessage(this.UserInfo.Phone , MessageSender));
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

    public static ArrayList<User> GetAllUser() {
        ArrayList<User> UsersList = new ArrayList<>() ;
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
                UsersList.add(new User(UserName , UserPhone)) ;
            }
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
            return null ;
        }
        return UsersList ;
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
            for(int i=0 ; i < JsonList.length() ; i++) {
                JSONObject JsonData = JsonList.getJSONObject(i) ;
                String Sender = JsonData.getString("SenderName") ;
                String Receiver = JsonData.getString("ReceiverName") ;
                String MessageSend = JsonData.getString("Message") ;
                Message MessageCreate = new Message(Sender , Receiver , MessageSend) ;
                MessagesResult.add(MessageCreate) ;
            }
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
            return null ;
        }
        return MessagesResult ;
    }

    public static String SendMessage(long PhoneTarget , String Message) {
        JSONObject JsonSender = new JSONObject() ;
        JsonSender.put("SendMessageRequest" ,
                new JSONObject().put("SenderID" , AuthenticationService.GetUserData().UserID)
                        .put("PhoneTarget" , PhoneTarget)
                        .put("Message" , Message)
        );
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(JsonSender) ;
        if(DataResponse.has("SendMessageResponse"))
            return "Message Is Send Success" ;
        else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            return ExportError.getString("Message") ;
        }
        return "" ;
    }

}

