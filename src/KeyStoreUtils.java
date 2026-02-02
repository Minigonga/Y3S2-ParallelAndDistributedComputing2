import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Utility class for loading AES secret keys from a Java KeyStore.
 *
 * This class supports loading keys stored in a PKCS12 keystore.
 */
public class KeyStoreUtils {

    /**
     * Loads an AES secret key from a PKCS12 keystore file.
     *
     * @param keystorePath     the file system path to the keystore file
     * @param keystorePassword the password used to unlock the keystore
     * @param keyAlias         the alias under which the AES key is stored
     * @param keyPassword      the password used to protect the key entry
     * @return the {@link SecretKey} retrieved from the keystore
     */
    public static SecretKey loadAESKey(String keystorePath, String keystorePassword, String keyAlias, String keyPassword) {
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, keystorePassword.toCharArray());

            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keyPassword.toCharArray());
            KeyStore.SecretKeyEntry skEntry = (KeyStore.SecretKeyEntry) ks.getEntry(keyAlias, protParam);

            return skEntry.getSecretKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load secret key from keystore", e);
        }
    }
}