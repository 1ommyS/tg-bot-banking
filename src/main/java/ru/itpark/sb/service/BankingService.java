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

    public TransactionEntity transfer(Long fromTelegramId, Long toTelegramId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть положительной");
        }

        if (fromTelegramId.equals(toTelegramId)) {
            throw new IllegalArgumentException("Нельзя переводить средства самому себе");
        }

        User fromUser = getUserByTelegramId(fromTelegramId);
        User toUser = getUserByTelegramId(toTelegramId);

        if (fromUser.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Недостаточно средств на счете для перевода");
        }

        fromUser.setBalance(fromUser.getBalance().subtract(amount));
        toUser.setBalance(toUser.getBalance().add(amount));

        userDao.update(fromUser);
        userDao.update(toUser);

        TransactionEntity outboundTransaction = new TransactionEntity(fromUser, TransactionEntity.TransactionType.TRANSFER_OUT, amount, description);
        outboundTransaction.setRecipientId(toTelegramId);
        transactionDao.save(outboundTransaction);

        TransactionEntity inboundTransaction = new TransactionEntity(toUser, TransactionEntity.TransactionType.TRANSFER_IN, amount, description != null ? description + " (от пользователя)" : "Перевод от пользователя");
        inboundTransaction.setRecipientId(fromTelegramId);
        transactionDao.save(inboundTransaction);

        return outboundTransaction;
    }

    public TransactionStatistics getStatistics(Long telegramId) {
        User user = getUserByTelegramId(telegramId);
        List<TransactionEntity> allTransactions = transactionDao.findAllByUser(user);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfersOut = BigDecimal.ZERO;
        BigDecimal totalTransfersIn = BigDecimal.ZERO;
        int depositCount = 0;
        int withdrawalCount = 0;
        int transferOutCount = 0;
        int transferInCount = 0;

        for (TransactionEntity transaction : allTransactions) {
            switch (transaction.getType()) {
                case DEPOSIT:
                    totalDeposits = totalDeposits.add(transaction.getAmount());
                    depositCount++;
                    break;
                case WITHDRAWAL:
                    totalWithdrawals = totalWithdrawals.add(transaction.getAmount());
                    withdrawalCount++;
                    break;
                case TRANSFER_OUT:
                    totalTransfersOut = totalTransfersOut.add(transaction.getAmount());
                    transferOutCount++;
                    break;
                case TRANSFER_IN:
                    totalTransfersIn = totalTransfersIn.add(transaction.getAmount());
                    transferInCount++;
                    break;
            }
        }

        BigDecimal avgDeposit = depositCount > 0 ? totalDeposits.divide(BigDecimal.valueOf(depositCount), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgWithdrawal = withdrawalCount > 0 ? totalWithdrawals.divide(BigDecimal.valueOf(withdrawalCount), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new TransactionStatistics(
            totalDeposits,
            totalWithdrawals,
            totalTransfersOut,
            totalTransfersIn,
            depositCount,
            withdrawalCount,
            transferOutCount,
            transferInCount,
            allTransactions.size(),
            avgDeposit,
            avgWithdrawal
        );
    }

    public static class TransactionStatistics {
        private final BigDecimal totalDeposits;
        private final BigDecimal totalWithdrawals;
        private final BigDecimal totalTransfersOut;
        private final BigDecimal totalTransfersIn;
        private final int depositCount;
        private final int withdrawalCount;
        private final int transferOutCount;
        private final int transferInCount;
        private final int totalTransactions;
        private final BigDecimal avgDeposit;
        private final BigDecimal avgWithdrawal;

        public TransactionStatistics(BigDecimal totalDeposits, BigDecimal totalWithdrawals,
                                   BigDecimal totalTransfersOut, BigDecimal totalTransfersIn,
                                   int depositCount, int withdrawalCount,
                                   int transferOutCount, int transferInCount,
                                   int totalTransactions,
                                   BigDecimal avgDeposit, BigDecimal avgWithdrawal) {
            this.totalDeposits = totalDeposits;
            this.totalWithdrawals = totalWithdrawals;
            this.totalTransfersOut = totalTransfersOut;
            this.totalTransfersIn = totalTransfersIn;
            this.depositCount = depositCount;
            this.withdrawalCount = withdrawalCount;
            this.transferOutCount = transferOutCount;
            this.transferInCount = transferInCount;
            this.totalTransactions = totalTransactions;
            this.avgDeposit = avgDeposit;
            this.avgWithdrawal = avgWithdrawal;
        }

        public BigDecimal getTotalDeposits() { return totalDeposits; }
        public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
        public BigDecimal getTotalTransfersOut() { return totalTransfersOut; }
        public BigDecimal getTotalTransfersIn() { return totalTransfersIn; }
        public int getDepositCount() { return depositCount; }
        public int getWithdrawalCount() { return withdrawalCount; }
        public int getTransferOutCount() { return transferOutCount; }
        public int getTransferInCount() { return transferInCount; }
        public int getTotalTransactions() { return totalTransactions; }
        public BigDecimal getAvgDeposit() { return avgDeposit; }
        public BigDecimal getAvgWithdrawal() { return avgWithdrawal; }
    }
}

