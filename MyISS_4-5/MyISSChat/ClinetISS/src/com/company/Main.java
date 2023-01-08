package com.company;

import Connect.SocketConnect;
import Controller.ConsoleController;

public class Main {

    private static ConsoleController ControllerUser ;

    private static SocketConnect ServerConnect ;

    public static void main(String[] args) {
        Main.ServerConnect = new SocketConnect(Singleton.ServerName , Singleton.PortNumber) ;
        if(!Main.ServerConnect.IsConnected()) {
            System.out.println("We Not Found Server Please Try Again");
            return;
        }
        Main.ServerConnect.start();
        Main.ControllerUser = new ConsoleController() ;
        Main.ControllerUser.UserKeyBoard();
    }

    public static SocketConnect GetConnectSocket() {
        return Main.ServerConnect ;
    }

}
