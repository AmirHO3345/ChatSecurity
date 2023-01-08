package Processes;

import ClientClasses.ClientHandle;
import ServerClasses.Server;
import org.json.JSONObject;

public abstract class ProcessRequest {

    Server ProcessServer ;

    ClientHandle ProcessRequester ;

    JSONObject RequestNeed ;

    public ProcessRequest(Server ServerObject ,
                          ClientHandle Requester , JSONObject Request) {
        this.ProcessServer = ServerObject ;
        this.ProcessRequester = Requester ;
        this.RequestNeed = Request ;
    }

    public abstract JSONObject Process() ;

}
