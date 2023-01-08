package Model;

import Security.RSA;
import org.json.JSONObject;

import java.security.PrivateKey;
import java.security.PublicKey;

public class RSASecureModel {

    private final PrivateKey PrivateServerKey;

    private final PublicKey PublicServerKey;

    public RSASecureModel() {
        JSONObject DataJson = RSA.GenerateKey() ;
        this.PublicServerKey = RSA.ConvertString2PublicKey(DataJson.getString("PublicKey"));
        this.PrivateServerKey = RSA.ConvertString2PrivateKey(DataJson.getString("PrivateKey"));
    }

    public PrivateKey GetPrivateKey() {
        return this.PrivateServerKey ;
    }

    public PublicKey GetPublicKey() {
        return this.PublicServerKey ;
    }

}
