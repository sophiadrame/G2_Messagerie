package model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import model.User;
@Entity
@Table(name = "messages")
public class Message implements Serializable {

    public enum Statut { ENVOYE, RECU, LU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 1000)
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ENVOYE;

    public Message() {}

    public Message(User sender, User receiver, String contenu) {
        if (contenu == null || contenu.isBlank())
            throw new IllegalArgumentException("Le contenu ne peut pas être vide.");
        if (contenu.length() > 1000)
            throw new IllegalArgumentException("Le message ne peut pas dépasser 1000 caractères.");
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Message{from=" + sender.getUsername() + ", to=" + receiver.getUsername()
                + ", statut=" + statut + ", date=" + dateEnvoi + "}";
    }
}