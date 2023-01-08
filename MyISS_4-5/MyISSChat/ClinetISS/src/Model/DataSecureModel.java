package Model;

import Security.AES;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DataSecureModel {

    private final SecretKeySpec keySpec ;

    private final IvParameterSpec ivParameter ;

    public DataSecureModel(SecretKeySpec key , IvParameterSpec iv) {
        this.keySpec = key ;
        this.ivParameter = iv ;
    }

    public SecretKeySpec GetKey() {
        return this.keySpec;
    }

    public IvParameterSpec GetIV() {
        return this.ivParameter ;
    }

    @Override
    public String toString() {
        return "IvParameter : " + AES.ConvertIv2String(this.ivParameter) +
        "\nSessionKey : " + AES.ConvertKey2String(this.keySpec) ;
    }
}
