package uz.pdp.foodrecipesbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.foodrecipesbot.bot.models.entity.Comment;
import uz.pdp.foodrecipesbot.bot.models.entity.Recipe;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRecipe(Recipe recipe);
}