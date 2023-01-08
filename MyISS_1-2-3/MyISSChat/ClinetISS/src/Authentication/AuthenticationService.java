package Authentication;


import Model.DataSecureModel;
import Model.MyAccount;
import Security.AES;
import com.company.Main;
import org.json.JSONObject;

import javax.crypto.spec.IvParameterSpec;

public class AuthenticationService {

    private static MyAccount DataUser = null ;

    public boolean UserLogin(long UserPhone , String UserPassword) {
        JSONObject SymmetricData = AES.GenerateSymmetricData(UserPassword);
        JSONObject DataSend = new JSONObject()
            .put("SignInRequest" , new JSONObject()
            .put("Phone" , UserPhone).put("Password" , UserPassword)
            .put("SessionKey" , SymmetricData.get("SessionKey"))
            .put("ParameterIV" , SymmetricData.get("IvParameter"))) ;
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(DataSend) ;
        if(DataResponse.has("SignInResponse")) {
            JSONObject UserData = DataResponse.getJSONObject("SignInResponse") ;
            int ID = UserData.getInt("UserID") ;
            String Name = UserData.getString("UserName") ;
            AuthenticationService.DataUser = new MyAccount(ID , Name , UserPhone ,
                    new DataSecureModel(
                            AES.ConvertKey2Object(SymmetricData.getString("SessionKey")) ,
                            new IvParameterSpec(AES.
                                    ConvertIv2byte(SymmetricData.getJSONArray("IvParameter")))
                    ));
            return true ;
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
                .put("SessionKey" , SymmetricData.get("SessionKey"))
                .put("ParameterIV" , SymmetricData.get("IvParameter"))) ;
        JSONObject DataResponse = Main.GetConnectSocket().SendRequest(DataSend) ;
        if(DataResponse.has("SignUpResponse")) {
            JSONObject UserData = DataResponse.getJSONObject("SignUpResponse") ;
            int ID = UserData.getInt("UserID") ;
            AuthenticationService.DataUser = new MyAccount(ID , UserName , UserPhone ,
                    new DataSecureModel(
                            AES.ConvertKey2Object(SymmetricData.getString("SessionKey")) ,
                            new IvParameterSpec(AES
                                    .ConvertIv2byte(SymmetricData.getJSONArray("IvParameter")))
                    )) ;
            return true ;
        } else if(DataResponse.has("ErrorMessage")) {
            JSONObject ExportError = DataResponse.getJSONObject("ErrorMessage") ;
            System.out.println(ExportError.getString("Message"));
        }
        return false ;
    }

    public static boolean IsUserRegister() {
        return AuthenticationService.DataUser != null ;
    }

    public static MyAccount GetUserData() {
        return AuthenticationService.DataUser ;
    }
}
