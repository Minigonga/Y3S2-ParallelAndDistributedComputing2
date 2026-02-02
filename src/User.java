import java.util.concurrent.locks.*;

/**
 * Represents a user with a username, password hash, and an optional room assignment.
 * Provides thread-safe access and updates to the current room.
 */
public class User {
    private final String username;
    private final int passwordHash;
    private String currentRoom;
    private final ReentrantLock userLock = new ReentrantLock();


    /**
     * Constructs a new User instance.
     * 
     * @param username The username of the user.
     * @param passwordHash The hashed password of the user.
     */
    public User(String username, int passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the username of the user.
     * 
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Verifies the password by comparing its hash to the stored hash.
     * 
     * @param password The password to verify.
     * @return True if the password is correct, false otherwise.
     */
    public boolean verifyPassword(String password) {
        return this.passwordHash == password.hashCode();
    }

    /**
     * Gets the current room the user is in.
     * 
     * @return The current room name.
     */
    public String getCurrentRoom() {
        userLock.lock();
        try {
            return currentRoom;
        } finally {
            userLock.unlock();
        }
    }

    /**
     * Sets the current room of the user and updates associated tokens.
     * 
     * @param roomName The name of the room to set.
     */
    public void setCurrentRoom(String roomName) {
        userLock.lock();
        try {
            this.currentRoom = roomName;
            TokenUtils.updateUserTokensRoom(this.username, roomName);
        } finally {
            userLock.unlock();
        }
    }

    // Example of stronger password hashing (disabled in current version)
    // private static int hashPassword(String password) {
    //     return Arrays.hashCode(MessageDigest.getInstance("SHA-256").digest(password.getBytes()));
    // }
}