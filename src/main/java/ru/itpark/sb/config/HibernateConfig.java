package ru.itpark.sb.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import ru.itpark.sb.model.TransactionEntity;
import ru.itpark.sb.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HibernateConfig {
    private static SessionFactory sessionFactory;

    static {
        try {
            Configuration configuration = new Configuration();
            
            Properties properties = loadProperties();
            
            configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            configuration.setProperty("hibernate.connection.url", properties.getProperty("db.url"));
            configuration.setProperty("hibernate.connection.username", properties.getProperty("db.username"));
            configuration.setProperty("hibernate.connection.password", properties.getProperty("db.password"));
            
            configuration.setProperty("hibernate.hikari.maximumPoolSize", "10");
            configuration.setProperty("hibernate.hikari.minimumIdle", "5");
            
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            
            configuration.setProperty("hibernate.show_sql", properties.getProperty("hibernate.show_sql", "false"));
            configuration.setProperty("hibernate.format_sql", properties.getProperty("hibernate.format_sql", "true"));
            
            configuration.setProperty("hibernate.hbm2ddl.auto", properties.getProperty("hibernate.hbm2ddl.auto", "update"));
            
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(TransactionEntity.class);
            
            sessionFactory = configuration.buildSessionFactory();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Ошибка инициализации Hibernate: " + e.getMessage());
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = HibernateConfig.class.getClassLoader()
                .getResourceAsStream("application.properties");
        
        if (inputStream != null) {
            properties.load(inputStream);
            inputStream.close();
        }
        
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = properties.getProperty("db.url", "jdbc:postgresql://localhost:5432/banking_bot");
        }
        properties.setProperty("db.url", dbUrl);
        
        String dbUsername = System.getenv("DB_USERNAME");
        if (dbUsername == null || dbUsername.isEmpty()) {
            dbUsername = properties.getProperty("db.username", "postgres");
        }
        properties.setProperty("db.username", dbUsername);
        
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = properties.getProperty("db.password", "postgres");
        }
        properties.setProperty("db.password", dbPassword);
        
        properties.setProperty("hibernate.show_sql", 
                System.getenv().getOrDefault("HIBERNATE_SHOW_SQL", 
                        properties.getProperty("hibernate.show_sql", "false")));
        properties.setProperty("hibernate.format_sql", 
                System.getenv().getOrDefault("HIBERNATE_FORMAT_SQL", 
                        properties.getProperty("hibernate.format_sql", "true")));
        properties.setProperty("hibernate.hbm2ddl.auto", 
                System.getenv().getOrDefault("HIBERNATE_HBM2DDL_AUTO", 
                        properties.getProperty("hibernate.hbm2ddl.auto", "update")));
        
        return properties;
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}

