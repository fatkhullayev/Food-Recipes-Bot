// src/main/java/uz/pdp/foodrecipesbot/bot/service/UserService.java

package uz.pdp.foodrecipesbot.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import uz.pdp.foodrecipesbot.bot.tgBot.TelegramBot;
import uz.pdp.foodrecipesbot.bot.models.entity.Recipe;
import uz.pdp.foodrecipesbot.bot.models.entity.User;
import uz.pdp.foodrecipesbot.bot.models.enums.BotState;
import uz.pdp.foodrecipesbot.bot.repository.UserRepository;
import uz.pdp.foodrecipesbot.bot.repository.RecipeRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor // Constructor injection uchun
@Transactional
public class UserService {

    private final UserRepository userRepository;
    @Autowired
    @Lazy
    private TelegramBot telegramBot;
    private final RecipeRepository recipeRepository;

    public User getOrCreateUser(Long telegramId, String userName) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .telegramId(telegramId)
                            .userName(userName)
                            .botState(BotState.START) // Boshlang'ich holat
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }


    public void sendUserProfile(Long chatId, User user) {
        StringBuilder profileText = new StringBuilder();
        profileText.append("<b>Sizning profilingiz:</b>\n\n");
        profileText.append("Ism: ").append(user.getUserName()).append("\n");
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            profileText.append("Telefon raqami: ").append(user.getPhoneNumber()).append("\n");
        }
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            profileText.append("Bio: ").append(user.getBio()).append("\n");
        }
        profileText.append("Obunachilar soni: ").append(user.getFollowers().size()).append("\n");
        profileText.append("Obuna bo'lganlar soni: ").append(user.getFollowing().size()).append("\n");
        profileText.append("Qo'shgan retseptlar soni: ").append(user.getCreatedRecipes().size()).append("\n");

        SendMessage message = new SendMessage(String.valueOf(chatId), profileText.toString());
        message.enableHtml(true);
        telegramBot.executeMessage(message);
    }

    public void followUser(User follower, Long followingUserId) {
        Optional<User> followingUserOptional = userRepository.findById(followingUserId);
        if (followingUserOptional.isPresent()) {
            User followingUser = followingUserOptional.get();
            if (!follower.getFollowing().contains(followingUser)) {
                follower.getFollowing().add(followingUser);
                followingUser.getFollowers().add(follower);
                userRepository.save(follower);
                userRepository.save(followingUser);
            }
        }
    }

    public void sendNotifications(Long chatId, User user) {
        List<User> followingUsers = user.getFollowing();
        if (followingUsers.isEmpty()) {
            telegramBot.sendMessage(chatId, "Siz hech kimga obuna bo'lmagansiz. Obuna bo'lgan foydalanuvchilaringizdan yangi retseptlar haqida xabar olasiz.");
            return;
        }

        StringBuilder notificationText = new StringBuilder("<b>Yangi retseptlar haqida bildirishnomalar:</b>\n\n");
        AtomicBoolean hasNewNotifications = new AtomicBoolean(false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        for (User followedUser : followingUsers) {
            List<Recipe> newRecipes = recipeRepository.findByAuthorId(followedUser.getId());
            newRecipes.stream()
                    .filter(recipe -> recipe.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1))) // Oxirgi 24 soat ichidagilarni filterlaymiz
                    .forEach(recipe -> {
                        notificationText.append(String.format("📢 %s da Oshpaz <b>%s</b> yangi retsept tayyorladi: <b>%s</b>\n",
                                recipe.getCreatedAt().format(formatter),
                                followedUser.getUserName(),
                                recipe.getName()));
                        hasNewNotifications.set(true);
                    });
        }

        if (!hasNewNotifications.get()) {
            notificationText.append("Hozircha yangi bildirishnomalar yo'q. Yangi retseptlar qo'shilganda bu yerda ko'rishingiz mumkin.");
        }

        SendMessage message = new SendMessage(String.valueOf(chatId), notificationText.toString());
        message.enableHtml(true);
        telegramBot.executeMessage(message);
    }
}