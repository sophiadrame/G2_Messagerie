package common;

import java.io.Serializable;

/**
 * Objet échangé entre le client et le serveur via les sockets.
 * Le type définit l'action à effectuer.
 */
public class NetworkPacket implements Serializable {

    public enum Type {
        // Auth
        LOGIN, LOGOUT, REGISTER,
        // Réponses serveur
        SUCCESS, ERROR,
        // Messagerie
        SEND_MESSAGE, RECEIVE_MESSAGE,
        // Listes
        GET_ONLINE_USERS, USER_LIST,
        GET_HISTORY, MESSAGE_HISTORY,
        // Statut
        USER_CONNECTED, USER_DISCONNECTED
    }

    private Type type;
    private Object payload;   // Données associées (User, Message, List, String...)
    private String info;      // Message textuel optionnel (erreur, info)

    public NetworkPacket() {}

    public NetworkPacket(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public NetworkPacket(Type type, String info) {
        this.type = type;
        this.info = info;
    }

    public NetworkPacket(Type type, Object payload, String info) {
        this.type = type;
        this.payload = payload;
        this.info = info;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    @Override
    public String toString() {
        return "NetworkPacket{type=" + type + ", info='" + info + "'}";
    }
}