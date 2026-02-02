import javax.crypto.SecretKey;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class for managing user tokens using AES encryption.
 * Tokens are stored in a file and encrypted with a key loaded from a Java KeyStore.
 * Provides functionality for creating, validating, updating, and deleting tokens.
 */
public class TokenUtils {
    private static final String TOKEN_FILE = "database/tokens.dat";
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final String KEYSTORE_PATH = "database/keystores/token_keystore.jks";
    private static final String KEYSTORE_PASS = "123456";
    private static final String KEY_ALIAS = "tokensecretkey";
    private static final String KEY_PASS = "123456";

    // Load AES key once from keystore
    private static final SecretKey secretKey;

    static {
        secretKey = KeyStoreUtils.loadAESKey(KEYSTORE_PATH, KEYSTORE_PASS, KEY_ALIAS, KEY_PASS);
    }

    /**
     * Encrypts a raw token string using the preloaded AES secret key.
     *
     * @param token the plaintext token to encrypt
     * @return the encrypted token string
     */
    private static String encryptToken(String token) {
        return AESUtils.encrypt(secretKey, token);
    }

    /**
     * Decrypts an encrypted token string using the preloaded AES secret key.
     *
     * @param encryptedToken the encrypted token string
     * @return the original plaintext token
     */
    private static String decryptToken(String encryptedToken) {
        return AESUtils.decrypt(secretKey, encryptedToken);
    }

    /**
     * Creates a new token for the user, removing any existing tokens for that username
     * 
     * @param username The username to associate with the token
     * @param room The room to associate with the token
     * @return The newly created token
     */
    public static String createToken(String username, String room) {
        lock.writeLock().lock();
        try {
            removeTokensForUser(username);

            String rawToken = UUID.randomUUID().toString();
            String encryptedToken = encryptToken(rawToken);

            long expiresAt = Instant.now().plusSeconds(2 * 3600).getEpochSecond();

            String tokenRecord = String.join(",", encryptedToken, String.valueOf(expiresAt), username, room);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE, true))) {
                writer.write(tokenRecord);
                writer.newLine();
                return rawToken; // return raw (unencrypted) token to user
            } catch (IOException e) {
                throw new RuntimeException("Failed to write token to file", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all tokens associated with a specific username
     * 
     * @param username The username whose tokens should be removed
     */
    private static void removeTokensForUser(String username) {
        try {
            File file = new File(TOKEN_FILE);
            if (!file.exists()) return;

            List<String> validTokens = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3 && !parts[2].equals(username)) {
                        validTokens.add(line);
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String token : validTokens) {
                    writer.write(token);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove tokens for user", e);
        }
    }

    /**
     * Validates if a token exists and is not expired
     * 
     * @param token The token to validate
     * @return Map containing user info if valid, null otherwise
     */
    public static Map<String, String> validateToken(String token) {
        lock.readLock().lock();
        try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String encryptedToken = parts[0];
                    String decryptedToken = decryptToken(encryptedToken);
                    if (decryptedToken.equals(token)) {
                        long expiresAt = Long.parseLong(parts[1]);
                        if (Instant.now().getEpochSecond() < expiresAt) {
                            Map<String, String> result = new HashMap<>();
                            result.put("username", parts[2]);
                            result.put("room", parts[3]);
                            result.put("expiresAt", parts[1]);
                            return result;
                        }
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tokens file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Deletes expired tokens from the file
     */
    public static void cleanupExpiredTokens() {
        lock.writeLock().lock();
        try {
            File file = new File(TOKEN_FILE);
            if (!file.exists()) return;

            List<String> validTokens = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long currentTime = Instant.now().getEpochSecond();
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        long expiresAt = Long.parseLong(parts[1]);
                        if (expiresAt > currentTime) {
                            validTokens.add(line);
                        }
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String token : validTokens) {
                    writer.write(token);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to cleanup tokens", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * Updates the room and sets expiration to 2 hours from now for all tokens associated with a username
     * 
     * @param username The username whose tokens should be updated
     * @param newRoom The new room to associate with the tokens
     * @return true if at least one token was found and updated, false otherwise
     */
    public static boolean updateUserTokensRoom(String username, String newRoom) {
        lock.writeLock().lock();
        try {
            File file = new File(TOKEN_FILE);
            if (!file.exists()) return false;
            if (newRoom == null){
                newRoom = "";
            }
            List<String> updatedTokens = new ArrayList<>();
            boolean found = false;
            long newExpiration = Instant.now().plusSeconds(2 * 3600).getEpochSecond();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3 && parts[2].equals(username)) {
                        // update expiration and room but keep the encrypted token as is
                        String updatedLine = String.join(",",
                                parts[0],
                                String.valueOf(newExpiration),
                                parts[2],
                                newRoom
                        );
                        updatedTokens.add(updatedLine);
                        found = true;
                    } else {
                        updatedTokens.add(line);
                    }
                }
            }

            if (found) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    for (String updatedToken : updatedTokens) {
                        writer.write(updatedToken);
                        writer.newLine();
                    }
                }
            }

            return found;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update user tokens room", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the username associated with a valid token by reading directly from the tokens file
     * 
     * @param token The token to look up
     * @return Username if token is valid, null otherwise
     */
    public static String getUserByToken(String token) {
        lock.readLock().lock();
        try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String encryptedToken = parts[0];
                    String decryptedToken = decryptToken(encryptedToken);
                    if (decryptedToken.equals(token)) {
                        long expiresAt = Long.parseLong(parts[1]);
                        if (Instant.now().getEpochSecond() < expiresAt) {
                            return parts[2];
                        }
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tokens file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the room associated with a valid token by reading directly from the tokens file
     * 
     * @param token The token to look up
     * @return Room name if token is valid and has a room, empty string if no room, null if invalid token
     */
    public static String getRoomByToken(String token) {
        lock.readLock().lock();
        try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String encryptedToken = parts[0];
                    String decryptedToken = decryptToken(encryptedToken);
                    if (decryptedToken.equals(token)) {
                        long expiresAt = Long.parseLong(parts[1]);
                        if (Instant.now().getEpochSecond() < expiresAt) {
                            return parts[3];
                        }
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tokens file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Deletes all tokens associated with the given username.
     *
     * @param username The username whose tokens should be deleted
     * @return true if at least one token was found and deleted, false otherwise
     */
    public static boolean deleteTokensForUser(String username) {
        lock.writeLock().lock();
        try {
            File file = new File(TOKEN_FILE);
            if (!file.exists()) return false;

            List<String> remainingTokens = new ArrayList<>();
            boolean found = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3 && parts[2].equals(username)) {
                        found = true;
                        // skip line to delete
                    } else {
                        remainingTokens.add(line);
                    }
                }
            }

            if (found) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    for (String token : remainingTokens) {
                        writer.write(token);
                        writer.newLine();
                    }
                }
            }

            return found;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete tokens for user", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
