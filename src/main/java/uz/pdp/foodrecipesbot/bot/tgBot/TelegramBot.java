// src/main/java/uz/pdp/foodrecipesbot/bot/tgBot/TelegramBot.java

package uz.pdp.foodrecipesbot.bot.tgBot;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.pdp.foodrecipesbot.bot.models.enums.BotState; // To'g'ri paketni import qiling
import uz.pdp.foodrecipesbot.bot.service.UserService;
import uz.pdp.foodrecipesbot.bot.service.RecipeService;
import uz.pdp.foodrecipesbot.bot.util.KeyboardUtil;
import uz.pdp.foodrecipesbot.bot.models.entity.User; // To'g'ri paketni import qiling

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@Component
@Getter
@Setter
@Lazy
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final UserService userService;
    private final RecipeService recipeService;

    public TelegramBot(UserService userService, RecipeService recipeService) {
        this.userService = userService;
        this.recipeService = recipeService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("Telegram bot muvaffaqiyatli ro'yxatdan o'tkazildi va ishga tushdi!");
        } catch (TelegramApiException e) {
            log.error("Botni ro'yxatdan o'tkazishda xato: " + e.getMessage());
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

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }



    private void handleMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        if (update.getMessage().hasContact()) {
            handleContact(update);
            return;
        }


        User user = userService.getOrCreateUser(chatId, update.getMessage().getFrom().getFirstName());

        switch (user.getBotState()) {
            case START:
                if ("/start".equals(messageText)) {
                    sendPhoneNumberRequest(chatId);
                    user.setBotState(BotState.WAITING_FOR_PHONE_NUMBER);
                    userService.saveUser(user);
                }
                break;
            case WAITING_FOR_PHONE_NUMBER:
                sendMessage(chatId, "Telefon raqamingizni pastdagi tugma orqali yuboring iltimos.");
                break;
            case MAIN_MENU:
                handleMainMenuSelection(chatId, messageText, user);
                break;
            case ADDING_RECIPE_NAME:
                recipeService.handleRecipeName(chatId, messageText, user);
                break;
            case ADDING_RECIPE_INGREDIENTS:
                recipeService.handleRecipeIngredients(chatId, messageText, user);
                break;
            case ADDING_RECIPE_DESCRIPTION:
                recipeService.handleRecipeDescription(chatId, messageText, user);
                break;
            case ADDING_RECIPE_INSTRUCTIONS:
                recipeService.handleRecipeInstructions(chatId, messageText, user);
                break;
            case ADDING_RECIPE_CATEGORY:
                recipeService.handleRecipeCategory
            case WAITING_FOR_COMMENT:
                recipeService.saveComment(chatId, messageText, user);
                // Comment saqlangandan keyin holatni MAIN_MENU ga o'tkazish va menyuni ko'rsatish
                user.setBotState(BotState.MAIN_MENU);
                userService.saveUser(user);
                sendMainMenu(chatId);
                break;
            default:
                sendMessage(chatId, "Noma'lum buyruq. Iltimos, bosh menyudagi tugmalardan birini tanlang.");
                sendMainMenu(chatId);
                break;
        }
    }

    private void handleContact(Update update) {
        Long chatId = update.getMessage().getChatId();
        String phoneNumber = update.getMessage().getContact().getPhoneNumber();

        User user = userService.getOrCreateUser(chatId, update.getMessage().getFrom().getFirstName());
        user.setPhoneNumber(phoneNumber);
        user.setBotState(BotState.MAIN_MENU);
        userService.saveUser(user);

        sendWelcomeMessageAndMainMenu(chatId, user.getUserName());
    }

    private void sendPhoneNumberRequest(Long chatId) {
        SendMessage message = new SendMessage(String.valueOf(chatId), "Assalomu alaykum! Botimizga xush kelibsiz. Iltimos, davom etish uchun telefon raqamingizni yuboring.");
        message.setReplyMarkup(KeyboardUtil.getPhoneNumberRequestKeyboard());
        executeMessage(message);
    }

    private void sendWelcomeMessageAndMainMenu(Long chatId, String userName) {
        String welcomeMessage = String.format(
                "🤩 Salom, %s! Botimizga xush kelibsiz! Bu yerda siz bir-biridan mazali retseptlarni topishingiz, o'zingizning retseptlaringizni baham ko'rishingiz va yangi do'stlar orttirishingiz mumkin.\n\n" +
                        "Keling, sevimli taomlaringizni birga yaratamiz! 🍜🍝🍕🍣",
                userName
        );
        sendMessage(chatId, welcomeMessage);
        sendMainMenu(chatId);
    }

    public void sendMainMenu(Long chatId) {
        SendMessage message = new SendMessage(String.valueOf(chatId), "Quyidagi tugmalardan birini tanlang:");
        message.setReplyMarkup(KeyboardUtil.getMainMenuKeyboard());
        executeMessage(message);
    }

    private void handleMainMenuSelection(Long chatId, String messageText, User user) {
        switch (messageText) {
            case "Mening profilim":
                userService.sendUserProfile(chatId, user);
                break;
            case "Retseplar ko'rish":
                recipeService.sendCategoriesInlineKeyboard(chatId);
                break;
            case "Retsep qo'shish":
                recipeService.startAddRecipeFlow(chatId, user);
                break;
            case "Notifications":
                userService.sendNotifications(chatId, user);
                break;
            case "Saqlangan retseplar":
                recipeService.sendSavedRecipes(chatId, user);
                break;
            default:
                sendMessage(chatId, "Kechirasiz, men sizning buyrug'ingizni tushunmadim. Iltimos, quyidagi tugmalardan foydalaning.");
                sendMainMenu(chatId);
                break;
        }
    }



    private void handleCallbackQuery(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackData = update.getCallbackQuery().getData();
        // Integer messageId = update.getCallbackQuery().getMessage().getMessageId(); // Agar kerak bo'lsa
        User user = userService.getOrCreateUser(chatId, update.getCallbackQuery().getFrom().getFirstName());

        if (callbackData.startsWith("CAT_")) {
            String categoryName = callbackData.substring(4);
            recipeService.sendRecipesByCategory(chatId, categoryName);
        } else if (callbackData.startsWith("RECIPE_COMMENT_")) {
            Long recipeId = Long.parseLong(callbackData.substring("RECIPE_COMMENT_".length()));
            recipeService.promptForComment(chatId, recipeId, user);
        } else if (callbackData.startsWith("RECIPE_VIEW_COMMENTS_")) {
            Long recipeId = Long.parseLong(callbackData.substring("RECIPE_VIEW_COMMENTS_".length()));
            recipeService.sendCommentsForRecipe(chatId, recipeId);
        } else if (callbackData.startsWith("RECIPE_FOLLOW_AUTHOR_")) {
            Long recipeAuthorId = Long.parseLong(callbackData.substring("RECIPE_FOLLOW_AUTHOR_".length()));
            userService.followUser(user, recipeAuthorId);
            sendMessage(chatId, "Muallifga obuna bo'ldingiz! 🎉");
        }
        else if (callbackData.startsWith("RECIPE_SAVE_")) {
            Long recipeId = Long.parseLong(callbackData.substring("RECIPE_SAVE_".length()));
            recipeService.saveRecipeForUser(chatId, user, recipeId);
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        executeMessage(message);
    }

    public void sendMessage(Long chatId, String text, boolean enableHtml) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.enableHtml(enableHtml);
        executeMessage(message);
    }

    public void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Xabar yuborishda xato: " + e.getMessage());
        }
    }


}