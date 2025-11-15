package ru.itpark.sb.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itpark.sb.service.BankingService;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BankingBotTest {

    @Mock
    private BankingService bankingService;

    private BankingBot bankingBot;

    @BeforeEach
    void setUp() {
        bankingBot = new BankingBot("test_token", "test_bot");
    }

    @Test
    void testParseAmount_ValidNumber() throws Exception {
        Method parseAmountMethod = BankingBot.class.getDeclaredMethod("parseAmount", String.class);
        parseAmountMethod.setAccessible(true);

        BigDecimal result1 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "1000");
        assertEquals(0, new BigDecimal("1000.00").compareTo(result1));

        BigDecimal result2 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "1000.50");
        assertEquals(0, new BigDecimal("1000.50").compareTo(result2));

        BigDecimal result3 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "1000,50");
        assertEquals(0, new BigDecimal("1000.50").compareTo(result3));

        BigDecimal result4 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "1 000.50");
        assertEquals(0, new BigDecimal("1000.50").compareTo(result4));

        BigDecimal result5 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "  500.99  ");
        assertEquals(0, new BigDecimal("500.99").compareTo(result5));
    }

    @Test
    void testParseAmount_InvalidFormat() throws Exception {
        Method parseAmountMethod = BankingBot.class.getDeclaredMethod("parseAmount", String.class);
        parseAmountMethod.setAccessible(true);

        assertThrows(Exception.class, () -> {
            parseAmountMethod.invoke(bankingBot, "abc");
        });

        assertThrows(Exception.class, () -> {
            parseAmountMethod.invoke(bankingBot, "");
        });

        assertThrows(Exception.class, () -> {
            parseAmountMethod.invoke(bankingBot, "not a number");
        });
    }

    @Test
    void testParseAmount_NegativeOrZero() throws Exception {
        Method parseAmountMethod = BankingBot.class.getDeclaredMethod("parseAmount", String.class);
        parseAmountMethod.setAccessible(true);

        assertThrows(Exception.class, () -> {
            parseAmountMethod.invoke(bankingBot, "-100");
        });

        assertThrows(Exception.class, () -> {
            parseAmountMethod.invoke(bankingBot, "0");
        });

        assertThrows(Exception.class, () -> {
            parseAmountMethod.invoke(bankingBot, "-0.01");
        });
    }

    @Test
    void testParseAmount_ScaleRounding() throws Exception {
        Method parseAmountMethod = BankingBot.class.getDeclaredMethod("parseAmount", String.class);
        parseAmountMethod.setAccessible(true);

        BigDecimal result1 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "1000.999");
        assertEquals(0, new BigDecimal("1001.00").compareTo(result1));

        BigDecimal result2 = (BigDecimal) parseAmountMethod.invoke(bankingBot, "1000.994");
        assertEquals(0, new BigDecimal("1000.99").compareTo(result2));
    }

    @Test
    void testIsMainMenuButton() throws Exception {
        Method isMainMenuButtonMethod = BankingBot.class.getDeclaredMethod("isMainMenuButton", String.class);
        isMainMenuButtonMethod.setAccessible(true);

        assertTrue((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "💰 Баланс"));
        assertTrue((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "💳 Пополнить"));
        assertTrue((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "💸 Снять"));
        assertTrue((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "📤 Перевод"));
        assertTrue((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "📜 История"));
        assertTrue((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "📊 Статистика"));

        assertFalse((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "Unknown"));
        assertFalse((Boolean) isMainMenuButtonMethod.invoke(bankingBot, "/start"));
    }
}

