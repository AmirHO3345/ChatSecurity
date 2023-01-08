package Model;

public class User {

    public final String UserName ;

    public final long Phone ;

    public User(String Name , long phone) {
        this.UserName = Name ;
        this.Phone = phone ;
    }

    @Override
    public String toString() {
        return "UserName : " + this.UserName + "\nUserPhone : " +
                this.Phone  ;
    }
}
