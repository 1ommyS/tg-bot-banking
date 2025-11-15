package ru.itpark.sb.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itpark.sb.model.TransactionEntity;
import ru.itpark.sb.model.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionDaoTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private Query<TransactionEntity> query;

    private TransactionDao transactionDao;
    private User testUser;

    @BeforeEach
    void setUp() {
        transactionDao = new TransactionDao();
        testUser = new User(123456789L, "testuser");
        testUser.setId(1L);
    }

    @Test
    void testSave_Success() {
        TransactionEntity transactionEntity = new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            BigDecimal.valueOf(100.00),
            "Test deposit"
        );

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.beginTransaction()).thenReturn(transaction);

            TransactionDao dao = new TransactionDao();
            TransactionEntity result = dao.save(transactionEntity);

            assertNotNull(result);
            verify(session).persist(transactionEntity);
            verify(transaction).commit();
            verify(session).close();
        }
    }

    @Test
    void testSave_RollbackOnException() {
        TransactionEntity transactionEntity = new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            BigDecimal.valueOf(100.00),
            "Test deposit"
        );

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.beginTransaction()).thenReturn(transaction);
            doThrow(new RuntimeException("Database error")).when(session).persist(any(TransactionEntity.class));

            TransactionDao dao = new TransactionDao();

            assertThrows(RuntimeException.class, () -> dao.save(transactionEntity));
            verify(transaction).rollback();
            verify(transaction, never()).commit();
            verify(session).close();
        }
    }

    @Test
    void testFindByUser_WithLimit() {
        int limit = 5;
        List<TransactionEntity> expectedTransactions = new ArrayList<>();
        expectedTransactions.add(new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            BigDecimal.valueOf(100.00),
            "Deposit 1"
        ));
        expectedTransactions.add(new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.WITHDRAWAL,
            BigDecimal.valueOf(50.00),
            "Withdrawal 1"
        ));

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.createQuery(anyString(), eq(TransactionEntity.class))).thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.setMaxResults(limit)).thenReturn(query);
            when(query.list()).thenReturn(expectedTransactions);

            TransactionDao dao = new TransactionDao();
            List<TransactionEntity> result = dao.findByUser(testUser, limit);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(query).setParameter(eq("user"), eq(testUser));
            verify(query).setMaxResults(limit);
            verify(session).close();
        }
    }

    @Test
    void testFindAllByUser() {
        List<TransactionEntity> expectedTransactions = new ArrayList<>();
        expectedTransactions.add(new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            BigDecimal.valueOf(100.00),
            "Deposit"
        ));

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.createQuery(anyString(), eq(TransactionEntity.class))).thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.list()).thenReturn(expectedTransactions);

            TransactionDao dao = new TransactionDao();
            List<TransactionEntity> result = dao.findAllByUser(testUser);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(query).setParameter(eq("user"), eq(testUser));
            verify(query, never()).setMaxResults(anyInt());
            verify(session).close();
        }
    }
}

