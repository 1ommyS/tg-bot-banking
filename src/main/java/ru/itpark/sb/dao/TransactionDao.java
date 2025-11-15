package ru.itpark.sb.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import ru.itpark.sb.config.HibernateConfig;
import ru.itpark.sb.model.TransactionEntity;
import ru.itpark.sb.model.User;

import java.util.List;

public class TransactionDao {
    private final SessionFactory sessionFactory;

    public TransactionDao() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    public TransactionEntity save(TransactionEntity transaction) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(transaction);
            tx.commit();
            return transaction;
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new RuntimeException("Ошибка при сохранении транзакции: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public List<TransactionEntity> findByUser(User user, int limit) {
        Session session = sessionFactory.openSession();
        try {
            Query<TransactionEntity> query = session.createQuery(
                    "FROM TransactionEntity WHERE user = :user ORDER BY createdAt DESC", TransactionEntity.class);
            query.setParameter("user", user);
            query.setMaxResults(limit);
            return query.list();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске транзакций: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public List<TransactionEntity> findAllByUser(User user) {
        Session session = sessionFactory.openSession();
        try {
            Query<TransactionEntity> query = session.createQuery(
                    "FROM TransactionEntity WHERE user = :user ORDER BY createdAt DESC", TransactionEntity.class);
            query.setParameter("user", user);
            return query.list();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при поиске всех транзакций: " + e.getMessage(), e);
        } finally {
            session.close();
        }
    }
}

