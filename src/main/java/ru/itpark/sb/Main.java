package ru.itpark.sb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.itpark.sb.bot.BankingBot;
import ru.itpark.sb.config.HibernateConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Инициализация Hibernate...");
            HibernateConfig.getSessionFactory();
            logger.info("Hibernate инициализирован успешно.");

            Properties properties = loadProperties();
            String botToken = getBotToken(properties);
            String botUsername = getBotUsername(properties);

            if (botToken == null || botToken.isEmpty() || botToken.equals("your_bot_token_here")) {
                logger.error("ОШИБКА: Токен бота не установлен!");
                logger.error("Укажите токен в application.properties (telegram.bot.token) или установите переменную окружения TELEGRAM_BOT_TOKEN");
                System.exit(1);
            }

            logger.info("Запуск Telegram бота...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            BankingBot bot = new BankingBot(botToken, botUsername);
            botsApi.registerBot(bot);
            logger.info("Бот успешно запущен и готов к работе!");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Завершение работы...");
                HibernateConfig.shutdown();
            }));

        } catch (TelegramApiException e) {
            logger.error("Ошибка при запуске бота: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Ошибка: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = Main.class.getClassLoader()
                .getResourceAsStream("application.properties");

        if (inputStream != null) {
            properties.load(inputStream);
            inputStream.close();
        }

        return properties;
    }

    private static String getBotToken(Properties properties) {
        String token = properties.getProperty("telegram.bot.token");
        
        if (token == null || token.isEmpty() || token.equals("your_bot_token_here")) {
            String envToken = System.getenv("TELEGRAM_BOT_TOKEN");
            if (envToken != null && !envToken.isEmpty()) {
                logger.info("Токен бота получен из переменной окружения TELEGRAM_BOT_TOKEN");
                return envToken;
            }
            return token;
        }
        
        logger.debug("Токен бота получен из application.properties");
        return token;
    }

    private static String getBotUsername(Properties properties) {
        String username = properties.getProperty("telegram.bot.username");
        
        if (username == null || username.isEmpty()) {
            String envUsername = System.getenv("TELEGRAM_BOT_USERNAME");
            if (envUsername != null && !envUsername.isEmpty()) {
                logger.info("Имя бота получено из переменной окружения TELEGRAM_BOT_USERNAME");
                return envUsername;
            }
            logger.debug("Имя бота не указано, используется значение по умолчанию: banking_bot");
            return "banking_bot";
        }
        
        logger.debug("Имя бота получено из application.properties");
        return username;
    }
}
