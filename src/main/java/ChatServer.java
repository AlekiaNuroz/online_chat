import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;

/**
 * A multithreaded chat server that manages multiple clients and chat rooms.
 * Supports room creation with passwords, user muting/banning, private messages,
 * and administrative commands. Thread-safe using concurrent collections and
 * synchronized blocks where necessary.
 */
public class ChatServer {
    /** Default admin username for server administration commands */
    private static final String SERVER_ADMIN = "admin";

    /** Thread-safe set of all connected client handlers */
    private static final Set<ClientHandler> allClients = Collections.synchronizedSet(new HashSet<>());
    /** Atomic counter for generating default usernames */
    private static int userCount = 0;

    /** Map of room names to their members (client handlers) */
    private static final Map<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    /** Map of room names to their passwords (null indicates no password) */
    private static final Map<String, String> roomPasswords = new ConcurrentHashMap<>();
    /** Map of room names to their admin users */
    private static final Map<String, Set<String>> roomAdmins = new ConcurrentHashMap<>();
    /** Map of muted usernames to their mute expiration timestamps */
    private static final Map<String, Long> mutedUsers = new ConcurrentHashMap<>();
    /** Map of room names to banned IP addresses */
    private static final Map<String, Set<String>> roomBans = new ConcurrentHashMap<>();
    /** Map of room names to their creator's username */
    private static final Map<String, String> roomCreators = new ConcurrentHashMap<>();

    /** Thread pool for handling client connections */
    private static final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    /** Logger for server-wide events */
    private static final Logger serverLogger = Logger.getLogger(ChatServer.class.getName());

    /** Initializes the default 'General' room */
    public ChatServer() {
        rooms.putIfAbsent("General", Collections.synchronizedSet(new HashSet<>()));
    }

