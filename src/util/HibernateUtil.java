package util;

import model.Message;
import model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


public class HibernateUtil {

    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration config = new Configuration();

            // Connexion PostgreSQL — adapter les valeurs à votre environnement
            config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            config.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost:5432/messagerie_g2");
            config.setProperty("hibernate.connection.username", "postgres");
            config.setProperty("hibernate.connection.password", " ");
            config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            config.setProperty("hibernate.hbm2ddl.auto", "update"); //
            config.setProperty("hibernate.show_sql", "true");
            config.setProperty("hibernate.format_sql", "true");

            // Entités
            config.addAnnotatedClass(User.class);
            config.addAnnotatedClass(Message.class);

            return config.buildSessionFactory();

        } catch (Exception e) {
            throw new RuntimeException("Erreur initialisation Hibernate : " + e.getMessage(), e);
        }
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}