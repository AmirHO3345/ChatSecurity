package Model;


import java.security.PrivateKey;
import java.security.PublicKey;

public class RSASecureModel {

    private final PrivateKey PrivateServerKey;

    private final PublicKey PublicServerKey;

    public RSASecureModel(PublicKey keyPublic , PrivateKey keyPrivate) {
        this.PublicServerKey = keyPublic ;
        this.PrivateServerKey = keyPrivate;
    }

    public PrivateKey GetPrivateKey() {
        return this.PrivateServerKey ;
    }

    public PublicKey GetPublicKey() {
        return this.PublicServerKey ;
    }

}
