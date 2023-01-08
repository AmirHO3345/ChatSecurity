package Security;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.util.Base64;


public class AES {

    public static byte[] encrypt(String message , SecretKeySpec key , IvParameterSpec ivParameter)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, key , ivParameter);
        byte[] MessageEncryption = null ;
        try {
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

    public static String decrypt(byte[] encryptedObject ,
                                 SecretKeySpec key , IvParameterSpec ivParameter)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, key , ivParameter);
        try
        {
            ByteArrayInputStream iStream = new ByteArrayInputStream(encryptedObject);
            CipherInputStream cipherInputStream = new CipherInputStream(iStream, cipher);
            ObjectInputStream inputStream = new ObjectInputStream(cipherInputStream);
            SealedObject SealedEncrypt = (SealedObject) inputStream.readObject();
            return (String) SealedEncrypt.getObject(cipher) ;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null ;
    }

    public static SecretKeySpec ConvertKey2Object(String key) {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        return new SecretKeySpec(decodedKey , 0 , decodedKey.length , "AES") ;
    }

    public static String ConvertIv2String(IvParameterSpec iv) {
        byte[] IvByte = iv.getIV() ;
        return Base64.getEncoder().encodeToString(IvByte) ;
    }

    public static IvParameterSpec ConvertString2Iv(String iv) {
        byte[] IvByte = Base64.getDecoder().decode(iv) ;
        return new IvParameterSpec(IvByte) ;
    }

}
