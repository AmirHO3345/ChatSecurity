package Model;

import java.io.*;
import java.security.PublicKey;
import java.util.Base64;

public class CSRSecureModel implements Serializable {

    public final String Name ;

    public final PublicKey PublicKeyRequester ;

    public CSRSecureModel(String name , PublicKey KeyRequester) {
        this.Name = name ;
        this.PublicKeyRequester = KeyRequester ;
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(this);
            byte[] CSRByte = bos.toByteArray();
            bos.close();
            return Base64.getEncoder().encodeToString(CSRByte) ;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static CSRSecureModel ConvertString2CSR(String CSR) {
        try {
            byte[] CSRByte = Base64.getDecoder().decode(CSR) ;
            ByteArrayInputStream bis = new ByteArrayInputStream(CSRByte);
            ObjectInput in = new ObjectInputStream(bis);
            CSRSecureModel CertificateResult  = (CSRSecureModel) in.readObject() ;
            bis.close();
            return CertificateResult ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

}
