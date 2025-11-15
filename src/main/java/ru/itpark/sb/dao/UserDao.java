package ru.itpark.sb.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import ru.itpark.sb.config.HibernateConfig;
import ru.itpark.sb.model.User;

import java.util.Optional;

public class UserDao {
    private final SessionFactory sessionFactory;

    public UserDao() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    public User save(User user) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при сохранении пользователя: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        Session session = sessionFactory.openSession();
        try {
            Query<User> query = session.createQuery(
                    "FROM User WHERE telegramId = :telegramId", User.class);
            query.setParameter("telegramId", telegramId);
            User user = query.uniqueResult();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске пользователя: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public User update(User user) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Ошибка при обновлении пользователя: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public User findById(Long id) {
        Session session = sessionFactory.openSession();
        try {
            return session.get(User.class, id);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске пользователя по ID: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }
}

