package Model;

import Security.CA;
import Security.RSA;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class MyAccount {

    public final int UserID ;

    public final String UserName ;

    public final long Phone ;

    public final DataSecureModel DataSecureAES;

    private RSASecureModel DataSecureRSA ;

    private X509Certificate ClientCertificate ;

    public MyAccount(int ID , String Name , long phone ,
                     DataSecureModel ASE) {
        this.UserID = ID ;
        this.UserName = Name ;
        this.Phone = phone ;
        this.DataSecureAES = ASE ;
        this.DataSecureRSA = null ;
        this.ClientCertificate = null ;
    }

    public void SetKeys(PublicKey publicKey , PrivateKey privateKey) {
        if(this.DataSecureRSA == null)
            this.DataSecureRSA = new RSASecureModel(publicKey , privateKey) ;
    }

    public void SetKeys(String publicKey , String privateKey) {
        if(this.DataSecureRSA == null) {
            PublicKey PubK = RSA.ConvertString2PublicKey(publicKey) ;
            PrivateKey PriK = RSA.ConvertString2PrivateKey(privateKey) ;
            this.DataSecureRSA = new RSASecureModel(PubK , PriK) ;
        }
    }

    public void SetCertificate(X509Certificate Certificate) {
        if(this.ClientCertificate == null)
            this.ClientCertificate = Certificate ;
    }

    public void SetCertificate(String Certificate) {
        if(this.ClientCertificate == null)
            this.ClientCertificate = CA.String2Certificate(Certificate) ;
    }

    public X509Certificate GetCertificate() {
        return this.ClientCertificate ;
    }

    public RSASecureModel GetKeys() {
        return this.DataSecureRSA ;
    }
}
