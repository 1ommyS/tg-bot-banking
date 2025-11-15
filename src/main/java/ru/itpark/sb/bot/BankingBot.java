package ru.itpark.sb.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.itpark.sb.keyboard.KeyboardFactory;
import ru.itpark.sb.model.TransactionEntity;
import ru.itpark.sb.service.BankingService;
import ru.itpark.sb.service.BankingService.TransactionStatistics;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BankingBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(BankingBot.class);
    
    private final BankingService bankingService;
    private final String botToken;
    private final String botUsername;
    
    private final Map<Long, BotState> userStates = new HashMap<>();
    
    private enum BotState {
        IDLE,
        WAITING_DEPOSIT_AMOUNT,
        WAITING_WITHDRAWAL_AMOUNT,
        WAITING_TRANSFER_RECIPIENT,
        WAITING_TRANSFER_AMOUNT
    }
    
    private final Map<Long, Long> pendingTransfers = new HashMap<>();

    public BankingBot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.bankingService = new BankingService();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String text = message.getText();
            String username = message.getFrom().getUserName();
            Long telegramId = message.getFrom().getId();

            try {
                bankingService.registerOrGetUser(telegramId, username);

                BotState currentState = userStates.getOrDefault(chatId, BotState.IDLE);

                if (text.equals("/start")) {
                    handleStart(chatId, message.getFrom().getFirstName());
                    userStates.put(chatId, BotState.IDLE);
                    return;
                }

                if (text.equals("❌ Отмена")) {
                    handleCancel(chatId);
                    userStates.put(chatId, BotState.IDLE);
                    pendingTransfers.remove(chatId);
                    return;
                }

                if (isMainMenuButton(text)) {
                    userStates.put(chatId, BotState.IDLE);
                    handleMainMenu(chatId, telegramId, text);
                    return;
                }

                switch (currentState) {
                    case WAITING_DEPOSIT_AMOUNT:
                        handleDepositAmount(chatId, telegramId, text);
                        break;
                    case WAITING_WITHDRAWAL_AMOUNT:
                        handleWithdrawalAmount(chatId, telegramId, text);
                        break;
                    case WAITING_TRANSFER_RECIPIENT:
                        handleTransferRecipient(chatId, telegramId, text);
                        break;
                    case WAITING_TRANSFER_AMOUNT:
                        handleTransferAmount(chatId, telegramId, text);
                        break;
                    case IDLE:
                    default:
                        handleMainMenu(chatId, telegramId, text);
                        break;
                }
            } catch (Exception e) {
                logger.error("Ошибка при обработке обновления для chatId: {}", chatId, e);
                sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
                userStates.put(chatId, BotState.IDLE);
            }
        }
    }

    private void handleStart(Long chatId, String firstName) {
        String message = "👋 Привет, " + firstName + "!\n\n" +
                "Добро пожаловать в банковское приложение!\n\n" +
                "Выберите действие из меню:";
        sendMessageWithKeyboard(chatId, message, KeyboardFactory.createMainMenu());
    }

    private void handleCancel(Long chatId) {
        sendMessageWithKeyboard(chatId, "Операция отменена.", KeyboardFactory.createMainMenu());
    }

    private void handleMainMenu(Long chatId, Long telegramId, String text) {
        switch (text) {
            case "💰 Баланс":
                handleBalance(chatId, telegramId);
                break;
            case "💳 Пополнить":
                handleDeposit(chatId);
                break;
            case "💸 Снять":
                handleWithdrawal(chatId);
                break;
            case "📤 Перевод":
                handleTransfer(chatId);
                break;
            case "📜 История":
                handleHistory(chatId, telegramId);
                break;
            case "📊 Статистика":
                handleStatistics(chatId, telegramId);
                break;
            default:
                sendMessage(chatId, "Пожалуйста, используйте кнопки меню.");
                break;
        }
    }

    private void handleBalance(Long chatId, Long telegramId) {
        try {
            BigDecimal balance = bankingService.getBalance(telegramId);
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            
            String message = "💰 Ваш баланс: " + formatter.format(balance) + " ₽";
            sendMessageWithKeyboard(chatId, message, KeyboardFactory.createMainMenu());
            logger.debug("Баланс запрошен для пользователя telegramId: {}, chatId: {}", telegramId, chatId);
        } catch (Exception e) {
            logger.error("Ошибка при получении баланса для telegramId: {}, chatId: {}", telegramId, chatId, e);
            sendMessage(chatId, "❌ Ошибка при получении баланса: " + e.getMessage());
        }
    }

    private void handleDeposit(Long chatId) {
        userStates.put(chatId, BotState.WAITING_DEPOSIT_AMOUNT);
        String message = "💳 Введите сумму для пополнения:";
        sendMessageWithKeyboard(chatId, message, KeyboardFactory.createCancelMenu());
    }

    private void handleDepositAmount(Long chatId, Long telegramId, String amountText) {
        try {
            BigDecimal amount = parseAmount(amountText);
            bankingService.deposit(telegramId, amount, "Пополнение счета");
            
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            
            String message = "✅ Счет пополнен на " + formatter.format(amount) + " ₽\n\n" +
                    "💰 Новый баланс: " + formatter.format(bankingService.getBalance(telegramId)) + " ₽";
            sendMessageWithKeyboard(chatId, message, KeyboardFactory.createMainMenu());
            userStates.put(chatId, BotState.IDLE);
            logger.info("Пополнение счета для пользователя telegramId: {}, сумма: {}", telegramId, amount);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при пополнении счета для telegramId: {}, сумма: {}, ошибка: {}", 
                    telegramId, amountText, e.getMessage());
            sendMessageWithKeyboard(chatId, "❌ " + e.getMessage() + "\n\nПопробуйте снова или отмените операцию.", 
                    KeyboardFactory.createCancelMenu());
        }
    }

    private void handleWithdrawal(Long chatId) {
        userStates.put(chatId, BotState.WAITING_WITHDRAWAL_AMOUNT);
        String message = "💸 Введите сумму для снятия:";
        sendMessageWithKeyboard(chatId, message, KeyboardFactory.createCancelMenu());
    }

    private void handleWithdrawalAmount(Long chatId, Long telegramId, String amountText) {
        try {
            BigDecimal amount = parseAmount(amountText);
            bankingService.withdraw(telegramId, amount, "Снятие средств");
            
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            
            String message = "✅ Со счета снято " + formatter.format(amount) + " ₽\n\n" +
                    "💰 Новый баланс: " + formatter.format(bankingService.getBalance(telegramId)) + " ₽";
            sendMessageWithKeyboard(chatId, message, KeyboardFactory.createMainMenu());
            userStates.put(chatId, BotState.IDLE);
            logger.info("Снятие средств для пользователя telegramId: {}, сумма: {}", telegramId, amount);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при снятии средств для telegramId: {}, сумма: {}, ошибка: {}", 
                    telegramId, amountText, e.getMessage());
            sendMessageWithKeyboard(chatId, "❌ " + e.getMessage() + "\n\nПопробуйте снова или отмените операцию.", 
                    KeyboardFactory.createCancelMenu());
        }
    }

    private void handleHistory(Long chatId, Long telegramId) {
        try {
            List<TransactionEntity> transactions = bankingService.getTransactionHistory(telegramId, 10);
            
            if (transactions.isEmpty()) {
                sendMessageWithKeyboard(chatId, "📜 История транзакций пуста.", KeyboardFactory.createMainMenu());
                return;
            }

            StringBuilder message = new StringBuilder("📜 История транзакций (последние 10):\n\n");
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            
            for (TransactionEntity transaction : transactions) {
                String typeEmoji;
                String typeText;
                switch (transaction.getType()) {
                    case DEPOSIT:
                        typeEmoji = "➕";
                        typeText = "Пополнение";
                        break;
                    case WITHDRAWAL:
                        typeEmoji = "➖";
                        typeText = "Снятие";
                        break;
                    case TRANSFER_OUT:
                        typeEmoji = "📤";
                        typeText = "Перевод";
                        break;
                    case TRANSFER_IN:
                        typeEmoji = "📥";
                        typeText = "Получен перевод";
                        break;
                    default:
                        typeEmoji = "💰";
                        typeText = "Операция";
                }
                
                message.append(typeEmoji).append(" ").append(typeText)
                        .append(": ").append(formatter.format(transaction.getAmount())).append(" ₽\n")
                        .append("📅 ").append(transaction.getCreatedAt().format(dateFormatter)).append("\n");
                
                if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                    message.append("📝 ").append(transaction.getDescription()).append("\n");
                }
                if (transaction.getRecipientId() != null) {
                    message.append("👤 Получатель: ").append(transaction.getRecipientId()).append("\n");
                }
                message.append("\n");
            }
            
            sendMessageWithKeyboard(chatId, message.toString(), KeyboardFactory.createMainMenu());
            logger.debug("История транзакций запрошена для пользователя telegramId: {}, chatId: {}", telegramId, chatId);
        } catch (Exception e) {
            logger.error("Ошибка при получении истории транзакций для telegramId: {}, chatId: {}", telegramId, chatId, e);
            sendMessage(chatId, "❌ Ошибка при получении истории: " + e.getMessage());
        }
    }

    private void handleTransfer(Long chatId) {
        userStates.put(chatId, BotState.WAITING_TRANSFER_RECIPIENT);
        String message = "📤 Перевод средств\n\n" +
                "Введите Telegram ID получателя (число):\n\n" +
                "💡 Подсказка: Telegram ID можно узнать у получателя";
        sendMessageWithKeyboard(chatId, message, KeyboardFactory.createCancelMenu());
    }

    private void handleTransferRecipient(Long chatId, Long telegramId, String recipientText) {
        try {
            Long recipientTelegramId = Long.parseLong(recipientText.trim());
            
            if (recipientTelegramId.equals(telegramId)) {
                sendMessageWithKeyboard(chatId, "❌ Нельзя переводить средства самому себе!\n\nПопробуйте снова или отмените операцию.", 
                        KeyboardFactory.createCancelMenu());
                return;
            }

            try {
                bankingService.getUserByTelegramId(recipientTelegramId);
            } catch (RuntimeException e) {
                sendMessageWithKeyboard(chatId, "❌ Получатель с таким Telegram ID не найден в системе!\n\nПопробуйте снова или отмените операцию.", 
                        KeyboardFactory.createCancelMenu());
                return;
            }

            pendingTransfers.put(chatId, recipientTelegramId);
            userStates.put(chatId, BotState.WAITING_TRANSFER_AMOUNT);
            String message = "💵 Введите сумму для перевода:";
            sendMessageWithKeyboard(chatId, message, KeyboardFactory.createCancelMenu());
        } catch (NumberFormatException e) {
            sendMessageWithKeyboard(chatId, "❌ Неверный формат Telegram ID. Введите число.\n\nПопробуйте снова или отмените операцию.", 
                    KeyboardFactory.createCancelMenu());
        }
    }

    private void handleTransferAmount(Long chatId, Long telegramId, String amountText) {
        Long recipientId = pendingTransfers.get(chatId);
        if (recipientId == null) {
            sendMessageWithKeyboard(chatId, "❌ Ошибка: получатель не указан. Начните перевод заново.", 
                    KeyboardFactory.createMainMenu());
            userStates.put(chatId, BotState.IDLE);
            pendingTransfers.remove(chatId);
            return;
        }

        try {
            BigDecimal amount = parseAmount(amountText);
            TransactionEntity transaction = bankingService.transfer(telegramId, recipientId, amount, "Перевод между пользователями");
            
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            
            String recipientUsername = "";
            try {
                recipientUsername = " (" + bankingService.getUserByTelegramId(recipientId).getUsername() + ")";
            } catch (Exception e) {
            }
            
            String message = "✅ Перевод выполнен!\n\n" +
                    "📤 Отправлено: " + formatter.format(amount) + " ₽\n" +
                    "👤 Получатель: " + recipientId + recipientUsername + "\n\n" +
                    "💰 Ваш баланс: " + formatter.format(bankingService.getBalance(telegramId)) + " ₽";
            sendMessageWithKeyboard(chatId, message, KeyboardFactory.createMainMenu());
            userStates.put(chatId, BotState.IDLE);
            pendingTransfers.remove(chatId);
            logger.info("Перевод выполнен: от {} к {}, сумма: {}", telegramId, recipientId, amount);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при переводе от telegramId: {} к {}, сумма: {}, ошибка: {}", 
                    telegramId, recipientId, amountText, e.getMessage());
            sendMessageWithKeyboard(chatId, "❌ " + e.getMessage() + "\n\nПопробуйте снова или отмените операцию.", 
                    KeyboardFactory.createCancelMenu());
        }
    }

    private void handleStatistics(Long chatId, Long telegramId) {
        try {
            TransactionStatistics stats = bankingService.getStatistics(telegramId);
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            
            StringBuilder message = new StringBuilder("📊 Ваша статистика:\n\n");
            
            message.append("💰 Баланс: ").append(formatter.format(bankingService.getBalance(telegramId))).append(" ₽\n\n");
            
            message.append("📈 Пополнения:\n");
            message.append("  • Всего: ").append(formatter.format(stats.getTotalDeposits())).append(" ₽\n");
            message.append("  • Количество: ").append(stats.getDepositCount()).append("\n");
            if (stats.getDepositCount() > 0) {
                message.append("  • Средняя сумма: ").append(formatter.format(stats.getAvgDeposit())).append(" ₽\n");
            }
            message.append("\n");
            
            message.append("📉 Снятия:\n");
            message.append("  • Всего: ").append(formatter.format(stats.getTotalWithdrawals())).append(" ₽\n");
            message.append("  • Количество: ").append(stats.getWithdrawalCount()).append("\n");
            if (stats.getWithdrawalCount() > 0) {
                message.append("  • Средняя сумма: ").append(formatter.format(stats.getAvgWithdrawal())).append(" ₽\n");
            }
            message.append("\n");
            
            if (stats.getTransferOutCount() > 0 || stats.getTransferInCount() > 0) {
                message.append("📤 Переводы:\n");
                message.append("  • Отправлено: ").append(formatter.format(stats.getTotalTransfersOut())).append(" ₽ (").append(stats.getTransferOutCount()).append(")\n");
                message.append("  • Получено: ").append(formatter.format(stats.getTotalTransfersIn())).append(" ₽ (").append(stats.getTransferInCount()).append(")\n\n");
            }
            
            message.append("📋 Всего транзакций: ").append(stats.getTotalTransactions());
            
            sendMessageWithKeyboard(chatId, message.toString(), KeyboardFactory.createMainMenu());
            logger.debug("Статистика запрошена для пользователя telegramId: {}, chatId: {}", telegramId, chatId);
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики для telegramId: {}, chatId: {}", telegramId, chatId, e);
            sendMessage(chatId, "❌ Ошибка при получении статистики: " + e.getMessage());
        }
    }

    private boolean isMainMenuButton(String text) {
        return text.equals("💰 Баланс") || 
               text.equals("💳 Пополнить") || 
               text.equals("💸 Снять") || 
               text.equals("📤 Перевод") ||
               text.equals("📜 История") ||
               text.equals("📊 Статистика");
    }

    private BigDecimal parseAmount(String amountText) {
        try {
            String cleaned = amountText.trim().replace(",", ".").replace(" ", "");
            BigDecimal amount = new BigDecimal(cleaned);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Сумма должна быть положительной");
            }
            
            if (amount.scale() > 2) {
                amount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
            }
            
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат суммы. Введите число, например: 1000 или 1000.50");
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения в chatId: {}", chatId, e);
        }
    }

    private void sendMessageWithKeyboard(Long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения с клавиатурой в chatId: {}", chatId, e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}

