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
import uz.pdp.foodrecipesbot.bot.tgBot.TelegramBot;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;
import uz.pdp.foodrecipesbot.bot.models.entity.Comment;
import uz.pdp.foodrecipesbot.bot.models.entity.Recipe;
import uz.pdp.foodrecipesbot.bot.models.entity.User;
import uz.pdp.foodrecipesbot.bot.models.enums.BotState;
import uz.pdp.foodrecipesbot.bot.repository.CategoryRepository;
import uz.pdp.foodrecipesbot.bot.repository.CommentRepository;
import uz.pdp.foodrecipesbot.bot.repository.RecipeRepository;
import uz.pdp.foodrecipesbot.bot.repository.UserRepository;
import uz.pdp.foodrecipesbot.bot.util.KeyboardUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private String recipeInstructions;

    @Autowired
    @Lazy
    private TelegramBot telegramBot;

    private static final int PAGE_SIZE = 2; // Количество рецептов на странице

    public void sendRecipesByCategory(Long chatId, String categoryName, int page) {
        Optional<Category> categoryOptional = categoryRepository.findByName(categoryName);
        if (categoryOptional.isEmpty()) {
            telegramBot.sendMessage(chatId, "Kechirasiz, bunday kategoriya topilmadi.");
            return;
        }

        Category category = categoryOptional.get();
        Page<Recipe> recipePage = recipeRepository.findByCategory(
                category,
                PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending())
        );

        List<Recipe> recipes = recipePage.getContent();

        if (recipes.isEmpty()) {
            telegramBot.sendMessage(chatId, "Afsuski, <b>" + categoryName + "</b> kategoriyasida hozircha retseptlar yo'q.", true);
            return;
        }

        // Отправка рецептов
        for (Recipe recipe : recipes) {
            String recipeText = formatRecipeText(recipe);
            SendMessage message = new SendMessage(String.valueOf(chatId), recipeText);
            message.enableHtml(true);
            message.setReplyMarkup(KeyboardUtil.getRecipeActionInlineKeyboard(recipe.getId(), recipe.getAuthor().getId()));
            telegramBot.executeMessage(message);
        }

        // Добавляем кнопки пагинации
        if (recipePage.getTotalPages() > 1) {
            SendMessage paginationMessage = new SendMessage(String.valueOf(chatId), "Sahifa " + (page + 1) + " / " + recipePage.getTotalPages());
            paginationMessage.setReplyMarkup(KeyboardUtil.getPaginationKeyboard(categoryName, page, recipePage.getTotalPages()));
            telegramBot.executeMessage(paginationMessage);
        }
    }

    private String formatRecipeText(Recipe recipe) {
        return String.format(
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

    public void sendRecipesByCategory(Long chatId, String categoryName) {
        Optional<Category> categoryOptional = categoryRepository.findByName(categoryName);
        if (categoryOptional.isEmpty()) {
            telegramBot.sendMessage(chatId, "Kechirasiz, bunday kategoriya topilmadi.");
            return;
        }
        Category category = categoryOptional.get();
        List<Recipe> recipes = recipeRepository.findByCategory(category);

        if (recipes.isEmpty()) {
            telegramBot.sendMessage(chatId, "Afsuski, <b>" + categoryName + "</b> kategoriyasida hozircha retseptlar yo'q.", true);
            return;
        }

        for (Recipe recipe : recipes) {
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
            SendMessage message = new SendMessage(String.valueOf(chatId), recipeText);
            message.enableHtml(true);
            message.setReplyMarkup(KeyboardUtil.getRecipeActionInlineKeyboard(recipe.getId(), recipe.getAuthor().getId()));
            telegramBot.executeMessage(message);
        }
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

//            recipe = Recipe.builder()
//                    .author(user)
//                    .category(defaultCategory)
//                    .name(name.trim())
//                    .build();

//            recipe = new Recipe();
//            recipe.setCategory(defaultCategory);
//            recipe.setName(name);
//            recipe.setAuthor(user);
//
//            Recipe savedRecipe = recipeRepository.save(recipe);
//            System.out.println("savedRecipe.getId() = " + savedRecipe.getId());
//            user.setCurrentRecipeId(savedRecipe.getId());

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

//        recipeRepository.save(recipe);

        telegramBot.sendMessage(chatId, "Masalliqlarni kiriting (har birini yangi qatordan yoki vergul bilan ajratib yozing):");
    }


    public void handleRecipeIngredients(Long chatId, String ingredients, User user) {
//        Long currentRecipeId = user.getCurrentRecipeId();
//        if (currentRecipeId == null) {
//            telegramBot.sendMessage(chatId, "Xatolik: Retseptni aniqlab bo'lmadi.");
//            user.setBotState(BotState.MAIN_MENU);
//            userRepository.save(user);
//            return;
//        }
//        Recipe recipe = recipeRepository.findById(currentRecipeId).orElse(null);
//        if (recipe == null) {
//            telegramBot.sendMessage(chatId, "Xatolik: Retsept topilmadi.");
//            user.setBotState(BotState.MAIN_MENU);
//            userRepository.save(user);
//            return;
//        }
//        System.out.println("recipe.getName() = " + recipe.getName());

        user.setBotState(BotState.ADDING_RECIPE_DESCRIPTION);
        System.out.println("ingredients = " + ingredients);
//        System.out.println("recipe.getIngredients() = " + recipe.getIngredients());
//        recipe.setIngredients(ingredients);
//        System.out.println("recipe.getIngredients() = " + recipe.getIngredients());
//
//        recipeRepository.save(recipe);
        recipeIngredients = ingredients;
        userRepository.save(user);
        telegramBot.sendMessage(chatId, "Retsept tavsifini kiriting:");
    }

    public void handleRecipeDescription(Long chatId, String description, User user) {
//        Long currentRecipeId = user.getCurrentRecipeId();
//        if (currentRecipeId == null) {
//            telegramBot.sendMessage(chatId, "Xatolik: Retseptni aniqlab bo'lmadi.");
//            user.setBotState(BotState.MAIN_MENU);
//            userRepository.save(user);
//            return;
//        }
//        Recipe recipe = recipeRepository.findById(currentRecipeId).orElse(null);
//        if (recipe == null) {
//            telegramBot.sendMessage(chatId, "Xatolik: Retsept topilmadi.");
//            user.setBotState(BotState.MAIN_MENU);
//            userRepository.save(user);
//            return;
//        }
//
//        recipe.setDescription(description);
        recipeDesc = description;
        user.setBotState(BotState.ADDING_RECIPE_CATEGORY);
        userRepository.save(user);
//        recipeRepository.save(recipe);

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

    public void handleRecipeCategory(Long chatId, String categoryName,User user){

        Optional<Category> categoryRepositoryByName = categoryRepository.findByName(categoryName);
        if (categoryRepositoryByName.isEmpty()) {
            telegramBot.sendMessage(chatId, "Xatolik: Retseptni aniqlab bo'lmadi.");
        }
        category = categoryRepositoryByName.get();
        user.setBotState(BotState.ADDING_RECIPE_INSTRUCTIONS);
        userRepository.save(user);
//        telegramBot.executeMessage(message);
        telegramBot.sendMessage(chatId, "Tayyorlash bosqichlarini kiriting (har bir bosqichni yangi qatordan yozing):");
    }


    public void handleRecipeInstructions(Long chatId, String instructions, User user) {
//        Long currentRecipeId = user.getCurrentRecipeId();
//        if (currentRecipeId == null) {
//            telegramBot.sendMessage(chatId, "Xatolik: Retseptni aniqlab bo'lmadi.");
//            user.setBotState(BotState.MAIN_MENU);
//            userRepository.save(user);
//            return;
//        }
//        Recipe recipe = recipeRepository.findById(currentRecipeId).orElse(null);
//        if (recipe == null) {
//            telegramBot.sendMessage(chatId, "Xatolik: Retsept topilmadi.");
//            user.setBotState(BotState.MAIN_MENU);
//            userRepository.save(user);
//            return;
//        }
//
//        recipe.setInstructions(instructions);
       /* Category defaultCategory = categoryRepository.findByName("General")
                .orElseGet(() -> {
                    Category generalCategory = Category.builder().name("General").build();
                    return categoryRepository.save(generalCategory);
                });*/

        user.setBotState(BotState.MAIN_MENU);
//        user.setCurrentRecipeId(null);
        User savedUser = userRepository.save(user);

        Recipe recipe = new Recipe();
        recipe.setName(recipeName);
        recipe.setIngredients(recipeIngredients);
        recipe.setDescription(recipeDesc);
        recipe.setInstructions(instructions);
        recipe.setAuthor(savedUser);
        recipe.setCategory(category);
        recipeRepository.save(recipe);

        telegramBot.sendMessage(chatId, "🎉 Retsept muvaffaqiyatli qo'shildi! Endi uni Retseplar ko'rish bo'limidan topishingiz mumkin.");
        telegramBot.sendMainMenu(chatId);
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
            commentsText.append(String.format("<b>%s</b> (@%s): %s\n",
                    comment.getUser().getUserName(),
                    comment.getUser().getUname() != null ? comment.getUser().getUname() : "no_usepdp rname",
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
        for (Recipe recipe : savedRecipes) {
            String recipeText = String.format(
                    "<b>Retsept nomi:</b> %s\n\n" +
                            "<b>Tavsif:</b> %s\n\n" +
                            "<i>Muallif: %s</i>",
                    recipe.getName(),
                    recipe.getDescription(),
                    recipe.getAuthor().getUserName()
            );
            SendMessage message = new SendMessage(String.valueOf(chatId), recipeText);
            message.enableHtml(true);
            message.setReplyMarkup(KeyboardUtil.getRecipeActionInlineKeyboard(recipe.getId(), recipe.getAuthor().getId()));
            telegramBot.executeMessage(message);
        }
    }
}