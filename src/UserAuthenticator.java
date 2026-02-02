import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.*;

import storage.UserStorage;

/**
 * Manages user registration, authentication, and reconnection using tokens.
 * Ensures thread-safe access to the user database.
 */
public class UserAuthenticator {
    private final Map<String, User> users;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Initializes the authenticator and loads users from persistent storage.
     * 
     */
    UserAuthenticator() throws IOException {
        this.users = new HashMap<>();
        Map<String, Integer> loadedUsers = UserStorage.loadUsers();
        loadedUsers.forEach((name, hash) -> users.put(name, new User(name, hash)));
    }

    /**
     * Registers a new user with the given username and password.
     * 
     * @param username The desired username.
     * @param password The user's password.
     * @return True if registration is successful, false if the username already exists.
     */
    public boolean register(String username, String password) throws IOException {
        lock.writeLock().lock();
        try {
            if (users.containsKey(username)) return false;

            int hash = password.hashCode();
            UserStorage.saveUser(username, hash);
            users.put(username, new User(username, hash));
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Authenticates a user based on their username and password.
     * 
     * @param username The username.
     * @param password The password.
     * @return A User object if authentication is successful, null otherwise.
     */
    public User authenticate(String username, String password) {
        lock.readLock().lock();
        try {
            User user = users.get(username);
            if (user != null && user.verifyPassword(password)) {
                user.setCurrentRoom(null);
                return user;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Attempts to reconnect a user using a previously issued token.
     * Updates the user's current room from the token data.
     * 
     * @param token The token to validate and reconnect with.
     * @return The User object if the token is valid, null otherwise.
     */
    public User reconnect(String token){
        lock.readLock().lock();
        try {
            String username = TokenUtils.getUserByToken(token);
            if (username != null){
                User user = users.get(username);
                String room = TokenUtils.getRoomByToken(token);
                user.setCurrentRoom(room);
                return user;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
}
