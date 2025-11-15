package ru.itpark.sb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void testUserCreation() {
        Long telegramId = 123456789L;
        String username = "testuser";
        User newUser = new User(telegramId, username);

        assertNotNull(newUser);
        assertEquals(telegramId, newUser.getTelegramId());
        assertEquals(username, newUser.getUsername());
        assertEquals(BigDecimal.ZERO, newUser.getBalance());
        assertNotNull(newUser.getCreatedAt());
    }

    @Test
    void testSetBalance() {
        BigDecimal balance = BigDecimal.valueOf(1000.50);
        user.setBalance(balance);

        assertEquals(balance, user.getBalance());
    }

    @Test
    void testSetTelegramId() {
        Long telegramId = 987654321L;
        user.setTelegramId(telegramId);

        assertEquals(telegramId, user.getTelegramId());
    }

    @Test
    void testSetUsername() {
        String username = "newusername";
        user.setUsername(username);

        assertEquals(username, user.getUsername());
    }

    @Test
    void testTransactionsList() {
        assertNotNull(user.getTransactions());
        assertTrue(user.getTransactions().isEmpty());
    }

    @Test
    void testDefaultBalance() {
        User newUser = new User();
        assertEquals(BigDecimal.ZERO, newUser.getBalance());
    }
}


