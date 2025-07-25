package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class FoodIngredient extends BaseEntity {

    @ManyToOne
    private Ingredient ingredient;

    @ManyToOne
    private Food food;

    private Integer quantity;

}
