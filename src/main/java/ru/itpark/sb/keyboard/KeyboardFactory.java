package ru.itpark.sb.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {
    public static ReplyKeyboardMarkup createMainMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("💰 Баланс");
        row1.add("💳 Пополнить");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("💸 Снять");
        row2.add("📜 История");
        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createCancelMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("❌ Отмена");
        keyboardRows.add(row1);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
}

