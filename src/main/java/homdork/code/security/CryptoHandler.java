package homdork.code.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

/**
 * Houses cipher setup, encryption and decryption methods
 * Uses key from @KeyLoader
 */
public class CryptoHandler {

    Cipher cipher = null;
    Key key = null;
    SecretKeySpec secretKey;
    byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

    public void setUpCipher() throws Exception{
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    public String aesDecrypt(byte[] cipherText) throws Exception {
        String encryptedString;
        byte[] encryptText = null;

        encryptText = Base64.getDecoder().decode(cipherText);
        cipher.init(Cipher.DECRYPT_MODE, KeyLoader.loader(), ivParameterSpec);
        encryptedString = new String(cipher.doFinal(encryptText));

        return encryptedString;
    }
}
