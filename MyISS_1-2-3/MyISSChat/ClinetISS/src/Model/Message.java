package Model;

public class Message {

    public final String Sender ;

    public final String Receiver ;

    public final String MessageSender ;

    public Message(String UserSender , String UserReceiver ,
                   String MessageSave) {
        this.Sender = UserSender ;
        this.Receiver = UserReceiver ;
        this.MessageSender = MessageSave ;
    }

}
