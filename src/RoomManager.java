import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.*;

import storage.RoomStorage;

/**
 * Manages chat rooms with support for thread-safe creation and retrieval.
 * 
 * Rooms are stored in memory and persisted using {@link RoomStorage}.
 * Supports AI and non-AI rooms with associated prompts.
 */
public class RoomManager {
    private final Map<String, ChatRoom> rooms;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Initializes the RoomManager by loading existing rooms from persistent storage.
     *
     * @throws IOException if loading room data fails
     */
    public RoomManager() throws IOException {
        this.rooms = new HashMap<>();
        List<RoomStorage.RoomData> roomData = RoomStorage.loadRooms();
        roomData.forEach(data -> 
            rooms.put(data.name(), new ChatRoom(data.name(), data.isAi(), data.aiPrompt())));
    }

    /**
     * Creates a new room and persists it to storage.
     *
     * @param name   the name of the room
     * @param isAi   whether the room is AI-powered
     * @param prompt the AI prompt (can be null or empty for non-AI rooms)
     * @return true if the room was successfully created, false if the room already exists
     */
    public boolean createRoom(String name, boolean isAi, String prompt) throws IOException {
        lock.writeLock().lock();
        try {
            if (rooms.containsKey(name)) return false;

            RoomStorage.saveRoom(name, isAi, prompt);
            rooms.put(name, new ChatRoom(name, isAi, prompt));
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a room by its name.
     *
     * @param name the name of the room
     * @return the {@link ChatRoom} instance if found, or null otherwise
     */
    public ChatRoom getRoom(String name) {
        lock.readLock().lock();
        try {
            return rooms.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a list of all room names.
     *
     * @return a list containing the names of all available rooms
     */
    public List<String> getRoomNames() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rooms.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
}
