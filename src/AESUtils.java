import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility class providing AES encryption and decryption using GCM mode.
 * This class uses AES/GCM/NoPadding for authenticated encryption, which ensures both confidentiality and integrity of the data.
 */
public class AESUtils {

    /**
     * Encrypts a plaintext string using AES with GCM mode and no padding.
     *
     * @param secretKey the AES secret key used for encryption
     * @param plaintext the plaintext string to encrypt
     * @return a Base64-encoded string containing the IV and ciphertext
     */
    public static String encrypt(SecretKey secretKey, String plaintext) {
        try {
            // Generate a random IV (12 bytes for GCM)
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher in encryption mode
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // Encrypt the plaintext
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            byte[] encryptedIVAndText = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedIVAndText, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedIVAndText, iv.length, encrypted.length);

            // Return as Base64-encoded string
            return Base64.getEncoder().encodeToString(encryptedIVAndText);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded string that was encrypted using AES with GCM mode.
     *
     * @param secretKey the AES secret key used for decryption
     * @param ciphertext the Base64-encoded string containing the IV and encrypted data
     * @return the decrypted plaintext string
     */
    public static String decrypt(SecretKey secretKey, String ciphertext) {
        try {
            // Decode Base64
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(decoded, 0, 12); // Extract IV (first 12 bytes)
            byte[] encrypted = Arrays.copyOfRange(decoded, 12, decoded.length); // Extract ciphertext

            // Initialize cipher in decryption mode
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // Decrypt and return plaintext
            byte[] original = cipher.doFinal(encrypted);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }
}
