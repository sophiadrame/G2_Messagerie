package client;

import common.NetworkPacket;
import model.Message;
import model.User;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;


public class ServerConnection {

    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private boolean running = false;

    // Callbacks JavaFX (appelés depuis le thread d'écoute)
    private Consumer<NetworkPacket> onPacketReceived;
    private Runnable onDisconnected;

    public void setOnPacketReceived(Consumer<NetworkPacket> handler) {
        this.onPacketReceived = handler;
    }

    public void setOnDisconnected(Runnable handler) {
        this.onDisconnected = handler;
    }


    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            running = true;
            startListening();
            logger.info("Connecté au serveur " + HOST + ":" + PORT);
            return true;
        } catch (IOException e) {
            logger.severe("Impossible de se connecter au serveur : " + e.getMessage());
            return false;
        }
    }


    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (running) {
                    NetworkPacket packet = (NetworkPacket) in.readObject();
                    if (onPacketReceived != null) {
                        onPacketReceived.accept(packet);
                    }
                }
            } catch (EOFException | java.net.SocketException e) {
                logger.info("Connexion au serveur perdue (RG10)");
            } catch (IOException | ClassNotFoundException e) {
                logger.warning("Erreur réception : " + e.getMessage());
            } finally {
                running = false;
                if (onDisconnected != null) onDisconnected.run();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    public synchronized boolean sendPacket(NetworkPacket packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
            return true;
        } catch (IOException e) {
            logger.severe("Erreur envoi paquet : " + e.getMessage());
            return false;
        }
    }



    public boolean login(String username, String password) {
        User credentials = new User(username, password, null);
        return sendPacket(new NetworkPacket(NetworkPacket.Type.LOGIN, credentials));
    }

    public boolean register(String username, String password, User.Role role) {
        User newUser = new User(username, password, role);
        return sendPacket(new NetworkPacket(NetworkPacket.Type.REGISTER, newUser));
    }

    public boolean sendMessage(String receiverUsername, String contenu) {
        // On crée un message "léger" avec juste le destinataire et le contenu
        User receiver = new User();
        receiver.setUsername(receiverUsername);
        Message msg = new Message();
        msg.setReceiver(receiver);
        msg.setContenu(contenu);
        return sendPacket(new NetworkPacket(NetworkPacket.Type.SEND_MESSAGE, msg));
    }

    public boolean requestOnlineUsers() {
        return sendPacket(new NetworkPacket(NetworkPacket.Type.GET_ONLINE_USERS, null));
    }

    public boolean requestHistory(String otherUsername) {
        return sendPacket(new NetworkPacket(NetworkPacket.Type.GET_HISTORY, otherUsername));
    }

    public boolean logout() {
        return sendPacket(new NetworkPacket(NetworkPacket.Type.LOGOUT, null));
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return running; }
}
