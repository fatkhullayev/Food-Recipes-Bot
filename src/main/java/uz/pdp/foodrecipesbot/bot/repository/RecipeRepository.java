package uz.pdp.foodrecipesbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;
import uz.pdp.foodrecipesbot.bot.models.entity.Recipe;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCategory(Category category);

    List<Recipe> findByAuthorId(Long authorId);
}