    /**
     * Starts the chat server on port 12345. Listens for client connections,
     * assigns default usernames, and submits client handlers to the thread pool.
     *
     * @param args Command-line arguments (unused)
     */
    public static void main(String[] args) {
        // Configure logging to file
        try {
            FileHandler fileHandler = new FileHandler("chatserver.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            serverLogger.addHandler(fileHandler);
            serverLogger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to set up server logger: " + e.getMessage());
        }

        serverLogger.log(Level.INFO, "Server starting on port 12345...");
        rooms.putIfAbsent("General", Collections.synchronizedSet(new HashSet<>()));
        System.out.println("Server started on port 12345...");

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                userCount++;
                ClientHandler handler = new ClientHandler(clientSocket, "User" + userCount);
                allClients.add(handler);

                rooms.get("General").add(handler);
                clientThreadPool.submit(handler);
            }
        } catch (IOException e) {
            serverLogger.log(Level.SEVERE, "Server exception: " + e.getMessage(), e);
        } finally {
            clientThreadPool.shutdown();
            System.out.println("Server shut down.");
            serverLogger.log(Level.INFO, "Server shut down.");
        }
    }

    /**
     * Handles communication with a single client. Processes commands, room joins,
     * messaging, and administrative actions. Maintains per-client state including
     * username, current room, and connection details.
     */
    static class ClientHandler implements Runnable {
        /** Logger for client-specific events */
        private final Logger clientLogger;
        private final Socket socket;
        private PrintWriter out;
        private String username;
        private String currentRoom = "General";
        /** ANSI color code for client's messages */
        private String userColor = ANSI_COLORS.get("reset");
        /** Client's IP address (simulated in development) */
        private final String clientIP;
        /** Counter for generating simulated IPs in development */
        private static int clientCounter = 1;

        /** ANSI color codes mapped to names */
        private static final Map<String, String> ANSI_COLORS = Map.of(
                "red", "\u001B[31m",
                "green", "\u001B[32m",
                "yellow", "\u001B[33m",
                "blue", "\u001B[34m",
                "purple", "\u001B[35m",
                "cyan", "\u001B[36m",
                "reset", "\u001B[0m"
        );

        /**
         * @param socket Client connection socket
         * @param username Initial username assigned to client
         */
        ClientHandler(Socket socket, String username) {
            this.socket = socket;
            this.username = username;
            this.clientLogger = Logger.getLogger(ClientHandler.class.getName() + "." + username);
            this.clientLogger.setLevel(Level.INFO);

            // Simulate different IPs in development environment
            if (System.getenv("Environment").equals("Local")) {
                this.clientIP = "127.0.0." + (clientCounter++);
            } else {
                this.clientIP = socket.getInetAddress().getHostAddress();
            }
        }

        public String getUsername() {
            return username;
        }

        private Optional<ClientHandler> findClient(String target) {
            synchronized (allClients) { // Ensure thread safety when iterating
                return allClients.stream()
                        .filter(client -> client.getUsername().equalsIgnoreCase(target))
                        .findFirst();
            }
        }

        /** Sends a message to this client */
        void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }

        /** Broadcasts message to all clients in current room except self */
        private void broadcast(String message) {
            Set<ClientHandler> clientsInRoom = rooms.get(currentRoom);
            if (clientsInRoom != null) {
                clientsInRoom.stream()
                        .filter(client -> !client.equals(this))
                        .forEach(client -> client.sendMessage(message));
            }
        }

        /** Broadcasts message to all clients in specified room */
        private void broadcast(String message, String room) {
            Set<ClientHandler> clientsInRoom = rooms.get(room);
            if (clientsInRoom != null) {
                clientsInRoom.forEach(client -> client.sendMessage(message));
            }
        }

        /** Updates username after validation */
        private void changeUsername(String newName) {
            if (newName == null || newName.trim().isEmpty()) {
                out.println("Username cannot be empty.");
                clientLogger.log(Level.WARNING, "{0} tried to change username to empty.", username);
                return;
            }
            String oldUsername = this.username;
            this.username = newName.trim();
            out.println("Username changed to " + this.username);
            clientLogger.log(Level.INFO, "{0} changed username to {1}.", new Object[]{oldUsername, this.username});
        }

        /**
         * Moves client to specified room, creating it if necessary.
         * @param roomName Target room name
         * @param password for protected rooms (null if none)
         */
        private void joinRoom(String roomName, String password) {
            roomName = roomName.trim();
            if (roomName.isEmpty()) {
                out.println("Room name cannot be empty.");
                clientLogger.log(Level.WARNING, "{0} tried to join an empty room name.", username);
                return;
            }

            Set<String> bannedIPs = roomBans.getOrDefault(roomName, Collections.emptySet());
            if (bannedIPs.contains(clientIP)) {
                out.println("You are banned from room: " + roomName);
                clientLogger.log(Level.WARNING, "{0} (IP: {1}) attempted to join banned room {2}.", new Object[]{username, clientIP, roomName});
                return;
            }

            synchronized (rooms) {
                boolean isNewRoom = false;
                String finalRoomName = roomName;
                Set<ClientHandler> targetRoomClients = rooms.computeIfAbsent(roomName, _ -> {
                    roomAdmins.put(finalRoomName, Collections.synchronizedSet(new HashSet<>()));
                    roomAdmins.get(finalRoomName).add(username);
                    roomCreators.put(finalRoomName, username);
                    clientLogger.log(Level.INFO, "{0} created new room: {1}.", new Object[]{username, finalRoomName});
                    return Collections.synchronizedSet(new HashSet<>());
                });

                // Handle password if creating new room
                if (roomCreators.get(roomName).equals(username) && !roomPasswords.containsKey(roomName)) {
                    isNewRoom = true;
                    if (password != null && !password.trim().isEmpty()) {
                        roomPasswords.put(roomName, password);
                        out.println("Room '" + roomName + "' created with a password.");
                        clientLogger.log(Level.INFO, "{0} created room {1} with password.", new Object[]{username, roomName});
                    } else {
                        out.println("Room '" + roomName + "' created.");
                        clientLogger.log(Level.INFO, "{0} created room {1} without password.", new Object[]{username, roomName});
                    }
                } else if (roomPasswords.containsKey(roomName)) {
                    String requiredPassword = roomPasswords.get(roomName);
                    if (requiredPassword != null && !requiredPassword.equals(password)) {
                        out.println("Incorrect password for room: " + roomName);
                        clientLogger.log(Level.WARNING, "{0} provided incorrect password for room {1}.", new Object[]{username, roomName});
                        return;
                    }
                }

                // Leave current room and join new
                Set<ClientHandler> currentRoomClients = rooms.get(currentRoom);
                if (currentRoomClients != null) {
                    currentRoomClients.remove(this);
                    if (!currentRoom.equals(roomName)) {
                        broadcast("[" + username + " left the room]", currentRoom);
                        clientLogger.log(Level.INFO, "{0} left room {1}.", new Object[]{username, currentRoom});
                    }
                }

                currentRoom = roomName;
                targetRoomClients.add(this);
                if (!isNewRoom) {
                    out.println("Joined room: " + roomName);
                }
                broadcast("[" + username + " joined the room]", currentRoom);
                clientLogger.log(Level.INFO, "{0} joined room {1}.", new Object[]{username, currentRoom});
            }
        }

        private void createPasswordForCurrentRoom(String password) {
            if (roomCreators.get(currentRoom).equals(username) || isRoomAdmin() || isServerAdmin()) {
                if (password == null || password.trim().isEmpty()) {
                    roomPasswords.remove(currentRoom);
                    out.println("Password removed for room: " + currentRoom);
                    clientLogger.log(Level.INFO, "{0} removed password for room {1}.", new Object[]{username, currentRoom});
                } else {
                    roomPasswords.put(currentRoom, password);
                    out.println("Password set for room: " + currentRoom);
                    clientLogger.log(Level.INFO, "{0} set password for room {1}.", new Object[]{username, currentRoom});
                }
            } else {
                out.println("Only the room creator can set a password.");
                clientLogger.log(Level.WARNING, "{0} attempted to set password for room {1} without permission.", new Object[]{username, currentRoom});
            }
        }

        /** Finds client by username in a given set */
        private Optional<ClientHandler> findClientInSet(Set<ClientHandler> clients, String targetUsername) {
            if (clients == null) {
                return Optional.empty();
            }
            return clients.stream()
                    .filter(client -> client.username.equalsIgnoreCase(targetUsername))
                    .findFirst();
        }

        /** Sends private message to user in current room */
        private void sendPrivateMessage(String target, String msg) {
            if (target.equalsIgnoreCase(username)) {
                out.println("You cannot send a private message to yourself.");
                return;
            }

            Optional<ClientHandler> targetClient = findClient(target);

            if (targetClient.isPresent()) {
                targetClient.get().sendMessage("[Private] " + userColor + username + ANSI_COLORS.get("reset") + ": " + msg);
                out.println("[Private] To " + target + ": " + msg);
            } else {
                out.println("User " + target + " not found on the server.");
            }
        }

        /** Lists users in current room or all rooms if in General */
        private void listUsersInRoom() {
            StringBuilder sb = new StringBuilder();

            if (currentRoom.equalsIgnoreCase("General")) {
                sb.append("All users by room:\n");
                rooms.forEach((roomName, clientSet) -> {
                    if (roomPasswords.containsKey(roomName)) {
                        sb.append("[").append(roomName).append("] (Password protected):\n");
                    } else {
                        sb.append("[").append(roomName).append("]:\n");
                    }
                    clientSet.forEach(client -> sb.append("- ").append(client.username).append("\n"));
                });
            } else {
                Set<ClientHandler> clientsInCurrentRoom = rooms.get(currentRoom);
                if (clientsInCurrentRoom != null) {
                    sb.append("Users in room '").append(currentRoom).append("':\n");
                    clientsInCurrentRoom.forEach(client -> sb.append("- ").append(client.username).append("\n"));
                } else {
                    sb.append("Error: Current room '").append(currentRoom).append("' not found.");
                }
            }
            out.println(sb);
        }

        private boolean isServerAdmin() {
            return username.equals(SERVER_ADMIN);
        }

        private boolean isRoomAdmin() {
            return roomAdmins.getOrDefault(currentRoom, Collections.emptySet()).contains(username);
        }

        /** Removes user from current room and returns them to General */
        private void kickUser(String targetUsername) {
            Set<ClientHandler> clientsInRoom = rooms.get(currentRoom);
            if (clientsInRoom == null) {
                out.println("Error: Current room not found.");
                return;
            }

            Optional<ClientHandler> targetClientOpt = findClientInSet(clientsInRoom, targetUsername);

            if (targetClientOpt.isPresent()) {
                ClientHandler targetClient = targetClientOpt.get();

                if (targetClient.equals(this)) {
                    out.println("You cannot kick yourself.");
                    return;
                }
                String creator = roomCreators.get(currentRoom);
                if (creator != null && creator.equals(targetUsername)) {
                    out.println("You cannot kick the creator of the room.");
                    return;
                }

                synchronized (clientsInRoom) {
                    clientsInRoom.remove(targetClient);
                }
                targetClient.currentRoom = "General";
                rooms.get("General").add(targetClient);

                targetClient.sendMessage("You were kicked from room '" + currentRoom + "'. Returned to General.");
                broadcast("User " + targetUsername + " was kicked from the room.");
            } else {
                out.println("User '" + targetUsername + "' not found in room '" + currentRoom + "'.");
            }
        }

        /** Finds client by username across all connected clients */
        private ClientHandler getClientByUsername(String name) {
            synchronized (allClients) {
                return allClients.stream()
                        .filter(client -> client.username.equals(name))
                        .findFirst()
                        .orElse(null);
            }
        }

        /** Mutes user(s) in current room for specified duration */
        private void handleMuteCommand(String[] parts) {
            if (!(isRoomAdmin() || isServerAdmin())) {
                out.println("You are not authorized to use /mute.");
                return;
            }

            if (parts.length < 3) {
                out.println("Usage: /mute <username|all> <seconds (max 600)>");
                return;
            }

            String target = parts[1];
            int seconds;

            try {
                seconds = Math.min(Integer.parseInt(parts[2]), 600);
            } catch (NumberFormatException e) {
                out.println("Invalid time. Must be a number in seconds (max 600).");
                return;
            }

            long muteUntil = System.currentTimeMillis() + seconds * 1000L;

            Set<ClientHandler> clientsToMute;
            if (target.equalsIgnoreCase("all")) {
                clientsToMute = rooms.get(currentRoom).stream()
                        .filter(client -> !client.username.equals(username))
                        .collect(Collectors.toSet());
                out.println("All users (except you) have been muted for " + seconds + " seconds.");
            } else {
                ClientHandler targetClient = getClientByUsername(target);
                if (targetClient != null && rooms.get(currentRoom).contains(targetClient)) {
                    clientsToMute = Set.of(targetClient);
                    out.println("Muted " + target + " for " + seconds + " seconds.");
                } else {
                    out.println("User not found in current room.");
                    return;
                }
            }

            for (ClientHandler client : clientsToMute) {
                mutedUsers.put(client.username, muteUntil);
                client.sendMessage("You have been muted for " + seconds + " seconds.");
            }
        }

        /** Removes mute from user(s) in current room */
        private void handleUnmuteCommand(String[] parts) {
            if (!(isRoomAdmin() || isServerAdmin())) {
                out.println("You are not authorized to use /unmute.");
                return;
            }

            if (parts.length < 2) {
                out.println("Usage: /unmute <username|all>");
                return;
            }

            String target = parts[1];

            if (target.equalsIgnoreCase("all")) {
                rooms.get(currentRoom).forEach(client -> mutedUsers.remove(client.username));
                out.println("All users in this room have been unmuted.");
            } else {
                if (mutedUsers.containsKey(target)) {
                    mutedUsers.remove(target);
                    out.println("Unmuted " + target);
                } else {
                    out.println("User " + target + " is not currently muted.");
                }
            }
        }

        /** Lists muted users in current room with remaining time */
        private void listMutedUsers() {
            StringBuilder sb = new StringBuilder("Muted users in this room:\n");
            long now = System.currentTimeMillis();
            boolean found = false;

            for (ClientHandler client : rooms.get(currentRoom)) {
                Long muteExpiry = mutedUsers.get(client.username);
                if (muteExpiry != null && muteExpiry > now) {
                    long remaining = (muteExpiry - now) / 1000;
                    sb.append("- ").append(client.username).append(" (").append(remaining).append("s remaining)\n");
                    found = true;
                }
            }

            if (!found) {
                out.println("No users are currently muted in this room.");
            } else {
                out.println(sb);
            }
        }

        /** Checks if current user is muted */
        private boolean isMuted() {
            Long expiry = mutedUsers.get(username);
            return expiry != null && expiry > System.currentTimeMillis();
        }

        /** Bans user from current room by IP */
        private void handleBanCommand(String[] parts) {
            if (!(isRoomAdmin() || isServerAdmin())) {
                out.println("You are not authorized to use /ban.");
                return;
            }
            if (parts.length < 2) {
                out.println("Usage: /ban <username>");
                return;
            }

            String targetUsername = parts[1];
            ClientHandler targetClient = getClientByUsername(targetUsername);

            if (targetClient != null && rooms.get(currentRoom).contains(targetClient)) {
                if (targetClient.equals(this)) {
                    out.println("You cannot ban yourself.");
                    return;
                }
                String creator = roomCreators.get(currentRoom);
                if (creator != null && creator.equals(targetUsername)) {
                    out.println("You cannot ban the creator of the room.");
                    return;
                }

                roomBans.computeIfAbsent(currentRoom, _ -> Collections.synchronizedSet(new HashSet<>())).add(targetClient.clientIP);

                Set<ClientHandler> currentClientsInRoom = rooms.get(currentRoom);
                if (currentClientsInRoom != null) {
                    synchronized (currentClientsInRoom) {
                        currentClientsInRoom.remove(targetClient);
                    }
                }

                targetClient.sendMessage("You have been banned from room: " + currentRoom);
                targetClient.currentRoom = "General";
                rooms.get("General").add(targetClient);
                targetClient.sendMessage("You were moved to the General room.");

                broadcast("[" + targetUsername + " was banned and removed from the room]", currentRoom);
            } else {
                out.println("User not found in your current room or invalid target.");
            }
        }

        /** Removes IP ban from current room */
        private void handleUnbanCommand(String[] parts) {
            if (!(isRoomAdmin() || isServerAdmin())) {
                out.println("You are not authorized to use /unban.");
                return;
            }
            if (parts.length < 2) {
                out.println("Usage: /unban <username>");
                return;
            }

            String targetUsername = parts[1];
            ClientHandler targetClient = getClientByUsername(targetUsername);

            if (targetClient != null) {
                Set<String> bannedIPs = roomBans.get(currentRoom);
                if (bannedIPs != null && bannedIPs.remove(targetClient.clientIP)) {
                    out.println("Unbanned " + targetUsername + " from room " + currentRoom + ".");
                } else {
                    out.println("User " + targetUsername + " is not currently banned from this room.");
                }
            } else {
                out.println("User " + targetUsername + " not found (may be disconnected).");
            }
        }

        /**
         * Main client handling loop. Processes incoming messages and commands.
         * Manages connection setup/teardown and error handling.
         */
        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                broadcast("[" + username + " joined the room]", "General");
                clientLogger.log(Level.INFO, "{0} joined room General. IP: {1}", new Object[]{username, clientIP});
                out.println("Welcome " + username + " to the " + currentRoom + " room.");

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("/")) {
                        handleCommand(input);
                    } else {
                        if (!isMuted()) {
                            broadcast(userColor + username + ANSI_COLORS.get("reset") + ": " + input);
                            clientLogger.log(Level.INFO, "[{0}] {1}: {2}", new Object[]{currentRoom, username, input});
                        } else {
                            out.println("You are muted and cannot send messages.");
                            clientLogger.log(Level.WARNING, "{0} tried to send message while muted in room {1}.", new Object[]{username, currentRoom});
                        }
                    }
                }
            } catch (SocketException e) {
                System.out.println("Client disconnected unexpectedly: " + username);
                clientLogger.log(Level.INFO, "Client {0} disconnected unexpectedly. IP: {1}", new Object[]{username, clientIP});
            } catch (IOException e) {
                System.err.println("I/O error for client " + username + ": " + e.getMessage());
                clientLogger.log(Level.SEVERE, "I/O error for client {0}: {1}", new Object[]{username, e.getMessage()});
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error closing socket for " + username + ": " + e.getMessage());
                    clientLogger.log(Level.WARNING, "Error closing socket for {0}: {1}", new Object[]{username, e.getMessage()});
                }

                // Cleanup client from room and global list
                Set<ClientHandler> currentRoomClients = rooms.get(currentRoom);
                if (currentRoomClients != null) {
                    synchronized (currentRoomClients) {
                        currentRoomClients.remove(this);
                    }
                    broadcast("[" + username + " left the room]", currentRoom);
                }
                allClients.remove(this);
                System.out.println("Client " + username + " disconnected.");
                clientLogger.log(Level.INFO, "Client {0} disconnected. Final room: {1}", new Object[]{username, currentRoom});
            }
        }

        /** Parses and executes client commands */
        private void handleCommand(String input) {
            String[] parts = input.split(" ", 3);
            String command = parts[0];

            switch (command) {
                case "/username":
                    if (parts.length > 1) changeUsername(parts[1]);
                    else out.println("Usage: /username <newName>");
                    break;
                case "/join":
                    if (parts.length >= 2) {
                        String roomName = parts[1];
                        String password = parts.length == 3 ? parts[2] : null;
                        joinRoom(roomName, password);
                    } else out.println("Usage: /join <roomName> [password]");
                    break;
                case "/password":
                    if (parts.length > 1) {
                        createPasswordForCurrentRoom(parts[1]);
                    } else out.println("Usage: /password <newPassword>");
                    break;
                case "/msg":
                    if (parts.length >= 3) sendPrivateMessage(parts[1], parts[2]);
                    else out.println("Invalid private message. Use: /msg <username> <message>");
                    break;
                case "/users":
                    listUsersInRoom();
                    break;
                case "/kick":
                    if (isRoomAdmin() || isServerAdmin()) {
                        if (parts.length > 1) {
                            kickUser(parts[1]);
                            clientLogger.log(Level.INFO, "{0} (admin) kicked {1} from room {2}.", new Object[]{username, parts[1], currentRoom});
                        } else out.println("Usage: /kick <username>");
                    } else {
                        out.println("You are not authorized to use /kick.");
                        clientLogger.log(Level.WARNING, "{0} attempted to use /kick without authorization in room {1}.", new Object[]{username, currentRoom});
                    }
                    break;
                case "/mute":
                    handleMuteCommand(input.split(" "));
                    break;
                case "/muted":
                    listMutedUsers();
                    break;
                case "/unmute":
                    handleUnmuteCommand(input.split(" "));
                    break;
                case "/grant":
                    if (isRoomAdmin() || isServerAdmin()) {
                        if (parts.length > 1) {
                            String target = parts[1];
                            Optional<ClientHandler> targetClientOpt = findClientInSet(rooms.get(currentRoom), target);

                            if (targetClientOpt.isPresent()) {
                                ClientHandler targetClient = targetClientOpt.get();
                                roomAdmins.computeIfAbsent(currentRoom, _ -> Collections.synchronizedSet(new HashSet<>())).add(target);
                                out.println("Granted admin to " + target);
                                targetClient.sendMessage("You have been granted admin rights in room: " + currentRoom);
                            } else {
                                out.println("User not found in your current room.");
                            }
                        } else out.println("Usage: /grant <username>");
                    } else out.println("You are not authorized to grant admin rights.");
                    break;
                case "/revoke":
                    if (isServerAdmin()) {
                        if (parts.length > 1) {
                            String target = parts[1];
                            Set<String> adminsInRoom = roomAdmins.get(currentRoom);
                            if (adminsInRoom != null && adminsInRoom.remove(target)) {
                                out.println("Revoked admin from " + target);
                                ClientHandler targetClient = getClientByUsername(target);
                                if (targetClient != null) {
                                    targetClient.sendMessage("Your admin rights in room: " + currentRoom + " have been revoked.");
                                }
                            } else {
                                out.println("User " + target + " is not a room admin or not found.");
                            }
                        } else out.println("Usage: /revoke <username>");
                    } else out.println("Only server admin can revoke admin rights.");
                    break;
                case "/shutdown":
                    if (isServerAdmin()) {
                        out.println("Server is shutting down...");
                        serverLogger.log(Level.SEVERE, "Server shutdown initiated by {0}.", username);
                        System.exit(0);
                    } else {
                        out.println("Only server admin can shutdown.");
                        clientLogger.log(Level.WARNING, "{0} attempted server shutdown without authorization.", username);
                    }
                    break;
                case "/color":
                    if (parts.length > 1) {
                        String color = parts[1];
                        if (ANSI_COLORS.containsKey(color)) {
                            userColor = ANSI_COLORS.get(color);
                            out.println("Color changed to " + color);
                        } else {
                            out.println("Unsupported color. Available: " + String.join(", ", ANSI_COLORS.keySet()));
                        }
                    } else out.println("Usage: /color <colorName>");
                    break;
                case "/exit":
                    if (!currentRoom.equals("General")) {
                        Set<ClientHandler> currentRoomClients = rooms.get(currentRoom);
                        if (currentRoomClients != null) {
                            synchronized (currentRoomClients) {
                                currentRoomClients.remove(this);
                            }
                            broadcast("[" + username + " left the room]", currentRoom);
                        }

                        currentRoom = "General";
                        rooms.get("General").add(this);
                        broadcast("[" + username + " returned to General]", "General");
                        out.println("You have returned to the General room.");
                    } else {
                        out.println("You're already in the General room.");
                    }
                    break;
                case "/room":
                    out.println("You are currently in room: " + currentRoom);
                    break;
                case "/ban":
                    handleBanCommand(input.split(" "));
                    break;
                case "/unban":
                    handleUnbanCommand(input.split(" "));
                    break;
                default:
                    out.println("Unknown command: " + command);
                    break;
            }
        }
    }
}