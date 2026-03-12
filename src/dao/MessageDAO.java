package dao;

import model.Message;
import model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class MessageDAO {

    /** Enregistre un message en base. */
    public void save(Message message) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(message);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur sauvegarde Message : " + e.getMessage(), e);
        }
    }

    /** Met à jour le statut d'un message (ENVOYE → RECU → LU). */
    public void update(Message message) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(message);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur mise à jour Message : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne la conversation entre deux utilisateurs, triée chronologiquement (RG8).
     */
    public List<Message> findConversation(User user1, User user2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Message m WHERE " +
                                    "(m.sender = :u1 AND m.receiver = :u2) OR " +
                                    "(m.sender = :u2 AND m.receiver = :u1) " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("u1", user1)
                    .setParameter("u2", user2)
                    .list();
        }
    }


    public List<Message> findPendingMessages(User receiver) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Message m WHERE m.receiver = :receiver AND m.statut = :statut " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("receiver", receiver)
                    .setParameter("statut", Message.Statut.ENVOYE)
                    .list();
        }
    }

    /** Marque les messages en attente comme RECU après livraison. */
    public void markAsReceived(List<Message> messages) {
        messages.forEach(m -> {
            m.setStatut(Message.Statut.RECU);
            update(m);
        });
    }
}