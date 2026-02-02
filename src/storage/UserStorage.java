package storage;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Provides functionality to persist and retrieve user credentials from a file.
 */
public class UserStorage {
    private static final Path USER_FILE = Paths.get("database/users.dat");
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Saves a new user's username and password hash to the storage file.
     *
     * @param username     the username to save
     * @param passwordHash the hashed password
     */
    public static void saveUser(String username, int passwordHash) throws IOException {
        lock.writeLock().lock();
        try {
            Files.writeString(USER_FILE, 
                username + ":" + passwordHash + "\n",
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads all users from the storage file into a map.
     *
     * @return a map of usernames to their password hashes
     */
    public static Map<String, Integer> loadUsers() throws IOException {
        lock.readLock().lock();
        try {
            if (!Files.exists(USER_FILE)) return new HashMap<>();
            
            Map<String, Integer> users = new HashMap<>();
            Files.lines(USER_FILE).forEach(line -> {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], Integer.parseInt(parts[1]));
                }
            });
            return users;
        } finally {
            lock.readLock().unlock();
        }
    }
}