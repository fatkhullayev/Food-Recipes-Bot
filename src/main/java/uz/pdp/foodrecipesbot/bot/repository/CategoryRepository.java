package uz.pdp.foodrecipesbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

}
