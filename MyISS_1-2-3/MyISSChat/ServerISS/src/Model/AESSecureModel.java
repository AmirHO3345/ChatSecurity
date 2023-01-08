package Model;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESSecureModel {

    private final SecretKeySpec keySpec ;

    private final IvParameterSpec ivParameter ;

    public AESSecureModel(SecretKeySpec key , IvParameterSpec iv) {
        this.keySpec = key ;
        this.ivParameter = iv ;
    }

    public SecretKeySpec GetKey() {
        return this.keySpec;
    }

    public IvParameterSpec GetIV() {
        return this.ivParameter ;
    }

}
