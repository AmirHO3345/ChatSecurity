package ServerClasses;

import ClientClasses.ClientManagement;
import DataBaseClasses.DatabaseHandle;
import Model.CSRSecureModel;
import Model.RSASecureModel;
import Security.RSA;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class Server extends Thread {

    private ServerSocket SocketReceive ;

    private final ClientManagement ManagerClient ;

    private final DatabaseHandle DBServer ;

    private RSASecureModel RSASecure ;

    private X509Certificate ServerCertificate ;

    private PublicKey CAPublicKey ;

    private static final String ServerDomain = "amir@Server.com" ;

    public Server() {
        this.ManagerClient = new ClientManagement() ;
        this.DBServer = new DatabaseHandle() ;
        this.CAPublicKey = null ;
        this.GenerateKeys();
        this.GetCertificate();
        this.PrintServerInfo();
        try {
            this.SocketReceive = new ServerSocket(1255) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Ready Server");
        while(!this.SocketReceive.isClosed()) {
            try {
                Socket ClientSocket = this.SocketReceive.accept() ;
                this.ManagerClient.AddClient(ClientSocket , this);
            } catch (IOException e) {}
        }
    }

    public ClientManagement GetClientManagement() {
        return this.ManagerClient ;
    }

    public DatabaseHandle GetDBServer() {
        return this.DBServer ;
    }

    public RSASecureModel GetRSASecure() {
        return this.RSASecure ;
    }

    public X509Certificate GetServerCertificate() {
        return this.ServerCertificate ;
    }

    private void GenerateKeys() {
        JSONObject DBKeys = this.DBServer.GetServerKeys(Server.ServerDomain) ;
        if(DBKeys.has("Data")) {
            DBKeys = DBKeys.getJSONObject("Data") ;
            this.RSASecure = new RSASecureModel(
                    RSA.ConvertString2PublicKey(DBKeys.getString("PublicKey")) ,
                    RSA.ConvertString2PrivateKey(DBKeys.getString("PrivateKey"))
            );
            return;
        }
        JSONObject KeyGen = RSA.GenerateKey();
        this.RSASecure = new RSASecureModel(
                RSA.ConvertString2PublicKey(KeyGen.getString("PublicKey")),
                RSA.ConvertString2PrivateKey(KeyGen.getString("PrivateKey"))
        );
        this.DBServer.SetServerInfo(Server.ServerDomain, KeyGen.getString("PublicKey")
                , KeyGen.getString("PrivateKey"));
    }

    private void GetCertificate() {
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
            this.CAPublicKey = RSA.ConvertString2PublicKey(PublicKeyData) ;
            CSRSecureModel CSR = new CSRSecureModel(Server.ServerDomain, this.RSASecure.GetPublicKey()) ;
            ToCA.write(new JSONObject().put("CertificateRequest" , new JSONObject()
                    .put("CSRServer" , CSR.toString())).toString());
            ToCA.newLine();
            ToCA.flush();
            String Certificate = new JSONObject(FromCA.readLine()).getJSONObject("CertificateResponse")
                    .getString("Certificate");
            X509Certificate CertificateTemp = CA
                    .String2Certificate(Certificate) ;
            if(CertificateTemp == null || !CA.VerifyCertificate
                    (CertificateTemp , Server.ServerDomain , this.CAPublicKey)) {
                System.out.println("The Server Certificate Is Not Correct");
                return ;
            }
            this.ServerCertificate = CertificateTemp ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void PrintServerInfo() {
        System.out.println("\n\n\n");
        System.out.println("ServerDomain : " + Server.ServerDomain);
        System.out.println("Public Key : " + RSA.ConvertPublicKey2String(RSASecure.GetPublicKey()));
        System.out.println("Private Key : " + RSA.ConvertPrivateKey2String(RSASecure.GetPrivateKey()));
        System.out.println("Certificate : " + CA.Certificate2String(this.ServerCertificate));
        System.out.println("\n\n\n");
    }
}
