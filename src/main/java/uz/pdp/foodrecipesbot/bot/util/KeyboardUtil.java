// src/main/java/uz/pdp/foodrecipesbot/bot/util/KeyboardUtil.java

package uz.pdp.foodrecipesbot.bot.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyboardUtil {

    public static ReplyKeyboardMarkup getPhoneNumberRequestKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton("📱 Telefon raqamimni yuborish");
        button.setRequestContact(true);
        row.add(button);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Mening profilim");
        row1.add("Retseplar ko'rish");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Retsep qo'shish");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Notifications");
        row3.add("Saqlangan retseplar");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup getPaginationKeyboard(String categoryName, int currentPage, int totalPages) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> paginationRow = new ArrayList<>();

        // Кнопка "Назад"
        if (currentPage > 0) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("⬅️ Oldingi")
                    .callbackData("PAGE_" + categoryName + "_" + (currentPage - 1))
                    .build());
        }

        // Кнопка "Вперед"
        if (currentPage < totalPages - 1) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("Keyingi ➡️")
                    .callbackData("PAGE_" + categoryName + "_" + (currentPage + 1))
                    .build());
        }

        if (!paginationRow.isEmpty()) {
            rows.add(paginationRow);
        }

        // Кнопка возврата к категориям
        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("🔙 Kategoriyalarga qaytish")
                        .callbackData("BACK_TO_CATEGORIES")
                        .build()
        ));

        markup.setKeyboard(rows);
        return markup;
    }

    public static ReplyKeyboardMarkup getCategoriesList(List<String> categories) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (String category : categories) {
            KeyboardRow row = new KeyboardRow();
            row.add(category);
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup getCategoriesInlineKeyboard(List<String> categories) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String category : categories) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category);
            button.setCallbackData("CAT_" + category);
            rowInline.add(button);
            rowsInline.add(rowInline);
        }
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public static InlineKeyboardMarkup getRecipeActionInlineKeyboard(Long recipeId, Long authorId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder().text("➕ Muallifga obuna bo'lish").callbackData("RECIPE_FOLLOW_AUTHOR_" + authorId).build());
        row1.add(InlineKeyboardButton.builder().text("💬 Fikr bildirish").callbackData("RECIPE_COMMENT_" + recipeId).build());
        rowsInline.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder().text("👁️ Fikrlarni ko'rish").callbackData("RECIPE_VIEW_COMMENTS_" + recipeId).build());
        row2.add(InlineKeyboardButton.builder().text("💾 Retseptni saqlash").callbackData("RECIPE_SAVE_" + recipeId).build());
        rowsInline.add(row2);

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }
}