import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * The main server class for handling client connections, authentication,
 * room management, and chat interactions.
 * 
 * This class uses {@link UserAuthenticator} for login/register/reconnect,
 * {@link RoomManager} to manage chat rooms (including AI rooms),
 * and handles each client in a virtual thread for scalability.
 */
public class ChatServer {
    private final int port;
    private final UserAuthenticator authenticator;
    private final RoomManager roomManager;
    private static final String KEYSTORE_PATH = "database/keystores/tls_keystore.jks";
    private static final String KEYSTORE_PASS = "123456";

    /**
     * Constructs a ChatServer instance with the given port.
     *
     * @param port the port number to bind the server to
     * @throws IOException if initialization of RoomManager fails
     */
    public ChatServer(String port) throws IOException {
        this.port = Integer.parseInt(port);
        this.authenticator = new UserAuthenticator();
        this.roomManager = new RoomManager();
    }

    /**
     * Starts the chat server and listens for incoming client connections.
     * Each client is handled in a separate virtual thread.
     * @throws Exception 
     */
    public void start() throws Exception {
        try {
            SSLContext sslContext = TLSConfig.createServerSSLContext(KEYSTORE_PATH, KEYSTORE_PASS);
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
            System.out.println("Chat server (TLS) started on port " + port);
            
            while (true) {
                Socket clientSocket = sslServerSocket.accept();
                Thread.startVirtualThread(() -> {
                    handleClient(clientSocket);
                });
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Handles the authentication process (login, register, or reconnect).
     *
     * @param in  the input stream from the client
     * @param out the output stream to the client
     * @return the authenticated {@link User}, or null if the client disconnects
     * @throws IOException if an I/O error occurs during communication
     */
    private User authenticateUser(BufferedReader in, PrintWriter out) throws IOException {
        while (true) {
            out.println("AUTH Choose: \n1. Login \n2. Register \n3. Reconnect \n4. Exit");
            String choice = in.readLine();
            
            if (choice == null) return null;
    
            String username;
            switch (choice) {
                case "1":
                    out.println("AUTH Enter username:");
                    username = in.readLine();
                    out.println("AUTH Enter password:");
                    String password = in.readLine();
                    
                    User user = authenticator.authenticate(username, password);
                    if (user != null) {
                        String token = TokenUtils.createToken(user.getUsername(), "");
                        out.println("AUTH_SUCCESS Welcome " + username + " Token: " + token);
                        return user;
                    }
                    out.println("AUTH_FAIL Invalid credentials");
                    break;

                case "2":
                    out.println("AUTH Enter username:");
                    username = in.readLine();
                    out.println("AUTH Enter password:");
                    password = in.readLine();
                    
                    if (authenticator.register(username, password)) {
                        out.println("AUTH_SUCCESS Account created. Please login.");
                    } else {
                        out.println("AUTH_FAIL Username already exists");
                    }
                    break;

                case "3":
                    out.println("Enter your reconnection token:");
                    String token = in.readLine();
                    user = authenticator.reconnect(token);
                    if (user != null) {
                        String room = TokenUtils.getRoomByToken(token);
                        username = user.getUsername();
                        String newToken = TokenUtils.createToken(username, room == null ? "" : room);
                        out.println("AUTH_SUCCESS Welcome back " + username + ".\nYour new token is " + newToken);
                        if (room != null && !room.isEmpty()) {
                            out.println("You were reconnected to " + room);
                        }
                        return user;
                    }
                    out.println("AUTH_FAIL Invalid or expired token");
                    break;

                case "4":
                    out.println("_/#EXIT$%_");
                    return null;

                default:
                    out.println("AUTH_FAIL Invalid choice");
            }
        }
    }

    /**
     * Displays available rooms to the user, lets them create or join a room,
     * and handles the messaging loop within that room.
     *
     * @param user the authenticated user
     * @param in   the input stream from the client
     * @param out  the output stream to the client
     * @throws IOException if an I/O error occurs
     */
    private void chatLoop(User user, BufferedReader in, PrintWriter out) throws IOException {
        while (true) {
            List<String> roomNames = roomManager.getRoomNames();
            StringBuilder roomsList = new StringBuilder("ROOMS Available rooms:\n");
            for (String name : roomNames) {
                ChatRoom room = roomManager.getRoom(name);
                roomsList.append("- ").append(name);
                if (room != null && room.isAiRoom()) {
                    roomsList.append(" (AI Room) [Prompt: " + room.getAiRoomHandler().getPrompt() + "]");
                }
                roomsList.append("\n");
            }
            out.println(roomsList.toString());
            out.println("ROOMS Enter room name to join/create (or /exit to logout and disconnect):");
            
            String roomName = in.readLine();
            if (roomName == null) {
                return;
            }
            
            if (roomName.equalsIgnoreCase("/exit")) {
                TokenUtils.deleteTokensForUser(user.getUsername());
                return;
            }
            
            boolean isAiRoom = false;
            String aiPrompt = null;

            if (roomManager.getRoom(roomName) == null) {
                out.println("ROOM_TYPE Do you want to create an AI room? \n1. Yes \n2. No");
                String aiChoice = in.readLine();
                if ("1".equals(aiChoice)) {
                    isAiRoom = true;
                    out.println("ROOM_PROMPT Enter the AI prompt/topic for this room:");
                    aiPrompt = in.readLine();
                    if (aiPrompt == null || aiPrompt.trim().isEmpty()) {
                        aiPrompt = "General Chat";
                    }
                }
            }

            ChatRoom room = roomManager.getRoom(roomName);
            if (room == null) {
                if (!roomManager.createRoom(roomName, isAiRoom, aiPrompt)) {
                    out.println("ERROR Room creation failed");
                    continue;
                }
                room = roomManager.getRoom(roomName);
            }

            out.println("\nRoom: " + roomName + (room.isAiRoom() ? " (AI Room) [Prompt: " + room.getAiRoomHandler().getPrompt() + "]" : ""));
            room.addParticipant(user, out);
            user.setCurrentRoom(roomName);

            String input;
            while ((input = in.readLine()) != null) {
                if (input.equalsIgnoreCase("/exit")) {
                    String leaveMessage = room.removeParticipant(user);
                    out.println(leaveMessage);
                    user.setCurrentRoom(null);
                    break;
                }
                room.addMessage(user, input);
            }
        }
    }

    /**
     * Handles a single client connection, including authentication and chat room interaction.
     *
     * @param clientSocket the socket associated with the client
     */
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            User user = authenticateUser(in, out);
            if (user != null) {
                if (user.getCurrentRoom() != null) {
                    String input;
                    ChatRoom room = roomManager.getRoom(user.getCurrentRoom());
                    room.reconnectParticipant(user, out);
                    while ((input = in.readLine()) != null) {
                        if (input.equalsIgnoreCase("/exit")) {
                            String leaveMessage = room.removeParticipant(user);
                            out.println(leaveMessage);
                            user.setCurrentRoom(null);
                            break;
                        }
                        room.addMessage(user, input);
                    }
                }
                chatLoop(user, in, out);
            }
        } catch (IOException e) {
            System.err.println("Client handling error: " + e.getMessage());
        }
    }

    /**
     * Entry point of the server application.
     *
     * @param args command-line arguments; expects the port number as the first argument
     */
    public static void main(String[] args) throws Exception {
        new ChatServer(args[0]).start();
    }
}
