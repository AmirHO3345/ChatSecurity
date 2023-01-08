package Security;

import org.json.JSONObject;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSA {

    public static JSONObject GenerateKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return new JSONObject().put("PublicKey" , RSA.ConvertPublicKey2String(keyPair.getPublic()))
                    .put("PrivateKey" , RSA.ConvertPrivateKey2String(keyPair.getPrivate()));
        }catch (Exception e){
            e.printStackTrace();
        }
        return null ;
    }

    public static String Decrypt(String MSGEncrypt , PrivateKey privateKey) {
        try {
            byte[] MSGByte = Base64.getDecoder().decode(MSGEncrypt);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] DecryptedData = cipher.doFinal(MSGByte);
            return new String(DecryptedData , StandardCharsets.UTF_8) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static PublicKey ConvertString2PublicKey(String key) {
        try {
            byte[] PublicKeyByte = Base64.getDecoder().decode(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA") ;
            return keyFactory
                    .generatePublic(new X509EncodedKeySpec(PublicKeyByte));
        } catch (Exception e) {
            System.out.println("Error in Converting 2 Public In RSA");
        }
        return null ;
    }

    public static PrivateKey ConvertString2PrivateKey(String key) {
        try {
            byte[] PrivateKeyByte = Base64.getDecoder().decode(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA") ;
            return keyFactory
                    .generatePrivate(new PKCS8EncodedKeySpec(PrivateKeyByte));
        } catch (Exception e) {
            System.out.println("Error in Converting 2 Public In RSA");
        }
        return null ;
    }

    public static String ConvertPublicKey2String(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String ConvertPrivateKey2String(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
