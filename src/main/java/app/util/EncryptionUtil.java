package app.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EncryptionUtil {

    private static final Logger LOGGER = Logger.getLogger(EncryptionUtil.class.getName());

    /**
     * To use with JBOSS add to standalone.xml
     * <system-properties>
     *     <property name="my.app.encryption.key" value="localhost_dev_key"/>
     * </system-properties>
     * */
    // IMPORTANT: In a real application, the encryption key should NEVER be hardcoded.
    // It should be loaded securely from environment variables, a key vault,
    // or a secure configuration management system.
    private static final String ENCRYPTION_KEY_STRING = System.getProperty("app.encryption.key", "localhost_dev_key_qdCdeXn9QMbYoD");
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; // CBC mode with PKCS5 padding

    private static SecretKey secretKey;

    static {
        try {
            // Ensure the key is 16, 24, or 32 bytes for AES-128, AES-192, or AES-256
            byte[] keyBytes = ENCRYPTION_KEY_STRING.getBytes("UTF-8");
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                // Adjust key length or throw an error based on your requirements
                LOGGER.severe("Encryption key length is not 16, 24, or 32 bytes. Adjusting to 32 bytes.");
                byte[] paddedKeyBytes = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKeyBytes, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKeyBytes;
            }
            secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize encryption key.", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    // Generates a random IV for each encryption. This is CRUCIAL for security.
    private static byte[] generateIV() {
        byte[] iv = new byte[16]; // 16 bytes for AES block size
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv = generateIV();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Prepend IV to the encrypted data for decryption later
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);

        // Extract IV
        byte[] iv = new byte[16];
        System.arraycopy(decodedBytes, 0, iv, 0, 16);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Extract actual encrypted data
        byte[] encryptedBytes = new byte[decodedBytes.length - 16];
        System.arraycopy(decodedBytes, 16, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, "UTF-8");
    }

    public static void main(String[] args) throws Exception {

        // generate a new 32-byte key
        /**
        String base = "localhost_dev_key_";
        int targetLength = 32;
        int padLength = targetLength - base.length();
        String padded = base + RandomStringUtils.randomAlphanumeric(padLength);
        System.out.println("Padded key: " + padded + " (length: " + padded.length() + ")");
         * */

        System.out.println("username: " + EncryptionUtil.encrypt("usuario"));
        System.out.println("pass: " + EncryptionUtil.encrypt("senha"));
        System.out.println("app1_jdbc: " + EncryptionUtil.encrypt("jdbc:postgresql://localhost:5432/servlet_customer001"));
        System.out.println("app2_jdbc: " + EncryptionUtil.encrypt("jdbc:postgresql://localhost:5432/servlet_customer002"));
    }

    // FOR PRODUCTION: Load the key securely. Examples:
    // private static String loadKeyFromEnvironmentVariable() {
    //     return System.getenv("ENCRYPTION_KEY");
    // }
    // private static String loadKeyFromVault() {
    //     // Logic to retrieve key from HashiCorp Vault, AWS Secrets Manager, etc.
    // }
}