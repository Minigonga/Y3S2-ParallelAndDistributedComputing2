import javax.net.ssl.*;
import java.io.*;
import java.security.*;

/**
 * Utility class for configuring TLS connections using Java's SSLContext.
 * Provides methods to create SSLContext instances for both server and client using PKCS12 keystores.
 */
public class TLSConfig {

    /**
     * Creates an {@link SSLContext} for a server by loading a keystore containing the server's private key and certificate.
     *
     * @param keystorePath      the file path to the keystore (PKCS12 format)
     * @param keystorePassword  the password to access the keystore and the private key
     * @return initialized SSLContext for server-side use
     */
    public static SSLContext createServerSSLContext(String keystorePath, String keystorePassword) throws Exception {

        // 1. Load Keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(keystorePath)) {
            keyStore.load(is, keystorePassword.toCharArray());
        }

        // 2. Initialize KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 3. Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext;
    }

    /**
     * Creates an {@link SSLContext} for a client by loading a truststore containing trusted certificates.
     *
     * @param truststorePath     the file path to the truststore (PKCS12 format)
     * @param truststorePassword the password to access the truststore
     * @return initialized SSLContext for client-side use
     */
    public static SSLContext createClientSSLContext(String truststorePath, String truststorePassword) throws Exception {

        // 1. Load Truststore
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(truststorePath)) {
            trustStore.load(is, truststorePassword.toCharArray());
        }

        // 2. Initialize TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        // 3. Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext;
    }
}