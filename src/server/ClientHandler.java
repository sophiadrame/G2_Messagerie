package server;

import common.NetworkPacket;
import dao.MessageDAO;
import dao.UserDAO;
import model.Message;
import model.User;
import util.PasswordUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User currentUser; // null tant que non authentifié

    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            NetworkPacket packet;
            while ((packet = (NetworkPacket) in.readObject()) != null) {
                handlePacket(packet);
            }

        } catch (EOFException | java.net.SocketException e) {
            logger.info("Client déconnecté brusquement : "
                    + (currentUser != null ? currentUser.getUsername() : "inconnu"));
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Erreur communication client : " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /** Dispatche le paquet reçu vers le bon traitement. */
    private void handlePacket(NetworkPacket packet) {
        logger.info("Paquet reçu : " + packet.getType()
                + (currentUser != null ? " | de : " + currentUser.getUsername() : ""));

        switch (packet.getType()) {
            case REGISTER        -> handleRegister(packet);
            case LOGIN           -> handleLogin(packet);
            case LOGOUT          -> disconnect();
            case SEND_MESSAGE    -> handleSendMessage(packet);
            case GET_ONLINE_USERS -> handleGetOnlineUsers();
            case GET_HISTORY     -> handleGetHistory(packet);
            default              -> sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Paquet inconnu"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INSCRIPTION (RG1, RG9)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleRegister(NetworkPacket packet) {
        User newUser = (User) packet.getPayload();

        if (userDAO.existsByUsername(newUser.getUsername())) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Username déjà utilisé (RG1)"));
            return;
        }

        // Hachage du mot de passe (RG9)
        newUser.setPassword(PasswordUtil.hash(newUser.getPassword()));
        newUser.setStatus(User.Status.OFFLINE);
        userDAO.save(newUser);
        logger.info("Inscription réussie : " + newUser.getUsername());
        sendPacket(new NetworkPacket(NetworkPacket.Type.SUCCESS, "Inscription réussie"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONNEXION (RG2, RG3, RG4, RG6)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleLogin(NetworkPacket packet) {
        User credentials = (User) packet.getPayload();

        Optional<User> opt = userDAO.findByUsername(credentials.getUsername());
        if (opt.isEmpty()) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Utilisateur introuvable"));
            return;
        }

        User user = opt.get();

        if (!PasswordUtil.verify(credentials.getPassword(), user.getPassword())) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Mot de passe incorrect"));
            return;
        }

        // RG3 : une seule session par utilisateur
        if (server.isConnected(user.getUsername())) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Déjà connecté sur un autre appareil (RG3)"));
            return;
        }

        // RG4 : statut ONLINE
        user.setStatus(User.Status.ONLINE);
        userDAO.update(user);
        currentUser = user;
        server.registerClient(user.getUsername(), this);
        logger.info("Connexion : " + user.getUsername());

        // On envoie une copie sans mot de passe au client
        User safeUser = new User(user.getUsername(), "", user.getRole());
        safeUser.setId(user.getId());
        safeUser.setStatus(user.getStatus());
        sendPacket(new NetworkPacket(NetworkPacket.Type.SUCCESS, safeUser, "Connexion réussie"));

        // RG6 : livraison des messages en attente
        deliverPendingMessages(user);
    }


    private void deliverPendingMessages(User user) {
        List<Message> pending = messageDAO.findPendingMessages(user);
        if (!pending.isEmpty()) {
            logger.info(pending.size() + " message(s) en attente pour " + user.getUsername());
            for (Message msg : pending) {
                sendPacket(new NetworkPacket(NetworkPacket.Type.RECEIVE_MESSAGE, msg));
            }
            messageDAO.markAsReceived(pending);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOI DE MESSAGE (RG2, RG5, RG6, RG7, RG12)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleSendMessage(NetworkPacket packet) {
        // RG2 : doit être authentifié
        if (currentUser == null) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Non authentifié (RG2)"));
            return;
        }

        Message msg = (Message) packet.getPayload();

        // RG7 : contenu non vide et <= 1000 caractères (déjà géré dans le constructeur Message)
        if (msg.getContenu() == null || msg.getContenu().isBlank()) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Message vide (RG7)"));
            return;
        }
        if (msg.getContenu().length() > 1000) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Message trop long (RG7)"));
            return;
        }

        // RG5 : destinataire doit exister
        Optional<User> receiverOpt = userDAO.findByUsername(msg.getReceiver().getUsername());
        if (receiverOpt.isEmpty()) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Destinataire introuvable (RG5)"));
            return;
        }

        User receiver = receiverOpt.get();
        Message newMsg = new Message(currentUser, receiver, msg.getContenu());
        messageDAO.save(newMsg);

        // RG12 : journalisation
        logger.info("Message envoyé de " + currentUser.getUsername() + " → " + receiver.getUsername());

        // Tentative de livraison en temps réel
        boolean delivered = server.deliverMessage(receiver.getUsername(),
                new NetworkPacket(NetworkPacket.Type.RECEIVE_MESSAGE, newMsg));

        if (delivered) {
            newMsg.setStatut(Message.Statut.RECU);
            messageDAO.update(newMsg);
        }
        // Sinon RG6 : le message reste en statut ENVOYE et sera livré à la reconnexion

        sendPacket(new NetworkPacket(NetworkPacket.Type.SUCCESS, "Message envoyé"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTE DES MEMBRES EN LIGNE
    // ─────────────────────────────────────────────────────────────────────────
    private void handleGetOnlineUsers() {
        if (currentUser == null) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Non authentifié (RG2)"));
            return;
        }
        List<String> onlineUsers = server.getOnlineUsernames();
        sendPacket(new NetworkPacket(NetworkPacket.Type.USER_LIST, onlineUsers));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTORIQUE DES MESSAGES (RG8)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleGetHistory(NetworkPacket packet) {
        if (currentUser == null) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Non authentifié (RG2)"));
            return;
        }

        String otherUsername = (String) packet.getPayload();
        Optional<User> otherOpt = userDAO.findByUsername(otherUsername);
        if (otherOpt.isEmpty()) {
            sendPacket(new NetworkPacket(NetworkPacket.Type.ERROR, "Utilisateur introuvable"));
            return;
        }

        // RG8 : trié chronologiquement (fait dans la requête DAO)
        List<Message> history = messageDAO.findConversation(currentUser, otherOpt.get());
        sendPacket(new NetworkPacket(NetworkPacket.Type.MESSAGE_HISTORY, history));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉCONNEXION (RG4, RG10, RG12)
    // ─────────────────────────────────────────────────────────────────────────
    private void disconnect() {
        if (currentUser != null) {
            userDAO.updateStatus(currentUser.getUsername(), User.Status.OFFLINE); // RG4
            server.unregisterClient(currentUser.getUsername());
            logger.info("Déconnexion : " + currentUser.getUsername()); // RG12
            currentUser = null;
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    /** Envoie un paquet au client de façon thread-safe. */
    public synchronized void sendPacket(NetworkPacket packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset(); // évite le cache d'objets Hibernate
        } catch (IOException e) {
            logger.warning("Impossible d'envoyer le paquet : " + e.getMessage());
        }
    }
}
