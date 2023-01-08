package ClientClasses;


import ServerClasses.Server;

import java.net.Socket;
import java.util.ArrayList;

public class ClientManagement {

    private final ArrayList<ClientHandle> Clients ;

    public ClientManagement() {
        this.Clients = new ArrayList<>() ;

    }

    public void AddClient(Socket ClientSocket , Server ClientServer) {
        ClientHandle ClientCreated = new ClientHandle(ClientSocket , ClientServer) ;
        this.Clients.add(ClientCreated) ;
        ClientCreated.start() ;
    }

    public ClientHandle GetClient(int ClientID) {
        for (ClientHandle Client : this.Clients) {
            if(Client.Info().UserId == ClientID)
                return Client ;
        }
        return null ;
    }

    public void DeleteClient(ClientHandle ClientDelete) {
        this.Clients.remove(ClientDelete) ;

    }

}