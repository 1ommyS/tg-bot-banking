package ru.itpark.sb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEntityTest {

    private User testUser;
    private TransactionEntity transaction;

    @BeforeEach
    void setUp() {
        testUser = new User(123456789L, "testuser");
        testUser.setId(1L);
    }

    @Test
    void testTransactionCreation() {
        BigDecimal amount = BigDecimal.valueOf(100.50);
        String description = "Test transaction";

        TransactionEntity transaction = new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            amount,
            description
        );

        assertNotNull(transaction);
        assertEquals(testUser, transaction.getUser());
        assertEquals(TransactionEntity.TransactionType.DEPOSIT, transaction.getType());
        assertEquals(amount, transaction.getAmount());
        assertEquals(description, transaction.getDescription());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void testTransactionType_DEPOSIT() {
        TransactionEntity transaction = new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            BigDecimal.valueOf(100),
            "Deposit"
        );

        assertEquals(TransactionEntity.TransactionType.DEPOSIT, transaction.getType());
    }

    @Test
    void testTransactionType_WITHDRAWAL() {
        TransactionEntity transaction = new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.WITHDRAWAL,
            BigDecimal.valueOf(50),
            "Withdrawal"
        );

        assertEquals(TransactionEntity.TransactionType.WITHDRAWAL, transaction.getType());
    }

    @Test
    void testSetAmount() {
        transaction = new TransactionEntity();
        BigDecimal amount = BigDecimal.valueOf(250.75);
        transaction.setAmount(amount);

        assertEquals(amount, transaction.getAmount());
    }

    @Test
    void testSetDescription() {
        transaction = new TransactionEntity();
        String description = "New description";
        transaction.setDescription(description);

        assertEquals(description, transaction.getDescription());
    }

    @Test
    void testCreatedAtIsSet() {
        transaction = new TransactionEntity(
            testUser,
            TransactionEntity.TransactionType.DEPOSIT,
            BigDecimal.valueOf(100),
            "Test"
        );

        assertNotNull(transaction.getCreatedAt());
        assertTrue(transaction.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testTransactionEnumValues() {
        TransactionEntity.TransactionType[] values = TransactionEntity.TransactionType.values();
        assertEquals(2, values.length);
        assertEquals(TransactionEntity.TransactionType.DEPOSIT, values[0]);
        assertEquals(TransactionEntity.TransactionType.WITHDRAWAL, values[1]);
    }
}

