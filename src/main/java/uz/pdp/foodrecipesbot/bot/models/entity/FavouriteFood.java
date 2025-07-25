package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class FavouriteFood extends BaseEntity {

    private Long userId;

    private Long foodId;

}
