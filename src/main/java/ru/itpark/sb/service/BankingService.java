package ru.itpark.sb.service;

import ru.itpark.sb.dao.TransactionDao;
import ru.itpark.sb.dao.UserDao;
import ru.itpark.sb.model.TransactionEntity;
import ru.itpark.sb.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class BankingService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public BankingService() {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
    }

    public BankingService(UserDao userDao, TransactionDao transactionDao) {
        this.userDao = userDao;
        this.transactionDao = transactionDao;
    }

    public User registerOrGetUser(Long telegramId, String username) {
        Optional<User> existingUser = userDao.findByTelegramId(telegramId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (username != null && !username.equals(user.getUsername())) {
                user.setUsername(username);
                userDao.update(user);
            }
            return user;
        } else {
            User newUser = new User(telegramId, username);
            return userDao.save(newUser);
        }
    }

    public User getUserByTelegramId(Long telegramId) {
        return userDao.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public BigDecimal getBalance(Long telegramId) {
        User user = getUserByTelegramId(telegramId);
        return user.getBalance();
    }

    public TransactionEntity deposit(Long telegramId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть положительной");
        }

        User user = getUserByTelegramId(telegramId);
        user.setBalance(user.getBalance().add(amount));
        userDao.update(user);

        TransactionEntity transaction = new TransactionEntity(user, TransactionEntity.TransactionType.DEPOSIT, amount, description);
        return transactionDao.save(transaction);
    }

    public TransactionEntity withdraw(Long telegramId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма снятия должна быть положительной");
        }

        User user = getUserByTelegramId(telegramId);
        if (user.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Недостаточно средств на счете");
        }

        user.setBalance(user.getBalance().subtract(amount));
        userDao.update(user);

        TransactionEntity transaction = new TransactionEntity(user, TransactionEntity.TransactionType.WITHDRAWAL, amount, description);
        return transactionDao.save(transaction);
    }

    public List<TransactionEntity> getTransactionHistory(Long telegramId, int limit) {
        User user = getUserByTelegramId(telegramId);
        return transactionDao.findByUser(user, limit);
    }

    public List<TransactionEntity> getAllTransactionHistory(Long telegramId) {
        User user = getUserByTelegramId(telegramId);
        return transactionDao.findAllByUser(user);
    }
}

