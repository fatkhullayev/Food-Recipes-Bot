package uz.pdp.foodrecipesbot.bot.tgBot;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;
import uz.pdp.foodrecipesbot.bot.models.entity.Recipe;
import uz.pdp.foodrecipesbot.bot.models.enums.BotState; // To'g'ri paketni import qiling
import uz.pdp.foodrecipesbot.bot.repository.CategoryRepository;
import uz.pdp.foodrecipesbot.bot.repository.RecipeRepository;
import uz.pdp.foodrecipesbot.bot.service.UserService;
import uz.pdp.foodrecipesbot.bot.service.RecipeService;
import uz.pdp.foodrecipesbot.bot.util.KeyboardUtil;
import uz.pdp.foodrecipesbot.bot.models.entity.User; // To'g'ri paketni import qiling

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.awt.SystemColor.text;

@Slf4j
@Component
@Getter
@Setter
@Lazy
public class TelegramBot extends TelegramLongPollingBot {
    private final CategoryRepository categoryRepository;
    private final RecipeRepository recipeRepository;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final UserService userService;
    private final @Lazy RecipeService recipeService;

    public TelegramBot(UserService userService, RecipeService recipeService,
                       CategoryRepository categoryRepository, RecipeRepository recipeRepository) {
        this.userService = userService;
        this.recipeService = recipeService;
        this.categoryRepository = categoryRepository;
        this.recipeRepository = recipeRepository;
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

    @SneakyThrows
    private void handleMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        User user = userService.getOrCreateUser(chatId, update.getMessage().getFrom().getFirstName());
        if (update.getMessage().hasContact()) {
            handleContact(update);
            if (user.getCurrentRecipeId() != null) {

                Recipe recipe = recipeRepository.findById(user.getCurrentRecipeId()).orElse(null);

                sendPhoto(chatId, Objects.requireNonNull(recipe).getAttachment().getFile(),
                        String.format(
                                "<b>Kanal orqali qidirilgan retsept</b> \n\n" +
                                        "<b>Retsept nomi:</b> %s\n\n" +
                                        "<b>Tavsif:</b> %s\n\n" +
                                        "<b>Masalliqlar:</b> %s\n\n" +
                                        "<b>Tayyorlanishi:</b> %s\n\n" +
                                        "<i>Muallif: %s</i>\n",
                                escapeHtml(recipe.getName()),
                                escapeHtml(recipe.getDescription()),
                                escapeHtml(recipe.getIngredients()),
                                escapeHtml(recipe.getInstructions()),
                                escapeHtml(recipe.getAuthor().getUserName()))
                );
            }
            sendMainMenu(chatId);
            return;
        }

        switch (user.getBotState()) {
            case START:
                String[] parts = messageText.split(" ");
                if (parts.length > 1 && parts[1].startsWith("recipe_")) {
                    Long recipeId = Long.parseLong(parts[1].substring(7));
                    user.setCurrentRecipeId(recipeId);
                    sendPhoneNumberRequest(chatId);
                    user.setBotState(BotState.WAITING_FOR_PHONE_NUMBER);
                    userService.saveUser(user);
                }
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
                recipeService.handleRecipeCategory(chatId, messageText, user);
                break;
            case ADDING_RECIPE_PHOTO:
                recipeService.handleRecipePhotoRequest(chatId, update, user);
                break;
            case WAITING_FOR_COMMENT:
                recipeService.saveComment(chatId, messageText, user);
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
    }

    public void sendMainMenu(Long chatId) {
        SendMessage message = new SendMessage(String.valueOf(chatId), "Quyidagi tugmalardan birini tanlang:");
        message.setReplyMarkup(KeyboardUtil.getMainMenuKeyboard());
        executeMessage(message);
    }

    @SneakyThrows
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
                String[] parts = messageText.split(" ");
                if (parts.length > 1 && parts[1].startsWith("recipe_")) {
                    Long recipeId = Long.parseLong(parts[1].substring(7));
                    Recipe recipe = recipeRepository.findById(recipeId).orElse(null);

                    sendPhoto(chatId, Objects.requireNonNull(recipe).getAttachment().getFile(),
                            String.format(
                                    "<b>Retsept nomi:</b> %s\n\n" +
                                            "<b>Tavsif:</b> %s\n\n" +
                                            "<b>Masalliqlar:</b> %s\n\n" +
                                            "<b>Tayyorlanishi:</b> %s\n\n" +
                                            "<i>Muallif: %s</i>\n",
                                    escapeHtml(recipe.getName()),
                                    escapeHtml(recipe.getDescription()),
                                    escapeHtml(recipe.getIngredients()),
                                    escapeHtml(recipe.getInstructions()),
                                    escapeHtml(recipe.getAuthor().getUserName()))
                    );
                    return;
                }
                sendMessage(chatId, "Kechirasiz, men sizning buyrug'ingizni tushunmadim. Iltimos, quyidagi tugmalardan foydalaning.");
                sendMainMenu(chatId);
                break;
        }
    }

    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText(text);
        message.enableHtml(true);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Xabarni tahrirlashda xato: " + e.getMessage());
        }
    }

    public void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.enableHtml(true);
        message.setReplyMarkup(keyboard);
        executeMessage(message);
    }

    private void handleCallbackQuery(Update update) {

        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        User user = userService.getOrCreateUser(chatId, update.getCallbackQuery().getFrom().getFirstName());

        if (callbackData.startsWith("CAT_")) {
            String categoryName = callbackData.substring(4);
            recipeService.sendRecipesByCategory(chatId, categoryName, 0, messageId);

        } else if (callbackData.startsWith("PAGE_")) {
            String[] parts = callbackData.split("_");
            String categoryName = parts[1];
            int page = Integer.parseInt(parts[2]);
            recipeService.sendRecipesByCategory(chatId, categoryName, page, messageId);

        } else if (callbackData.equals("BACK_TO_CATEGORIES")) {
            List<String> categoryNames = categoryRepository.findAll().stream()
                    .map(Category::getName)
                    .toList();

            SendMessage message = new SendMessage(chatId.toString(), "Kategoriyalar:");
            message.setReplyMarkup(KeyboardUtil.getCategoriesInlineKeyboard(categoryNames));
            executeMessage(message);

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

        } else if (callbackData.startsWith("RECIPE_SAVE_")) {
            Long recipeId = Long.parseLong(callbackData.substring("RECIPE_SAVE_".length()));
            recipeService.saveRecipeForUser(chatId, user, recipeId);

        } else if (callbackData.startsWith("RECIPE_DETAIL_")) {
            Long recipeId = Long.parseLong(callbackData.substring("RECIPE_DETAIL_".length()));
            Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId);

            if (recipeOptional.isPresent()) {
                Recipe recipe = recipeOptional.get();
                recipeService.sendRecipeWithPhoto(chatId, recipe, 1);

            } else {
                sendMessage(chatId, "Kechirasiz, retsept topilmadi.");
            }
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

    @SneakyThrows
    public byte[] downloadFileBytes(String fileId) throws TelegramApiException {
        GetFile getFile = new GetFile(fileId);
        File file = execute(getFile);

        java.io.File downloadedFile = downloadFile(file);

        return Files.readAllBytes(downloadedFile.toPath());
    }

    public Integer sendPhoto(Long chatId, byte[] photoBytes, String caption) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(photoBytes), "recipe.jpg"));
        sendPhoto.setCaption(caption);
        sendPhoto.setParseMode(ParseMode.HTML);
        Message execute = execute(sendPhoto);
        return execute.getMessageId();
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Xabarni o'chirishda xato: " + e.getMessage());
        }
    }

    @SneakyThrows
    public void sendRecipeToChannel(Recipe recipe) {
        String deepLink = "https://t.me/" + botUsername + "?start=recipe_" + recipe.getId();

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(-1002455002479L);
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(recipe.getAttachment().getFile()), "recipe.jpg"));

        String caption = String.format(
                "<b>Retsept nomi:</b> %s\n\n" +
                        "<b>Tavsif:</b> %s\n\n" +
                        "<b>Masalliqlar:</b> %s\n\n" +
                        "<b>Tayyorlanishi:</b> %s\n\n" +
                        "<i>Muallif: %s</i>\n" +
                        "────────────────────\n" +
                        "<a href=\"%s\">📌 Retseptni botda ko‘rish</a>",
                escapeHtml(recipe.getName()),
                escapeHtml(recipe.getDescription()),
                escapeHtml(recipe.getIngredients()),
                escapeHtml(recipe.getInstructions()),
                escapeHtml(recipe.getAuthor().getUserName()),
                deepLink
        );

        sendPhoto.setCaption(caption);
        sendPhoto.setParseMode(ParseMode.HTML);
        execute(sendPhoto);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}