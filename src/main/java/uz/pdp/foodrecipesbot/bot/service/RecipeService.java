package uz.pdp.foodrecipesbot.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import uz.pdp.foodrecipesbot.bot.models.entity.*;
import uz.pdp.foodrecipesbot.bot.repository.*;
import uz.pdp.foodrecipesbot.bot.tgBot.TelegramBot;
import uz.pdp.foodrecipesbot.bot.models.enums.BotState;
import uz.pdp.foodrecipesbot.bot.util.KeyboardUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    private String recipeName;
    private String recipeIngredients;
    private String recipeDesc;
    private Category category;
    private Attachment recipePhoto;

    @Autowired
    @Lazy
    private TelegramBot telegramBot;

    private static final int PAGE_SIZE = 2;
    private final AttachmentRepository attachmentRepository;

    public void sendRecipeWithPhoto(Long chatId, Recipe recipe, int index) {
        User user = userRepository.findByTelegramId(chatId).orElseThrow();
        if (user.getMessageId() != null) {
            telegramBot.deleteMessage(chatId, user.getMessageId());
        }

        String recipeText = String.format(
                "<b>Retsept nomi:</b> %s\n\n" +
                        "<b>Tavsif:</b> %s\n\n" +
                        "<b>Masalliqlar:</b> %s\n\n" +
                        "<b>Tayyorlanishi:</b> %s\n\n" +
                        "<i>Muallif: %s</i>",
                recipe.getName(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                recipe.getAuthor().getUserName()
        );
        if (recipe.getAttachment() != null && recipe.getAttachment().getFile() != null) {
            try {
                Integer i = telegramBot.sendPhoto(
                        chatId,
                        recipe.getAttachment().getFile(),
                        recipeText

                );
                user.setMessageId(i);
                userRepository.save(user);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendRecipesByCategory(Long chatId, String categoryName, int page, Integer messageId) {
        Optional<Category> categoryOptional = categoryRepository.findByName(categoryName);
        if (categoryOptional.isEmpty()) {
            telegramBot.sendMessage(chatId, "Kechirasiz, bunday kategoriya topilmadi.");
            return;
        }

        Category category = categoryOptional.get();
        Page<Recipe> recipePage = recipeRepository.findPageByCategory(
                category,
                PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending())
        );

        List<Recipe> recipes = recipePage.getContent();

        if (recipes.isEmpty()) {
            telegramBot.sendMessage(chatId, "Afsuski, <b>" + categoryName + "</b> kategoriyasida hozircha retseptlar yo'q.", true);
            return;
        }

        String messageText = "<b>" + categoryName + "</b> kategoriyasidagi retseptlar:\n\n";
        messageText += "Sahifa " + (page + 1) + " / " + recipePage.getTotalPages();

        InlineKeyboardMarkup keyboard = KeyboardUtil.getPaginationKeyboard(recipes, categoryName, page, recipePage.getTotalPages());

        if (messageId != null) {
            telegramBot.editMessage(chatId, messageId, messageText, keyboard);
        } else {
            telegramBot.sendMessageWithKeyboard(chatId, messageText, keyboard);
        }
    }

    private String formatRecipeText(Recipe recipe, int index) {
        return String.format(
                "<b>%d. Retsept nomi:</b> %s\n\n" +
                        "<b>Tavsif:</b> %s\n\n" +
                        "<b>Masalliqlar:</b> %s\n\n" +
                        "<b>Tayyorlanishi:</b> %s\n\n" +
                        "<i>Muallif: %s</i>\n" +
                        "────────────────────",
                index,
                recipe.getName(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                recipe.getAuthor().getUserName()
        );
    }


    public void sendCategoriesInlineKeyboard(Long chatId) {
        List<String> categoryNames = categoryRepository.findAll().stream()
                .map(Category::getName)
                .toList();
        if (categoryNames.isEmpty()) {
            telegramBot.sendMessage(chatId, "Hozircha hech qanday kategoriya mavjud emas.");
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), "O'zingizga yoqqan kategoriya bo'yicha tanlab oling:");
        message.setReplyMarkup(KeyboardUtil.getCategoriesInlineKeyboard(categoryNames));
        telegramBot.executeMessage(message);
    }


    public void startAddRecipeFlow(Long chatId, User user) {
        user.setBotState(BotState.ADDING_RECIPE_NAME);
        userRepository.save(user);
        telegramBot.sendMessage(chatId, "Yangi retsept qo'shishni boshlaymiz!\n\nRetsept nomini kiriting:");
    }

    public void handleRecipeName(Long chatId, String name, User user) {
        if (name == null || name.trim().isEmpty()) {
            telegramBot.sendMessage(chatId, "Retsept nomi bo'sh bo'lishi mumkin emas. Iltimos, retsept nomini kiriting:");
            return;
        }

        Long currentRecipeId = user.getCurrentRecipeId();
        Recipe recipe;

        if (currentRecipeId == null) {
            Category defaultCategory = categoryRepository.findByName("General")
                    .orElseGet(() -> {
                        Category generalCategory = Category.builder().name("General").build();
                        return categoryRepository.save(generalCategory);
                    });

            recipeName = name;

            user.setBotState(BotState.ADDING_RECIPE_INGREDIENTS);
            userRepository.save(user);

        } else {
            recipe = recipeRepository.findById(currentRecipeId).orElse(null);
            if (recipe == null) {
                telegramBot.sendMessage(chatId, "Xatolik: Retsept topilmadi. Iltimos, qayta urinib ko'ring.");
                user.setBotState(BotState.MAIN_MENU);
                userRepository.save(user);
                return;
            }
            recipe.setName(name.trim());
            user.setBotState(BotState.ADDING_RECIPE_INGREDIENTS);
            userRepository.save(user);
        }

        telegramBot.sendMessage(chatId, "Masalliqlarni kiriting (har birini yangi qatordan yoki vergul bilan ajratib yozing):");
    }


    public void handleRecipeIngredients(Long chatId, String ingredients, User user) {

        user.setBotState(BotState.ADDING_RECIPE_DESCRIPTION);
        System.out.println("ingredients = " + ingredients);
        recipeIngredients = ingredients;
        userRepository.save(user);
        telegramBot.sendMessage(chatId, "Retsept tavsifini kiriting:");
    }

    public void handleRecipeDescription(Long chatId, String description, User user) {
        recipeDesc = description;
        user.setBotState(BotState.ADDING_RECIPE_CATEGORY);
        userRepository.save(user);

        List<String> categoryNames = categoryRepository.findAll().stream()
                .map(Category::getName)
                .toList();
        if (categoryNames.isEmpty()) {
            telegramBot.sendMessage(chatId, "Hozircha hech qanday kategoriya mavjud emas.");
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), "Recipega mos bo'lgan kategoriyani tanlab oling:");
        message.setReplyMarkup(KeyboardUtil.getCategoriesList(categoryNames));
        telegramBot.executeMessage(message);
    }

    public void handleRecipeCategory(Long chatId, String categoryName, User user) {
        user.setBotState(BotState.ADDING_RECIPE_PHOTO);
        userRepository.save(user);
        Optional<Category> categoryRepositoryByName = categoryRepository.findByName(categoryName);

        if (categoryRepositoryByName.isEmpty()) {
            telegramBot.sendMessage(chatId, "Xatolik: Retseptni aniqlab bo'lmadi.");
            return;
        }

        category = categoryRepositoryByName.get();
        telegramBot.sendMessage(chatId, "Rasm kiriting");
    }


    public void handleRecipePhotoRequest(Long chatId, Update update, User user) {
        if (update.getMessage().hasPhoto()) {
            PhotoSize photo = update.getMessage().getPhoto().stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);

            if (photo != null) {
                String fileId = photo.getFileId();
                handleRecipePhoto(chatId, fileId, user);
            } else {
                telegramBot.sendMessage(chatId, "Fotosuratni tushunib bo'lmadi. Iltimos, qayta yuboring.");
            }
        } else {
            telegramBot.sendMessage(chatId, "Iltimos, retsept uchun fotosurat yuboring!");
        }
    }

    public void handleRecipePhoto(Long chatId, String fileId, User user) {
        try {
            byte[] fileContent = telegramBot.downloadFileBytes(fileId);

            Attachment attachment = new Attachment();
            attachment.setUrl(fileId);
            attachment.setFile(fileContent);
            attachmentRepository.save(attachment);
            this.recipePhoto = attachment;

            user.setBotState(BotState.ADDING_RECIPE_INSTRUCTIONS);
            userRepository.save(user);
            telegramBot.sendMessage(chatId, "Rasm yuklandi! Endi instrukciyalarni kiriting");

        } catch (Exception e) {
            e.printStackTrace();
            telegramBot.sendMessage(chatId, "Rasm yuklanmadi! Yana urinib koring");
        }
    }

    public void handleRecipeInstructions(Long chatId, String instructions, User user) {
        user.setBotState(BotState.MAIN_MENU);
        User savedUser = userRepository.save(user);

        Recipe recipe = new Recipe();
        recipe.setName(recipeName);
        recipe.setIngredients(recipeIngredients);
        recipe.setDescription(recipeDesc);
        recipe.setInstructions(instructions);
        recipe.setAuthor(savedUser);
        recipe.setCategory(category);
        recipe.setAttachment(recipePhoto);
        Recipe save = recipeRepository.save(recipe);

        telegramBot.sendMessage(chatId, "🎉 Retsept muvaffaqiyatli qo'shildi! Endi uni Retseplar ko'rish bo'limidan topishingiz mumkin.");
        telegramBot.sendMainMenu(chatId);
        telegramBot.sendRecipeToChannel(save);
    }

    public void promptForComment(Long chatId, Long recipeId, User user) {
        user.setCurrentRecipeId(recipeId);
        user.setBotState(BotState.WAITING_FOR_COMMENT);
        userRepository.save(user);
        telegramBot.sendMessage(chatId, "Iltimos, izohingizni kiriting:");
    }

    public void saveComment(Long chatId, String commentText, User user) {
        Long currentRecipeId = user.getCurrentRecipeId();
        if (currentRecipeId == null) {
            telegramBot.sendMessage(chatId, "Xatolik: Izoh qoldirish uchun retsept tanlanmagan.");
            user.setBotState(BotState.MAIN_MENU);
            userRepository.save(user);
            return;
        }

        Recipe recipe = recipeRepository.findById(currentRecipeId).orElse(null);
        if (recipe == null) {
            telegramBot.sendMessage(chatId, "Xatolik: Retsept topilmadi.");
            user.setBotState(BotState.MAIN_MENU);
            userRepository.save(user);
            return;
        }

        Comment comment = Comment.builder()
                .text(commentText)
                .user(user)
                .recipe(recipe)
                .createdAt(LocalDateTime.now())
                .build();
        commentRepository.save(comment);
        telegramBot.sendMessage(chatId, "Izohingiz qabul qilindi! ✅");

        user.setBotState(BotState.MAIN_MENU);
        user.setCurrentRecipeId(null);
        userRepository.save(user);
    }

    public void sendCommentsForRecipe(Long chatId, Long recipeId) {
        Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId);
        if (recipeOptional.isEmpty()) {
            telegramBot.sendMessage(chatId, "Kechirasiz, retsept topilmadi.");
            return;
        }
        Recipe recipe = recipeOptional.get();
        List<Comment> comments = commentRepository.findByRecipe(recipe);

        if (comments.isEmpty()) {
            telegramBot.sendMessage(chatId, "Bu retseptga hali hech qanday izoh qoldirilmagan.");
            return;
        }

        StringBuilder commentsText = new StringBuilder("<b>\"").append(recipe.getName()).append("\" retsepti uchun izohlar:</b>\n\n");
        for (Comment comment : comments) {
            commentsText.append(String.format("<b>%s</b>: %s\n",
                    comment.getUser().getUserName(),
                    comment.getText()));
            commentsText.append("<i>").append(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("</i>\n\n");
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), commentsText.toString());
        message.enableHtml(true);
        telegramBot.executeMessage(message);
    }

    public void saveRecipeForUser(Long chatId, User user, Long recipeId) {
        Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId);
        if (recipeOptional.isPresent()) {
            Recipe recipe = recipeOptional.get();
            if (!user.getSavedRecipes().contains(recipe)) {
                user.getSavedRecipes().add(recipe);
                userRepository.save(user);
                telegramBot.sendMessage(chatId, "Retsept muvaffaqiyatli saqlandi! ✅");
            } else {
                telegramBot.sendMessage(chatId, "Bu retsept allaqachon saqlangan retseptlaringizda mavjud.");
            }
        } else {
            telegramBot.sendMessage(chatId, "Kechirasiz, retsept topilmadi.");
        }
    }

    public void sendSavedRecipes(Long chatId, User user) {
        List<Recipe> savedRecipes = user.getSavedRecipes();
        if (savedRecipes.isEmpty()) {
            telegramBot.sendMessage(chatId, "Sizda saqlangan retseptlar yo'q. Retseptlarni ko'rishda ularni saqlashingiz mumkin.");
            return;
        }

        telegramBot.sendMessage(chatId, "<b>Sizning saqlangan retseptlaringiz:</b>\n", true);
        int index = 1;
        for (Recipe recipe : savedRecipes) {
            sendRecipeWithPhoto(chatId, recipe, index++);
        }
    }
}