package uz.pdp.foodrecipesbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.foodrecipesbot.bot.models.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

}