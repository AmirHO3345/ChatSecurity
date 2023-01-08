package Controller;

import Authentication.AuthenticationService;
import Authentication.AuthenticationUI;
import Chat.ViewUsersUI;
import com.company.Main;
import com.company.Singleton;

import java.util.Scanner;

public class ConsoleController {

    public void UserKeyBoard() {
        Scanner Input = new Scanner(System.in);
        do {
            this.Tips();
            System.out.print("Enter Command Number : ");
            int CommandN = Input.nextInt();
            switch (CommandN) {
                case 1:
                    if(!AuthenticationService.IsUserRegister() &&
                            Main.GetConnectSocket().IsConnected()) {
                        new AuthenticationUI().Controller();
                    }
                    break;
                case 2:
                    if(AuthenticationService.IsUserRegister() &&
                            Main.GetConnectSocket().IsConnected()) {
                        new ViewUsersUI().Controller();
                    }
                    break ;
                case 3:
                    Singleton.ClearScreen();
                    break ;
                case 4:
                    Main.GetConnectSocket().Disconnect();
                    System.exit(0);
                    return ;
                default :
                    System.out.println("-- Enter Correct Number Please --");
                    break ;
            }
        } while (true) ;
    }

    private void Tips() {
        if(AuthenticationService.IsUserRegister()) {
            System.out.println("2) To Go Chat");
        } else {
            System.out.println("1) To SingIn Or SingUp");
        }
        System.out.println("3) To Clear Screen");
        System.out.println("4) To Exit");
    }

}
