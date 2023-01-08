package Authentication;

import Model.MyAccount;
import Security.AES;
import Security.CA;
import Security.RSA;
import com.company.Main;
import com.company.Singleton;

import java.util.Scanner;

public class AuthenticationUI {

    private final AuthenticationService AuthService ;

    public AuthenticationUI() {
        this.AuthService = new AuthenticationService() ;
    }

    public void Controller() {
        Scanner Input = new Scanner(System.in);
        do {
            this.Tips() ;
            System.out.print("Enter Operation Number : ");
            int CommandN = Input.nextInt() ;
            switch (CommandN) {
                case 1 :
                    this.LoginUser() ;
                    break ;
                case 2 :
                    this.RegisterUser();
                    break ;
                case 3 :
                    Singleton.ClearScreen();
                    break ;
                case 4 :
                    return ;
                default :
                    System.out.println("-- Enter Correct Number Please --");
                    break ;
            }
        } while (Main.GetConnectSocket().IsConnected() &&
            !AuthenticationService.IsUserRegister());
    }

    private void LoginUser() {
        try {
            Scanner input = new Scanner(System.in) ;
            System.out.print("Enter Phone Number : ");
            long Phone =  input.nextLong() ;
            input.nextLine() ;
            System.out.print("Enter Password : ");
            String Password =  input.nextLine() ;
            if(this.AuthService.UserLogin(Phone , Password))
                this.PrintMyAccountInfo();
            else
                this.AuthService.ClearAccount();
        } catch (Exception e) {
            System.out.println("This Data Is Not Valid");
        }
    }

    private void RegisterUser() {
        try {
            Scanner input = new Scanner(System.in) ;
            System.out.print("Enter UserName : ");
            String UserName =  input.nextLine() ;
            System.out.print("Enter Phone Number : ");
            long Phone =  input.nextLong() ;
            input.nextLine() ;
            System.out.print("Enter Password : ");
            String Password =  input.nextLine() ;
            if(this.AuthService.UserRegister(Phone , UserName , Password))
                this.PrintMyAccountInfo();
            else
                this.AuthService.ClearAccount();
        } catch (Exception e) {
            System.out.println("This Data Is Not Valid");
        }
    }

    private void Tips() {
        System.out.println("1) SignIn To Server") ;
        System.out.println("2) SingUp To Server") ;
        System.out.println("3) Clear Console") ;
        System.out.println("4) Back To Last Page") ;
    }

    private void PrintMyAccountInfo() {
        MyAccount Account = AuthenticationService.GetUserData() ;
        System.out.println("\n\n\n");
        System.out.println("UserID : " + Account.UserID);
        System.out.println("UserName : " + Account.UserName);
        System.out.println("UserPhone : " + Account.Phone);
        System.out.println("SessionKey : " + AES.ConvertKey2String(Account.DataSecureAES.GetKey()));
        System.out.println("IvParameter : " + AES.ConvertIv2String(Account.DataSecureAES.GetIV()));
        System.out.println("PublicKey : " + RSA.ConvertPublicKey2String(Account.GetKeys().GetPublicKey()));
        System.out.println("PrivateKey : " + RSA.ConvertPrivateKey2String(Account.GetKeys().GetPrivateKey()));
        System.out.println("Certificate : " + CA.Certificate2String(Account.GetCertificate()));
        System.out.println("\n\n\n");
    }
}
