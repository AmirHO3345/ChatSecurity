package DataBaseClasses;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;

public class DatabaseHandle {

    private Connection DBConnect ;

    public DatabaseHandle() {
        try {
            Class.forName("com.mysql.jdbc.Driver") ;
            String URL ="jdbc:mysql://localhost:3306/db_chats_1?useSSL=false" ;
            String UserName = "root" ;
            String Password = "" ;
            this.DBConnect = DriverManager.getConnection(URL , UserName , Password) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject GetUserData(long Phone , String Password) {
        JSONObject DataResult = new JSONObject() ;
        try {
            Statement LoginStatement = this.DBConnect.createStatement() ;
            ResultSet LoginResult = LoginStatement.executeQuery("" +
                    "select * from users U WHERE U.phone=" + Phone +
                    " and U.password=" + "'" + Password + "'" + ";");
            if(LoginResult.next()) {
                DataResult.put("Data" , new JSONObject()
                    .put("UserID" , LoginResult.getInt("id"))
                    .put("UserName" , LoginResult.getString("name"))
                    .put("UserPhone" , LoginResult.getLong("phone")));
            } else
                DataResult.put("Validation_Error" , "The Phone Or Password Is Wrong") ;
        } catch (Exception e) {
            DataResult.put("DB_Error" , "Something Wrong With DB") ;
        }
        return DataResult ;
    }

    public JSONObject SetUserData(long Phone , String Password , String UserName) {
        JSONObject DataResult = new JSONObject() ;
        try {
            Statement RegisterStatement = this.DBConnect.createStatement() ;
            int RegisterResult ;
            try {
                RegisterResult = RegisterStatement.executeUpdate(
                        "insert into users (`name`, `phone`, `password`) VALUES" +
                                "(" +
                                "'" + UserName + "'" + "," + Phone + ","
                                + "'" + Password + "'" + ");") ;
            } catch (Exception e) {
                DataResult.put("Data_Error" , "This Phone Number Is Used Please Try Again") ;
                return DataResult ;
            }
            if(RegisterResult > 0) {
                JSONObject jsonData = this.GetUserByPhone(Phone) ;
                if(jsonData.has("Data")) {
                    jsonData = jsonData.getJSONObject("Data") ;
                    DataResult.put("Data" , new JSONObject()
                            .put("UserID" , jsonData.getInt("UserID")));
                } else if(jsonData.has("DB_Error"))
                    DataResult.put("DB_Error" , jsonData.getString("DB_Error")) ;
            }
        } catch (Exception e) {
            DataResult.put("DB_Error" , "Something Wrong With DB") ;
        }
        return DataResult ;
    }

    public JSONObject SetMessage(int IdSender , long PhoneTarget , String Message) {
        JSONObject DataResult = new JSONObject() ;
        JSONObject SenderInfo = this.GetUserByID(IdSender) ;
        JSONObject ReceiverInfo = this.GetUserByPhone(PhoneTarget) ;
        try {
            if(SenderInfo.has("Data") &&
                    ReceiverInfo.has("Data")) {
                SenderInfo = SenderInfo.getJSONObject("Data") ;
                ReceiverInfo = ReceiverInfo.getJSONObject("Data") ;
                /* Start Variable */
                int IDReceiver = ReceiverInfo.getInt("UserID") ;
                String UserNameReceiver = ReceiverInfo.getString("UserName") ;
                String UserNameSender = SenderInfo.getString("UserName") ;
                long PhoneReceiver = ReceiverInfo.getLong("UserPhone") ;
                long PhoneSender = SenderInfo.getLong("UserPhone") ;
                Statement GetStatement = this.DBConnect.createStatement() ;
                /* End Variable */
                int ResultStatement ;
                try {
                    ResultStatement = GetStatement.executeUpdate
                            ("insert into messages (id_resv,id_send,message) " +
                                    "values (" + IDReceiver + "," + IdSender
                                    + "," + "'" + Message + "'" + ");") ;
                } catch (Exception e) {
                    DataResult.put("Data_Error" , "Store Process Is Fail") ;
                    return DataResult ;
                }
                if(ResultStatement > 0) {
                    DataResult.put("Data" , new JSONObject()
                        .put("ReceiverID" , IDReceiver)
                        .put("SenderID" , IdSender)
                        .put("ReceiverName" , UserNameReceiver)
                        .put("SenderName" , UserNameSender)
                        .put("ReceiverPhone" , PhoneReceiver)
                        .put("SenderPhone" , PhoneSender)
                    );
                } else
                    DataResult.put("Process_Error" , "The Set Data Process Is Fail");
            } else
                DataResult.put("Sending_Error" , "Sending Message Is Fail") ;
        } catch (Exception e) {
            DataResult.put("DB_Error" , "Something Wrong With DB") ;
        }
        return DataResult ;
    }

    public JSONObject GetAllUsers(int IdUser) {
        JSONObject DataResult = new JSONObject() ;
        ArrayList<Integer> UsersIDs = new ArrayList<>() ;
        JSONArray AggregationData = new JSONArray() ;
        try {
            Statement GetStatement = this.DBConnect.createStatement() ;
            ResultSet UsersResult = GetStatement.executeQuery("" +
                    "select * from messages M WHERE M.id_resv=" + IdUser +
                    " or M.id_send=" + IdUser + ";");
            while(UsersResult.next()) {
                int ID_Receive = UsersResult.getInt("id_resv") ;
                int ID_Send = UsersResult.getInt("id_send") ;
                if(ID_Receive != IdUser && !UsersIDs.contains(ID_Receive))
                    UsersIDs.add(ID_Receive) ;
                else if(ID_Send != IdUser && !UsersIDs.contains(ID_Send))
                    UsersIDs.add(ID_Send) ;
            }
            for (int UserId : UsersIDs) {
                JSONObject UserInfo = this.GetUserByID(UserId) ;
                if(UserInfo.has("Data"))
                    AggregationData.put(UserInfo.getJSONObject("Data"));
            }
            DataResult.put("Data" , AggregationData) ;
        } catch (Exception e) {
            DataResult.put("DB_Error" , "Something Wrong With DB") ;
        }
        return DataResult ;
    }

    public JSONObject GetAllMessage(int User1ID , long User2Phone) {
        JSONObject ResultProcess = new JSONObject() ;
        JSONObject User1Info = this.GetUserByID(User1ID) ;
        JSONObject User2Info = this.GetUserByPhone(User2Phone) ;
        if(User1Info.has("Data") && User2Info.has("Data")) {
            User1Info = User1Info.getJSONObject("Data") ;
            User2Info = User2Info.getJSONObject("Data") ;
            JSONArray AggregationData = new JSONArray() ;
            try {
                Statement GetStatement = this.DBConnect.createStatement() ;

                int UserTwo = User2Info.getInt("UserID") ;
                ResultSet UsersResult = GetStatement.executeQuery("" +
                        "select * from messages M WHERE (M.id_resv=" + User1ID +
                        " and M.id_send=" + UserTwo + ") or (id_resv=" + UserTwo +
                        " and M.id_send=" + User1ID + ");");
                while(UsersResult.next()) {
                    JSONObject ProcessData = new JSONObject() ;
                    if(UsersResult.getInt("id_send") == User1ID) {
                        ProcessData.put("SenderName" , User1Info.getString("UserName")) ;
                        ProcessData.put("ReceiverName" , User2Info.getString("UserName")) ;
                    } else {
                        ProcessData.put("SenderName" , User2Info.getString("UserName")) ;
                        ProcessData.put("ReceiverName" , User1Info.getString("UserName")) ;
                    }
                    ProcessData.put("Message" , UsersResult.getString("message")) ;
                    AggregationData.put(ProcessData) ;
                }
                if(AggregationData.length() > 0)
                    ResultProcess.put("Data" , AggregationData) ;
            } catch (Exception e) {
                ResultProcess.put("DB_Error" , "Something Wrong With DB") ;
            }
        }
        return ResultProcess ;
    }

    private JSONObject GetUserByID(int UserID) {
        JSONObject DataResult = new JSONObject() ;
        try {
            Statement GetStatement = this.DBConnect.createStatement() ;
            ResultSet GetUserResult = GetStatement.executeQuery("" +
                    "select * from users U WHERE U.id=" + UserID + ";");
            if(GetUserResult.next()) {
                DataResult.put("Data", new JSONObject()
                    .put("UserName" , GetUserResult.getString("name"))
                    .put("UserPhone" , GetUserResult.getLong("phone"))) ;
            }
        } catch (Exception e) {
            DataResult.put("DB_Error" , "Something Wrong With DB") ;
        }
        return DataResult ;
    }

    private JSONObject GetUserByPhone(long Phone) {
        JSONObject DataResult = new JSONObject() ;
        try {
            Statement GetStatement = this.DBConnect.createStatement() ;
            ResultSet GetUserResult = GetStatement.executeQuery("" +
                    "select * from users U WHERE U.phone=" + Phone + ";");
            if(GetUserResult.next()) {
                DataResult.put("Data", new JSONObject()
                        .put("UserID" , GetUserResult.getString("id"))
                        .put("UserName" , GetUserResult.getString("name"))
                        .put("UserPhone" , GetUserResult.getLong("phone"))) ;
            }
        } catch (Exception e) {
            DataResult.put("DB_Error" , "Something Wrong With DB") ;
        }
        return DataResult ;
    }
}
