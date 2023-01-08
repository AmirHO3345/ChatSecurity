package Model;

import java.security.PublicKey;

public class ClientModel {

    public final int UserId ;

    public final String UserName ;

    public final long UserPhone ;

    public final AESSecureModel DataSecure ;

    private PublicKey ClientPublic ;

    public ClientModel(int ID , String Name , long Phone
            , AESSecureModel Secure , PublicKey publicKey) {
        this.UserId = ID ;
        this.UserName = Name ;
        this.UserPhone = Phone ;
        this.DataSecure = Secure ;
        this.ClientPublic = publicKey ;
    }

    public void SetPublicKey(PublicKey publicKey) {
        if(this.ClientPublic == null)
            this.ClientPublic = publicKey ;
    }

    public PublicKey GetPublicKey() {
        return this.ClientPublic ;

    }

}
