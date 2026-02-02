import java.net.*;
import java.io.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * ChatClient connects to a ChatServer to send and receive messages.
 * It supports reconnection attempts and handles server communication asynchronously.
 */
public class ChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = true;
    private static final String TRUSTSTORE_PATH = "database/keystores/tls_keystore.jks";
    private static final String TRUSTSTORE_PASS = "123456";

    /**
     * Starts the client and connects to the chat server.
     * Retries connection up to 3 times on failure.
     *
     * @param host the hostname or IP address of the server
     * @param port the port number as a string
     */
    public void start(String host, String port) throws Exception {
        int attempts = 0;
        final int maxAttempts = 3;

        while (attempts < maxAttempts) {
            try {
                SSLContext sslContext = TLSConfig.createClientSSLContext(TRUSTSTORE_PATH, TRUSTSTORE_PASS);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                socket = sslSocketFactory.createSocket(host, Integer.parseInt(port));
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Thread.startVirtualThread(this::listenForMessages);

                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                while (running) {
                    if (System.in.available() > 0) {
                        String input = consoleReader.readLine();
                        if (input != null) {
                            out.println(input);
                            if ("_/#EXIT$%_".equalsIgnoreCase(input.trim())) {
                                running = false;
                                break;
                            }
                        }
                    }
                }
                attempts = 999; // Indicates successful connection
            } catch (IOException e) {
                attempts++;
                System.err.println("Connection attempt " + attempts + " failed");
                if (attempts < maxAttempts) {
                    try {
                        Thread.sleep(attempts*1000); //Linear timeouts
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                closeResources();
            }
        }

        if (attempts != 999) {
            System.err.println(attempts >= maxAttempts
                    ? "Failed to connect after " + maxAttempts + " attempts"
                    : "Disconnected.");
        } else {
            System.out.println("Disconnected");
        }
    }

    /**
     * Listens for messages from the server and prints them to the console.
     * Terminates when the server sends a special disconnect signal.
     */
    private void listenForMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                if (message.equals("_/#EXIT$%_")) {
                    running = false;
                    return;
                }
                System.out.println(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    /**
     * Closes all network and I/O resources safely.
     */
    private void closeResources() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Entry point for the client application.
     *
     * @param args expects one argument: the port number
     */
    public static void main(String[] args) throws Exception {
        new ChatClient().start("localhost", args[0]);
    }
}
