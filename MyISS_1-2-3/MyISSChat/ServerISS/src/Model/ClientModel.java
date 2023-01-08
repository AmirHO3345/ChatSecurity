package Model;

public class ClientModel {

    public final int UserId ;

    public final String UserName ;

    public final long UserPhone ;

    public final AESSecureModel DataSecure ;

    public ClientModel(int ID , String Name , long Phone
            , AESSecureModel Secure) {
        this.UserId = ID ;
        this.UserName = Name ;
        this.UserPhone = Phone ;
        this.DataSecure = Secure ;
    }

}
