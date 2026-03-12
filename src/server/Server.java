package server;

import common.NetworkPacket;
import model.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class Server {

    private static final int PORT = 5000;
    private static final Logger logger = Logger.getLogger(Server.class.getName());


    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    public void start() {
        logger.info("Démarrage du serveur sur le port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Nouvelle connexion depuis : " + clientSocket.getInetAddress());
                // Chaque client est géré dans un thread séparé (RG11)
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            logger.severe("Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * Enregistre un client connecté et notifie les autres (RG4).
     */
    public synchronized void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        logger.info("Client enregistré : " + username + " | Connectés : " + connectedClients.size());
        broadcast(new NetworkPacket(NetworkPacket.Type.USER_CONNECTED, username), username);
    }


    public synchronized void unregisterClient(String username) {
        connectedClients.remove(username);
        logger.info("Client déconnecté : " + username + " | Connectés : " + connectedClients.size());
        broadcast(new NetworkPacket(NetworkPacket.Type.USER_DISCONNECTED, username), username);
    }


    public boolean deliverMessage(String receiverUsername, NetworkPacket packet) {
        ClientHandler receiver = connectedClients.get(receiverUsername);
        if (receiver != null) {
            receiver.sendPacket(packet);
            return true;
        }
        return false; // Message sera stocké et livré à la reconnexion (RG6)
    }


    public void broadcast(NetworkPacket packet, String excludeUsername) {
        connectedClients.forEach((username, handler) -> {
            if (!username.equals(excludeUsername)) {
                handler.sendPacket(packet);
            }
        });
    }


    public boolean isConnected(String username) {
        return connectedClients.containsKey(username);
    }


    public java.util.List<String> getOnlineUsernames() {
        return new java.util.ArrayList<>(connectedClients.keySet());
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
