package migrator.helpers;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * Created by Walter Ego on 29.04.2017.
 */
public class Encryptor {
    private static final byte[] encryptionKeyValue =
            new byte[] { 'L', 'e', 'v', 'i', 't', 'a', 't', 'i', 'o', 'n', 'N', 'a', 't', 'i', 'o', 'n' };

    public static String encrypt(String value) throws Exception {
        Key key = new SecretKeySpec(encryptionKeyValue, "AES");
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encValue = c.doFinal(value.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encValue);
        return encryptedValue;
    }

    public static String decrypt(String value) throws Exception {
        Key key = new SecretKeySpec(encryptionKeyValue, "AES");
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(value);
        byte[] decValue = c.doFinal(decordedValue);
        return new String(decValue);
    }
}
