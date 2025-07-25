package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
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
public class Food extends BaseEntity {

    private String name;

    private Short cookingTime;

    private Float rating;

    private Integer viewAmount;

   /* @ManyToOne
    private Attachment attachment;*/

    @ManyToOne
    private Category category;

    @ManyToOne
    User user;

}
