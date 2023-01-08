package Security;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AES {

    public static JSONObject GenerateSymmetricData(String password) {
        SecureRandom random = new SecureRandom();
        byte[] iv = random.generateSeed(16);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpecSecure = AES.generateKey(password) ;
        return new JSONObject().put("SessionKey" , AES.ConvertKey2String(keySpecSecure))
                .put("IvParameter" , new JSONArray(ivParameterSpec.getIV())) ;
    }

    public static byte[] encrypt(String message , SecretKeySpec key , IvParameterSpec ivParameter)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, key , ivParameter);
        byte[] MessageEncryption = null ;
        try
        {
            SealedObject encryptedObject = new SealedObject(message,cipher);
            ByteArrayOutputStream ListByte = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream  = new CipherOutputStream(ListByte , cipher) ;
            ObjectOutputStream SealedObjectOut = new ObjectOutputStream(cipherOutputStream) ;
            SealedObjectOut.writeObject(encryptedObject);
            SealedObjectOut.close();
            MessageEncryption = ListByte.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return MessageEncryption;
    }

    public static String decrypt(byte[] encryptedObject , SecretKeySpec key , IvParameterSpec ivParameter)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String decryptedObject = null;
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, key , ivParameter);
        try
        {
            ByteArrayInputStream iStream = new ByteArrayInputStream(encryptedObject);
            CipherInputStream cipherInputStream = new CipherInputStream(iStream, cipher);
            ObjectInputStream inputStream = new ObjectInputStream(cipherInputStream);
            SealedObject SealedEncrypt = (SealedObject) inputStream.readObject();
            decryptedObject = (String) SealedEncrypt.getObject(cipher);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return decryptedObject;
    }

    public static String ConvertKey2String(SecretKeySpec key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static SecretKeySpec ConvertKey2Object(String key) {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        return new SecretKeySpec(decodedKey , 0 , decodedKey.length , "AES") ;
    }

    private static SecretKeySpec generateKey(String Password) {
        try {
            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            //PBKDF2WithHmacSHA256 it's Algo used
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            //PBEKeySpec for create Key from Password
            KeySpec spec = new PBEKeySpec(Password.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] ConvertIv2byte(JSONArray ByteList) {
        byte[] iv = new byte[ByteList.length()];
        for (int i = 0; i < ByteList.length() ; i++)
            iv[i] = (byte) ByteList.get(i) ;
        return iv ;
    }

}
