import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.locks.*;

import storage.MessageStorage;

/**
 * Represents a chat room which can be either a regular or AI-enhanced room.
 * Handles participant management, message broadcasting, and message history storage.
 */
public class ChatRoom {
    private final String name;
    private final boolean isAiRoom;
    private final AiRoomHandler aiHandler;
    private final Map<String, PrintWriter> participantConnections = new HashMap<>();
    private final ReentrantReadWriteLock roomLock = new ReentrantReadWriteLock();

    /**
     * Constructs a ChatRoom.
     *
     * @param name     the name of the room
     * @param isAiRoom true if this is an AI-enhanced room
     * @param aiPrompt the prompt used to initialize the AI (if AI room)
     */
    public ChatRoom(String name, boolean isAiRoom, String aiPrompt) {
        this.name = name;
        this.isAiRoom = isAiRoom;
        this.aiHandler = isAiRoom ? new AiRoomHandler(aiPrompt) : null;
    }

    /**
     * Returns the name of the chat room.
     *
     * @return the room name
     */
    public String getName() {
        return name;
    }

    /**
     * Indicates whether the room is AI-enhanced.
     *
     * @return true if it's an AI room
     */
    public boolean isAiRoom() {
        return isAiRoom;
    }

    /**
     * Returns the AI room handler associated with this chat room.
     * This is only relevant if the room is an AI-enabled room.
     *
     * @return the {@link AiRoomHandler} instance, or {@code null} if not an AI room
     */
    public AiRoomHandler getAiRoomHandler() {
        return aiHandler;
    }

    /**
     * Adds a participant to the room and announces their entry.
     *
     * @param user the user joining
     * @param out  the output stream associated with the user
     */
    public void addParticipant(User user, PrintWriter out) {
        roomLock.writeLock().lock();
        try {
            participantConnections.put(user.getUsername(), out);
            out.println("To exit the room type /exit.");
            broadcastSystemMessage('[' + user.getUsername() + " enters the room]");
        } finally {
            roomLock.writeLock().unlock();
        }
    }

    /**
     * Reconnects a previously connected participant and announces their return.
     *
     * @param user the user reconnecting
     * @param out  the output stream associated with the user
     */
    public void reconnectParticipant(User user, PrintWriter out) {
        roomLock.writeLock().lock();
        try {
            participantConnections.put(user.getUsername(), out);
            out.println("To exit the room type /exit.");
            broadcastSystemMessage('[' + user.getUsername() + " reconnected to the room]");
        } finally {
            roomLock.writeLock().unlock();
        }
    }

    /**
     * Removes a participant from the room and announces their departure.
     *
     * @param user the user leaving
     * @return the departure message broadcast to others
     */
    public String removeParticipant(User user) {
        roomLock.writeLock().lock();
        try {
            participantConnections.remove(user.getUsername());
            String leaveMessage = '[' + user.getUsername() + " leaves the room]";
            broadcastSystemMessage(leaveMessage);
            return leaveMessage;
        } finally {
            roomLock.writeLock().unlock();
        }
    }

    /**
     * Adds a message to the room and broadcasts it to participants.
     * If this is an AI room, an AI-generated response is also broadcast.
     *
     * @param user    the sender
     * @param message the message text
     */
    public void addMessage(User user, String message) throws IOException {
        roomLock.writeLock().lock();
        try {
            MessageStorage.saveMessage(name, user.getUsername(), message);
            String formattedMsg = user.getUsername() + ": " + message;
            broadcastToAll(formattedMsg, user);
            
            if (isAiRoom) {
                String aiResponse = aiHandler.processMessage(formattedMsg);
                MessageStorage.saveMessage(name, "Bot", aiResponse);
                broadcastToAll("Bot: " + aiResponse, null);
            }
        } finally {
            roomLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the message history for this room from storage.
     *
     * @return a list of messages, or an empty list if loading fails
     */
    public List<String> getMessageHistory() {
        roomLock.readLock().lock();
        try {
            return MessageStorage.loadMessages(name);
        } catch (IOException e) {
            System.err.println("Failed to load messages: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            roomLock.readLock().unlock();
        }
    }

    /**
     * Broadcasts a message to all participants except the sender (if specified).
     *
     * @param message the message to broadcast
     * @param sender  the user to exclude, or null to broadcast to all
     */
    private void broadcastToAll(String message, User sender) {
        roomLock.readLock().lock();
        try {
            for (Map.Entry<String, PrintWriter> out : participantConnections.entrySet()) {
                if (sender == null || !out.getKey().equals(sender.getUsername())) {
                    out.getValue().println(message);
                }
            }
        } finally {
            roomLock.readLock().unlock();
        }
    }

    /**
     * Broadcasts a system message to all participants.
     *
     * @param message the system message to send
     */
    private void broadcastSystemMessage(String message) {
        roomLock.writeLock().lock();
        try {
            broadcastToAll(message, null);
        } finally {
            roomLock.writeLock().unlock();
        }
    }
}
