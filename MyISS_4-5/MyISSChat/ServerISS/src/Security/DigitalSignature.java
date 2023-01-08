package Security;

import java.security.*;
import java.util.Base64;

public class DigitalSignature {

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
