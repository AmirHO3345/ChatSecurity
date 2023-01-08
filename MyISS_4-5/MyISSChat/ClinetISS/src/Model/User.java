package Model;

public class User {

    public final String UserName ;

    public final long Phone ;

    private DataSecureModel DataSecureAES ;

    public User(String Name , long phone) {
        this.UserName = Name ;
        this.Phone = phone ;
        this.DataSecureAES = null ;
    }

    public void SetSecureAES(DataSecureModel SecureAES) {
        this.DataSecureAES = SecureAES ;
    }

    public DataSecureModel GetSecureAES() {
        return this.DataSecureAES ;
    }

    @Override
    public String toString() {
       return "UserName : " + this.UserName + "\nUserPhone : " +
               this.Phone + "\n" + this.DataSecureAES.toString() ;
    }
}
