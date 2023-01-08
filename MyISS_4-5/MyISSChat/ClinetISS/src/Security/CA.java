package Security;

import Model.CSRSecureModel;
import org.json.JSONObject;


import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CA {

    private static PublicKey CAPublicKey = null ;

    public static String Certificate2String(X509Certificate Certificate) {
        try {
            byte[] CertificateByte = Certificate.getEncoded() ;
            return Base64.getEncoder().encodeToString(CertificateByte) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static X509Certificate String2Certificate(String Certificate) {
        try {
            byte[] CertificateByte = Base64.getDecoder().decode(Certificate) ;
            ByteArrayInputStream CertificateStream = new ByteArrayInputStream(CertificateByte) ;
            CertificateFactory CFT = CertificateFactory.getInstance("X.509") ;
            X509Certificate CertificateResult = (X509Certificate)CFT.generateCertificate(CertificateStream);
            CertificateStream.close();
            return CertificateResult ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static boolean VerifyCertificate(X509Certificate Certificate
            , String SubjectCertificate) {
        if(CA.CAPublicKey == null)
            CA.GetCAPublicKey();
        String Subject = Certificate.getSubjectX500Principal()
                .getName().split("CN=")[1] ;
        try {
            Certificate.verify(CA.CAPublicKey);
            return SubjectCertificate.equals(Subject);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false ;
    }

    public static X509Certificate GetClientCertificate(CSRSecureModel CSR) {
        try {
            Socket CASocket = new Socket("localhost" , 2000) ;
            BufferedReader FromCA = new
                    BufferedReader(new InputStreamReader(CASocket.getInputStream()));
            BufferedWriter ToCA = new BufferedWriter
                    (new OutputStreamWriter(CASocket.getOutputStream()));
            ToCA.write(new JSONObject().put("CertificateRequest" , new JSONObject()
                .put("CSRClient" , CSR.toString())).toString());
            ToCA.newLine();
            ToCA.flush();
            String CertificateClient = new JSONObject(FromCA.readLine())
                    .getJSONObject("CertificateResponse")
                    .getString("Certificate") ;
            return CA.String2Certificate(CertificateClient) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    private static void GetCAPublicKey() {
        try {
            Socket CASocket = new Socket("localhost" , 2000) ;
            BufferedReader FromCA = new
                    BufferedReader(new InputStreamReader(CASocket.getInputStream()));
            BufferedWriter ToCA = new BufferedWriter
                    (new OutputStreamWriter(CASocket.getOutputStream()));
            ToCA.write(new JSONObject().put("PublicCARequest" , "").toString());
            ToCA.newLine();
            ToCA.flush();
            String PublicKeyData = new JSONObject(FromCA.readLine())
                    .getString("PublicCAResponse");
            CA.CAPublicKey = RSA.ConvertString2PublicKey(PublicKeyData) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
