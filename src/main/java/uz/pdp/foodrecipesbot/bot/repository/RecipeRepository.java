package uz.pdp.foodrecipesbot.bot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;
import uz.pdp.foodrecipesbot.bot.models.entity.Recipe;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByCategory(Category category);

    List<Recipe> findByAuthorId(Long authorId);

    @Query("SELECT r FROM Recipe r WHERE r.category = :category ORDER BY r.id")
    Page<Recipe> findPageByCategory(@Param("category") Category category, Pageable pageable);


    @Query("SELECT r FROM Recipe r WHERE r.category = :category ORDER BY r.id")
    Page<Recipe> findByCategory(@Param("category") Category category, Pageable pageable);

    @Query("SELECT r FROM Recipe r ORDER BY r.id")
    Page<Recipe> findAllRecipes(Pageable pageable);
}