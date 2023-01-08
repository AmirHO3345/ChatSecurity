package ServerClasses;

import DataBaseClasses.DatabaseHandle;
import Model.CSRSecureModel;
import Model.RSASecureModel;
import Security.RSA;
import org.json.JSONObject;
import sun.security.x509.*;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

public class CA extends Thread {

    private static DatabaseHandle DBServer ;

    private static RSASecureModel RSASecure ;

    private final Socket SocketReceive ;

    private BufferedReader RequestCome ;

    private BufferedWriter ClientSend ;

    private static final String ServerDomain = "admin@CA.com" ;

    public CA(Socket ClientSocket) {
        this.SocketReceive = ClientSocket ;
        try {
            this.ClientSend = new BufferedWriter(new OutputStreamWriter(this.SocketReceive.getOutputStream()));
            this.RequestCome = new BufferedReader(new InputStreamReader(this.SocketReceive.getInputStream()));
            System.out.println("Client Connect With Socket : " + this.SocketReceive) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            CA.DBServer = new DatabaseHandle() ;
            CA.GenerateCAKeys();
            ServerSocket SocketReceive = new ServerSocket(2000) ;
            CA.PrintCAInfo() ;
            System.out.println("CA Is Ready");
            while(!SocketReceive.isClosed()) {
                Socket Client = SocketReceive.accept() ;
                CA NewClinet = new CA(Client) ;
                NewClinet.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!this.SocketReceive.isClosed()) {
            try {
                String CSRReceive = this.RequestCome.readLine() ;
                this.CSRProcess(CSRReceive);
            } catch (Exception e) {
                break;
            }
        }
        System.out.println("Client Is Out Of Server");
    }

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
            , String SubjectCertificate , PublicKey publicKey) {
        String Subject = Certificate.getSubjectX500Principal()
                .getName().split("CN=")[1] ;
        try {
            Certificate.verify(publicKey);
            return SubjectCertificate.equals(Subject);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false ;
    }

    private void CSRProcess(String RequestCome) {
        JSONObject JsonRequest = new JSONObject(RequestCome) ;
        if(JsonRequest.has("CertificateRequest")) {
            JSONObject CSRRequest = JsonRequest.getJSONObject("CertificateRequest") ;
            CSRSecureModel CSRSecure = null ;
            X509Certificate Certificate = null ;
            if(CSRRequest.has("CSRServer")) {
                CSRSecure = CSRSecureModel.ConvertString2CSR(CSRRequest.getString("CSRServer")) ;
                Certificate = this.GenerateServerCertificate(CSRSecure) ;
            } else if(CSRRequest.has("CSRClient")) {
                CSRSecure = CSRSecureModel.ConvertString2CSR(CSRRequest.getString("CSRClient")) ;
                Certificate = this.GenerateClientCertificate(CSRSecure) ;
            }
            JSONObject CADataJson = new JSONObject()
                .put("CertificateResponse" , new JSONObject()
                .put("PublicKey" , RSA.ConvertPublicKey2String(CA.RSASecure.GetPublicKey()))
                .put("Certificate" , CA.Certificate2String(Certificate))) ;
            this.SendResponse(CADataJson.toString());
        } else if(JsonRequest.has("PublicCARequest")) {
            this.SendResponse(new JSONObject()
                    .put("PublicCAResponse" , RSA.
                            ConvertPublicKey2String(RSASecure.GetPublicKey())).toString());
        }
    }

    private void SendResponse(String DataSend) {
        try {
            this.ClientSend.write(DataSend);
            this.ClientSend.newLine();
            this.ClientSend.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void GenerateCAKeys() {
        JSONObject DBKeys = CA.DBServer.GetServerKeys(CA.ServerDomain);
        if(DBKeys.has("Data")) {
            DBKeys = DBKeys.getJSONObject("Data") ;
            CA.RSASecure = new RSASecureModel(
                    RSA.ConvertString2PublicKey(DBKeys.getString("PublicKey")) ,
                    RSA.ConvertString2PrivateKey(DBKeys.getString("PrivateKey"))
            );
            return;
        }
        JSONObject KeyGen = RSA.GenerateKey();
        CA.RSASecure = new RSASecureModel(
                RSA.ConvertString2PublicKey(KeyGen.getString("PublicKey")),
                RSA.ConvertString2PrivateKey(KeyGen.getString("PrivateKey"))
        );
        CA.DBServer.SetServerInfo(CA.ServerDomain , KeyGen.getString("PublicKey")
                , KeyGen.getString("PrivateKey"));
    }

    private X509Certificate GenerateServerCertificate(CSRSecureModel CSR) {
        try {
            JSONObject ServerKeys = CA.DBServer.GetServerKeys(CSR.Name) ;
            if(ServerKeys.has("Data")) {
                PublicKey PK = RSA.ConvertString2PublicKey(
                        ServerKeys.getJSONObject("Data").getString("PublicKey")) ;
                if(!CSR.PublicKeyRequester.equals(PK))
                    throw new Exception("This Data Is Not Correct ") ;
            } else
                throw new Exception("This Data Is Not Correct ") ;
            return this.GenerateCertificate(CSR) ;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    private X509Certificate GenerateClientCertificate(CSRSecureModel CSR) {
        X509Certificate Certificate = null ;
        String[] SubjectUser = CSR.Name.split(",") ;
        JSONObject CertificateJson = CA.DBServer.GetUserCertificate
                (SubjectUser[0] , Long.parseLong(SubjectUser[1])) ;
        if(CertificateJson.has("Data")) {
            Certificate = CA.String2Certificate(CertificateJson
                    .getJSONObject("Data").getString("Certificate"));
        } else if(CertificateJson.has("CertificateExpire") ||
                CertificateJson.has("Empty")) {
            CSR = new CSRSecureModel(SubjectUser[0].concat(SubjectUser[1]) ,
                    CSR.PublicKeyRequester) ;
            Certificate = this.GenerateCertificate(CSR) ;
            CA.DBServer.SetCertificate(CA.Certificate2String(Certificate) ,
                    Certificate.getNotBefore() , Certificate.getNotAfter() ,
                    Long.parseLong(SubjectUser[1]));
        }
        return Certificate ;
    }

    private X509Certificate GenerateCertificate(CSRSecureModel CSR) {
        try {
            Date from = new Date();
            Date to = new Date(from.getTime() + 365 * 1000L * 24L * 60L * 60L);
            CertificateValidity interval = new CertificateValidity(from , to);
            BigInteger serialNumber = new BigInteger(64, new SecureRandom());
            X500Name owner = new X500Name("cn=" + CSR.Name);
            X500Name issuer = new X500Name("cn=CA");
            AlgorithmId sigAlgId = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
            X509CertInfo info = new X509CertInfo();
            info.set(X509CertInfo.VALIDITY, interval);
            info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(serialNumber));
            info.set(X509CertInfo.SUBJECT, owner);
            info.set(X509CertInfo.ISSUER, issuer);
            info.set(X509CertInfo.KEY, new CertificateX509Key(CSR.PublicKeyRequester));
            info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(sigAlgId));
            X509CertImpl certificate = new X509CertImpl(info);
            certificate.sign(CA.RSASecure.GetPrivateKey() , "SHA256withRSA");
            return certificate ;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    private X509Certificate CloneCertificate(X509Certificate Certificate) {
        try {
            Date from = Certificate.getNotBefore();
            Date to = Certificate.getNotAfter() ;
            CertificateValidity interval = new CertificateValidity(from , to);
            BigInteger serialNumber = Certificate.getSerialNumber() ;
            X500Name owner = new X500Name(Certificate.getSubjectX500Principal().getName());
            X500Name issuer = new X500Name(Certificate.getIssuerX500Principal().getName());
            AlgorithmId sigAlgId = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
            X509CertInfo info = new X509CertInfo();
            info.set(X509CertInfo.VALIDITY, interval);
            info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(serialNumber));
            info.set(X509CertInfo.SUBJECT, owner);
            info.set(X509CertInfo.ISSUER, issuer);
            info.set(X509CertInfo.KEY, new CertificateX509Key(Certificate.getPublicKey()));
            info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(sigAlgId));
            X509CertImpl certificate = new X509CertImpl(info);
            certificate.sign(CA.RSASecure.GetPrivateKey() , "SHA256withRSA");
            return certificate ;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    private static void PrintCAInfo() {
        System.out.println("\n\n\n");
        System.out.println("Public Key : " + RSA.ConvertPublicKey2String(CA.RSASecure.GetPublicKey()));
        System.out.println("Private Key : " + RSA.ConvertPrivateKey2String(CA.RSASecure.GetPrivateKey()));
        System.out.println("\n\n\n");
    }
}
