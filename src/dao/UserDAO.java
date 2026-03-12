package dao;

import model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class UserDAO {

    /** Enregistre un nouvel utilisateur en base. */
    public void save(User user) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(user);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur sauvegarde User : " + e.getMessage(), e);
        }
    }

    /** Met à jour un utilisateur existant (statut, password, etc.). */
    public void update(User user) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(user);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur mise à jour User : " + e.getMessage(), e);
        }
    }

    /** Trouve un utilisateur par son username (RG1). */
    public Optional<User> findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional();
        }
    }

    /** Vérifie si un username est déjà pris (RG1). */
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    /** Retourne tous les utilisateurs connectés. */
    public List<User> findOnlineUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.status = :status", User.class)
                    .setParameter("status", User.Status.ONLINE)
                    .list();
        }
    }

    /** Retourne tous les membres inscrits (pour ORGANISATEUR — RG13). */
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class).list();
        }
    }

    /** Met à jour le statut d'un utilisateur (ONLINE/OFFLINE — RG4). */
    public void updateStatus(String username, User.Status status) {
        findByUsername(username).ifPresent(user -> {
            user.setStatus(status);
            update(user);
        });
    }
}