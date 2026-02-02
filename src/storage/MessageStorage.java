package storage;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Provides persistent storage and retrieval of chat messages per room.
 */
public class MessageStorage {
    private static final Path MSG_DIR = Paths.get("database/messages");
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    static { // Tries to create a dir called "messages"
        try {
            Files.createDirectories(MSG_DIR);
        } catch (IOException e) {
            System.err.println("Failed to create messages directory: " + e.getMessage());
        }
    }

    /**
     * Saves a message to the appropriate room's message file.
     *
     * @param roomName the name of the room
     * @param username the user who sent the message
     * @param content  the message content
     */
    public static void saveMessage(String roomName, String username, String content) throws IOException {
        lock.writeLock().lock();
        try {
            Path roomFile = MSG_DIR.resolve(roomName + ".msg");
            String entry = System.currentTimeMillis() + "|" + username + "|" + content + "\n";
            Files.writeString(roomFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads all messages from a room's message file.
     *
     * @param roomName the name of the room
     * @return a list of formatted message strings
     */
    public static List<String> loadMessages(String roomName) throws IOException {
        lock.readLock().lock();
        try {
            Path roomFile = MSG_DIR.resolve(roomName + ".msg");
            if (!Files.exists(roomFile)) return new ArrayList<>();
            
            List<String> messages = new ArrayList<>();
            for (String line : Files.readAllLines(roomFile)) {
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) {
                    messages.add(parts[1] + ": " + parts[2]);
                }
            }
            return messages;
        } finally {
            lock.readLock().unlock();
        }
    }
}