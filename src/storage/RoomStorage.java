package storage;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Manages persistence of chat room definitions, including AI configuration.
 */
public class RoomStorage {
    private static final Path ROOM_FILE = Paths.get("database/rooms.dat");
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Saves a new room definition to the persistent storage.
     *
     * @param name   the room name
     * @param isAi   whether the room uses AI
     * @param prompt the AI prompt (null for non-AI rooms)
     */
    public static void saveRoom(String name, boolean isAi, String prompt) throws IOException {
        lock.writeLock().lock();
        try {
            Files.writeString(ROOM_FILE, 
                name + "|" + isAi + "|" + (prompt != null ? prompt : "") + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads all saved rooms from storage.
     *
     * @return a list of room definitions
     */
    public static List<RoomData> loadRooms() throws IOException {
        lock.readLock().lock();
        try {
            if (!Files.exists(ROOM_FILE)) return new ArrayList<>();
            
            List<RoomData> rooms = new ArrayList<>();
            for (String line : Files.readAllLines(ROOM_FILE)) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 3) {
                    String prompt = parts[2].isEmpty() ? null : parts[2];
                    rooms.add(new RoomData(parts[0], Boolean.parseBoolean(parts[1]), prompt));
                }
            }
            return rooms;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Represents a data structure for a saved room.
     *
     * @param name     the room name
     * @param isAi     whether it's an AI room
     * @param aiPrompt the prompt used by the AI, if applicable
     */
    public record RoomData(String name, boolean isAi, String aiPrompt) {}
}