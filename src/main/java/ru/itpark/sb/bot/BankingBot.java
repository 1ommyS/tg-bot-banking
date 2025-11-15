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
        WAITING_WITHDRAWAL_AMOUNT
    }

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
            case "📜 История":
                handleHistory(chatId, telegramId);
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
                String typeEmoji = transaction.getType() == TransactionEntity.TransactionType.DEPOSIT ? "➕" : "➖";
                String typeText = transaction.getType() == TransactionEntity.TransactionType.DEPOSIT ? "Пополнение" : "Снятие";
                
                message.append(typeEmoji).append(" ").append(typeText)
                        .append(": ").append(formatter.format(transaction.getAmount())).append(" ₽\n")
                        .append("📅 ").append(transaction.getCreatedAt().format(dateFormatter)).append("\n");
                
                if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                    message.append("📝 ").append(transaction.getDescription()).append("\n");
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

    private boolean isMainMenuButton(String text) {
        return text.equals("💰 Баланс") || 
               text.equals("💳 Пополнить") || 
               text.equals("💸 Снять") || 
               text.equals("📜 История");
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

