package Security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

public class DigitalSignature {

    public static String GenerateSignature(byte[] MSG , PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey, new SecureRandom());
            signature.update(MSG);
            return Base64.getEncoder().encodeToString(signature.sign());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static boolean verifySignature(String MSGOrigin
            , PublicKey publicKey, String digitalSignature) {
        try {
            byte[] MSGByte = Base64.getDecoder().decode(MSGOrigin) ;
            byte[] digitalSignatureByte = Base64.getDecoder().decode(digitalSignature) ;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(MSGByte);
            return signature.verify(digitalSignatureByte);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
