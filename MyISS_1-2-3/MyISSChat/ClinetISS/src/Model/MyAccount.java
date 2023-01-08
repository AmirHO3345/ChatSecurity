package Model;

public class MyAccount {

    public final int UserID ;

    public final String UserName ;

    public final long Phone ;

    public final DataSecureModel DataSecure ;

    public MyAccount(int ID , String Name , long phone ,
                     DataSecureModel Secure) {
        this.UserID = ID ;
        this.UserName = Name ;
        this.Phone = phone ;
        this.DataSecure = Secure ;
    }

}
