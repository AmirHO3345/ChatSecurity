package ServerClasses;

import ClientClasses.ClientManagement;
import DataBaseClasses.DatabaseHandle;
import Model.RSASecureModel;
import Security.RSA;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

    private ServerSocket SocketReceive ;

    private final ClientManagement ManagerClient ;

    private final DatabaseHandle DBServer ;

    private final RSASecureModel RSASecure ;

    public Server() {
        this.ManagerClient = new ClientManagement() ;
        this.DBServer = new DatabaseHandle() ;
        this.RSASecure = new RSASecureModel() ;
        this.PrintServerInfo();
        try {
            this.SocketReceive = new ServerSocket(1255) ;
        } catch (IOException e) {}
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

    private void PrintServerInfo() {
        System.out.println("\n\n\n");
        System.out.println("Public Key : " + RSA.ConvertPublicKey2String(RSASecure.GetPublicKey()));
        System.out.println("Private Key : " + RSA.ConvertPrivateKey2String(RSASecure.GetPrivateKey()));
        System.out.println("\n\n\n");
    }
}
