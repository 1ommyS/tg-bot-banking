package ru.itpark.sb.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itpark.sb.model.User;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDaoTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private Query<User> query;

    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDao();
    }

    @Test
    void testSave_Success() {
        User user = new User(123456789L, "testuser");
        user.setBalance(BigDecimal.valueOf(1000.00));

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.beginTransaction()).thenReturn(transaction);

            UserDao dao = new UserDao();
            User result = dao.save(user);

            assertNotNull(result);
            verify(session).persist(user);
            verify(transaction).commit();
            verify(session).close();
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    void testSave_RollbackOnException() {
        User user = new User(123456789L, "testuser");

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.beginTransaction()).thenReturn(transaction);
            doThrow(new RuntimeException("Database error")).when(session).persist(any(User.class));

            UserDao dao = new UserDao();

            assertThrows(RuntimeException.class, () -> dao.save(user));
            verify(transaction).rollback();
            verify(transaction, never()).commit();
            verify(session).close();
        }
    }

    @Test
    void testFindByTelegramId_UserExists() {
        Long telegramId = 123456789L;
        User expectedUser = new User(telegramId, "testuser");

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.uniqueResult()).thenReturn(expectedUser);

            UserDao dao = new UserDao();
            Optional<User> result = dao.findByTelegramId(telegramId);

            assertTrue(result.isPresent());
            assertEquals(expectedUser, result.get());
            verify(query).setParameter(eq("telegramId"), eq(telegramId));
            verify(session).close();
        }
    }

    @Test
    void testFindByTelegramId_UserNotFound() {
        Long telegramId = 999999999L;

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.uniqueResult()).thenReturn(null);

            UserDao dao = new UserDao();
            Optional<User> result = dao.findByTelegramId(telegramId);

            assertTrue(result.isEmpty());
            verify(session).close();
        }
    }

    @Test
    void testUpdate_Success() {
        User user = new User(123456789L, "testuser");
        user.setId(1L);
        user.setBalance(BigDecimal.valueOf(2000.00));

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.beginTransaction()).thenReturn(transaction);
            when(session.merge(user)).thenReturn(user);

            UserDao dao = new UserDao();
            User result = dao.update(user);

            assertNotNull(result);
            assertEquals(user, result);
            verify(session).merge(user);
            verify(transaction).commit();
            verify(session).close();
        }
    }

    @Test
    void testFindById_Success() {
        Long id = 1L;
        User expectedUser = new User(123456789L, "testuser");
        expectedUser.setId(id);

        try (var mockedStatic = mockStatic(ru.itpark.sb.config.HibernateConfig.class)) {
            mockedStatic.when(ru.itpark.sb.config.HibernateConfig::getSessionFactory).thenReturn(sessionFactory);
            when(sessionFactory.openSession()).thenReturn(session);
            when(session.get(User.class, id)).thenReturn(expectedUser);

            UserDao dao = new UserDao();
            User result = dao.findById(id);

            assertNotNull(result);
            assertEquals(expectedUser, result);
            verify(session).get(User.class, id);
            verify(session).close();
        }
    }
}

