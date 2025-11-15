package ru.itpark.sb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itpark.sb.dao.TransactionDao;
import ru.itpark.sb.dao.UserDao;
import ru.itpark.sb.model.TransactionEntity;
import ru.itpark.sb.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private TransactionDao transactionDao;

    private BankingService bankingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        bankingService = new BankingService(userDao, transactionDao);
        testUser = new User();
        testUser.setId(1L);
        testUser.setTelegramId(123456789L);
        testUser.setUsername("testuser");
        testUser.setBalance(BigDecimal.valueOf(1000.00));
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testRegisterOrGetUser_NewUser() {
        Long telegramId = 123456789L;
        String username = "newuser";

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.empty());
        when(userDao.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = bankingService.registerOrGetUser(telegramId, username);

        assertNotNull(result);
        assertEquals(telegramId, result.getTelegramId());
        assertEquals(username, result.getUsername());
        verify(userDao).findByTelegramId(telegramId);
        verify(userDao).save(any(User.class));
        verify(userDao, never()).update(any(User.class));
    }

    @Test
    void testRegisterOrGetUser_ExistingUser() {
        Long telegramId = 123456789L;
        String username = "existinguser";

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));

        User result = bankingService.registerOrGetUser(telegramId, username);

        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userDao).findByTelegramId(telegramId);
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void testRegisterOrGetUser_UpdateUsername() {
        Long telegramId = 123456789L;
        String newUsername = "updateduser";

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));
        when(userDao.update(any(User.class))).thenReturn(testUser);

        User result = bankingService.registerOrGetUser(telegramId, newUsername);

        assertNotNull(result);
        assertEquals(newUsername, result.getUsername());
        verify(userDao).findByTelegramId(telegramId);
        verify(userDao).update(any(User.class));
    }

    @Test
    void testGetUserByTelegramId_UserExists() {
        Long telegramId = 123456789L;

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));

        User result = bankingService.getUserByTelegramId(telegramId);

        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userDao).findByTelegramId(telegramId);
    }

    @Test
    void testGetUserByTelegramId_UserNotFound() {
        Long telegramId = 999999999L;

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bankingService.getUserByTelegramId(telegramId));
        verify(userDao).findByTelegramId(telegramId);
    }

    @Test
    void testGetBalance() {
        Long telegramId = 123456789L;
        BigDecimal expectedBalance = BigDecimal.valueOf(1000.00);

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));

        BigDecimal result = bankingService.getBalance(telegramId);

        assertNotNull(result);
        assertEquals(expectedBalance, result);
        verify(userDao).findByTelegramId(telegramId);
    }

    @Test
    void testDeposit_Success() {
        Long telegramId = 123456789L;
        BigDecimal depositAmount = BigDecimal.valueOf(500.00);
        String description = "Test deposit";

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));
        when(userDao.update(any(User.class))).thenReturn(testUser);
        when(transactionDao.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
            TransactionEntity transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        TransactionEntity result = bankingService.deposit(telegramId, depositAmount, description);

        assertNotNull(result);
        assertEquals(TransactionEntity.TransactionType.DEPOSIT, result.getType());
        assertEquals(depositAmount, result.getAmount());
        assertEquals(description, result.getDescription());
        assertEquals(BigDecimal.valueOf(1500.00), testUser.getBalance());
        verify(userDao).findByTelegramId(telegramId);
        verify(userDao).update(any(User.class));
        verify(transactionDao).save(any(TransactionEntity.class));
    }

    @Test
    void testDeposit_NegativeAmount() {
        Long telegramId = 123456789L;
        BigDecimal negativeAmount = BigDecimal.valueOf(-100.00);

        assertThrows(IllegalArgumentException.class, 
            () -> bankingService.deposit(telegramId, negativeAmount, "Test"));
        
        verify(userDao, never()).update(any(User.class));
        verify(transactionDao, never()).save(any(TransactionEntity.class));
    }

    @Test
    void testDeposit_ZeroAmount() {
        Long telegramId = 123456789L;
        BigDecimal zeroAmount = BigDecimal.ZERO;

        assertThrows(IllegalArgumentException.class, 
            () -> bankingService.deposit(telegramId, zeroAmount, "Test"));
        
        verify(userDao, never()).update(any(User.class));
        verify(transactionDao, never()).save(any(TransactionEntity.class));
    }

    @Test
    void testWithdraw_Success() {
        Long telegramId = 123456789L;
        BigDecimal withdrawAmount = BigDecimal.valueOf(300.00);
        String description = "Test withdrawal";

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));
        when(userDao.update(any(User.class))).thenReturn(testUser);
        when(transactionDao.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
            TransactionEntity transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        TransactionEntity result = bankingService.withdraw(telegramId, withdrawAmount, description);

        assertNotNull(result);
        assertEquals(TransactionEntity.TransactionType.WITHDRAWAL, result.getType());
        assertEquals(withdrawAmount, result.getAmount());
        assertEquals(description, result.getDescription());
        assertEquals(BigDecimal.valueOf(700.00), testUser.getBalance());
        verify(userDao).findByTelegramId(telegramId);
        verify(userDao).update(any(User.class));
        verify(transactionDao).save(any(TransactionEntity.class));
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        Long telegramId = 123456789L;
        BigDecimal withdrawAmount = BigDecimal.valueOf(2000.00);

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, 
            () -> bankingService.withdraw(telegramId, withdrawAmount, "Test"));
        
        verify(userDao).findByTelegramId(telegramId);
        verify(userDao, never()).update(any(User.class));
        verify(transactionDao, never()).save(any(TransactionEntity.class));
    }

    @Test
    void testWithdraw_NegativeAmount() {
        Long telegramId = 123456789L;
        BigDecimal negativeAmount = BigDecimal.valueOf(-100.00);

        assertThrows(IllegalArgumentException.class, 
            () -> bankingService.withdraw(telegramId, negativeAmount, "Test"));
        
        verify(userDao, never()).findByTelegramId(telegramId);
        verify(userDao, never()).update(any(User.class));
        verify(transactionDao, never()).save(any(TransactionEntity.class));
    }

    @Test
    void testGetTransactionHistory() {
        Long telegramId = 123456789L;
        int limit = 10;

        TransactionEntity transaction1 = new TransactionEntity();
        transaction1.setId(1L);
        transaction1.setType(TransactionEntity.TransactionType.DEPOSIT);
        transaction1.setAmount(BigDecimal.valueOf(100.00));

        TransactionEntity transaction2 = new TransactionEntity();
        transaction2.setId(2L);
        transaction2.setType(TransactionEntity.TransactionType.WITHDRAWAL);
        transaction2.setAmount(BigDecimal.valueOf(50.00));

        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));
        when(transactionDao.findByUser(eq(testUser), eq(limit))).thenReturn(transactions);

        List<TransactionEntity> result = bankingService.getTransactionHistory(telegramId, limit);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userDao).findByTelegramId(telegramId);
        verify(transactionDao).findByUser(eq(testUser), eq(limit));
    }

    @Test
    void testGetAllTransactionHistory() {
        Long telegramId = 123456789L;

        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(new TransactionEntity());

        when(userDao.findByTelegramId(telegramId)).thenReturn(Optional.of(testUser));
        when(transactionDao.findAllByUser(eq(testUser))).thenReturn(transactions);

        List<TransactionEntity> result = bankingService.getAllTransactionHistory(telegramId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userDao).findByTelegramId(telegramId);
        verify(transactionDao).findAllByUser(eq(testUser));
    }
}


