package Authentication;


import Model.CSRSecureModel;
import Model.DataSecureModel;
import Model.MyAccount;
import Security.AES;
import Security.CA;
import Security.RSA;
import com.company.Main;
import org.json.JSONObject;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class AuthenticationService {

    private static MyAccount DataUser = null ;

    public boolean UserLogin(long UserPhone , String UserPassword) {
        JSONObject SymmetricData = AES.GenerateSymmetricData(UserPassword);
        JSONObject DataSend = new JSONObject()
            .put("SignInRequest" , new JSONObject()
            .put("Phone" , UserPhone).put("Password" , UserPassword)
            .put("SessionKey" , SymmetricData.getString("SessionKey"))
            .put("ParameterIV" , SymmetricData.getString("IvParameter"))) ;
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(DataSend) ;
        if(DataResponse.has("LoginEncryption")) {
            String DecryptData = this.LoginDecryption(DataResponse.toString() ,
                    SymmetricData.getString("SessionKey") ,
                    SymmetricData.getString("IvParameter")) ;
            if(DecryptData == null) {
                System.out.println("Error Happen In Decryption Data");
                return false;
            }
            DataResponse = new JSONObject(DecryptData) ;
            JSONObject UserData = DataResponse.getJSONObject("SignInResponse") ;
            int ID = UserData.getInt("UserID") ;
            String Name = UserData.getString("UserName") ;
            AuthenticationService.DataUser = new MyAccount(ID , Name , UserPhone ,
                    new DataSecureModel(
                            AES.ConvertKey2Object(SymmetricData.getString("SessionKey")) ,
                            AES.ConvertString2Iv(SymmetricData.getString("IvParameter"))));
            AuthenticationService.DataUser.SetKeys(
                    UserData.getString("PublicKey") ,
                    UserData.getString("PrivateKey"));
            {
                X509Certificate Certificate = CA.GetClientCertificate
                        (this.CSRInitAccount(Name , UserPhone ,
                                RSA.ConvertString2PublicKey(UserData.getString("PublicKey")))) ;
                if(Certificate != null && CA.VerifyCertificate(Certificate,
                        AuthenticationService.DataUser.UserName.concat
                        (AuthenticationService.DataUser.Phone+""))) {
                    AuthenticationService.DataUser.SetCertificate(Certificate);
                    return true ;
                } else
                    System.out.println("The Get Certificate Is Fail");
            }
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
        }
        return false ;
    }

    public boolean UserRegister(long UserPhone , String UserName
            , String UserPassword) {
        JSONObject SymmetricData = AES.GenerateSymmetricData(UserPassword);
        JSONObject DataSend = new JSONObject()
                .put("SignUpRequest" , new JSONObject()
                .put("UserName" , UserName).put("Phone" , UserPhone)
                .put("Password" , UserPassword)
                .put("SessionKey" , SymmetricData.getString("SessionKey"))
                .put("ParameterIV" , SymmetricData.getString("IvParameter"))) ;
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(DataSend) ;
        if(DataResponse.has("SignUpResponse")) {
            JSONObject UserData = DataResponse.getJSONObject("SignUpResponse") ;
            int ID = UserData.getInt("UserID") ;
            AuthenticationService.DataUser = new MyAccount(ID , UserName , UserPhone ,
                    new DataSecureModel(
                            AES.ConvertKey2Object(SymmetricData.getString("SessionKey")) ,
                            AES.ConvertString2Iv(SymmetricData.getString("IvParameter"))));
            {
                JSONObject KeyData = RSA.GenerateKey() ;
                JSONObject SendKeys = new JSONObject()
                        .put("SetKeysRequest" , new JSONObject()
                                .put("UserID" , ID)
                                .put("PublicKey" , KeyData.getString("PublicKey"))
                                .put("PrivateKey" ,  KeyData.getString("PrivateKey"))) ;
                SendKeys = Main.GetConnectSocket().SendRequest(SendKeys);
                if(SendKeys.has("SetKeysResponse")) {
                    AuthenticationService.DataUser.SetKeys(
                            KeyData.getString("PublicKey") ,
                            KeyData.getString("PrivateKey"));
                    X509Certificate Certificate = CA.GetClientCertificate
                            (this.CSRInitAccount(UserName , UserPhone ,
                                   AuthenticationService.DataUser.GetKeys().GetPublicKey())) ;
                    if(Certificate != null && CA.VerifyCertificate(Certificate,
                            AuthenticationService.DataUser.UserName.concat
                                    (AuthenticationService.DataUser.Phone+""))) {
                        AuthenticationService.DataUser.SetCertificate(Certificate);
                        return true ;
                    } else
                        System.out.println("The Get Certificate Is Fail");
                } else {
                    System.out.println("The Send Keys Is Fail");
                }
            }
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
        }
        return false ;
    }

    public void ClearAccount() {
        AuthenticationService.DataUser = null ;

    }

    public static boolean IsUserRegister() {
        return AuthenticationService.DataUser != null ;
    }

    public static MyAccount GetUserData() {
        return AuthenticationService.DataUser ;
    }

    private CSRSecureModel CSRInitAccount(String UserName , long Phone
            , PublicKey publicKey) {
        String ConcatData = UserName + "," + Phone ;
        return new CSRSecureModel(ConcatData , publicKey) ;
    }

    private String LoginDecryption(String EncryptData , String keySpec
            , String iv) {
        try {
            String DataEncrypt = new JSONObject(EncryptData)
                    .getJSONObject("LoginEncryption").getString("Data");
            byte[] ListByteMessage = Base64.getDecoder().decode(DataEncrypt) ;
            return AES.decrypt(ListByteMessage ,
                    AES.ConvertKey2Object(keySpec) ,
                    AES.ConvertString2Iv(iv));
        } catch (Exception e) {
            System.out.println("Error In LoginDecryption Func");
        }
        return null ;
    }
}
