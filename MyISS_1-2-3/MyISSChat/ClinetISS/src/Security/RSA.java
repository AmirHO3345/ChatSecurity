package Security;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSA {

    public static String Encrypt(String MSG , PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] EncryptedData = cipher.doFinal(MSG.getBytes());
            return Base64.getEncoder().encodeToString(EncryptedData);
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
}
