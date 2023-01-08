package Security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

public class HMac {

    public static String Hashing(String message, SecretKeySpec key , IvParameterSpec ivParameter) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, key , ivParameter);
            SealedObject EncryptionData = new SealedObject(message,cipher) ;
            return HMac.Hashing(EncryptionData , key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static String Hashing(SealedObject EncryptionData, SecretKeySpec key) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(EncryptionData) ;
            byte[] ListByte = out.toByteArray();
            Mac mac = Mac.getInstance("HmacSHA256") ;
            mac.init(key);
            byte[] macResult = mac.doFinal(ListByte);
            return Base64.getEncoder().encodeToString(macResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

}
