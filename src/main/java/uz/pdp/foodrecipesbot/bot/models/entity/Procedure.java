package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "procedure")
public class Procedure extends BaseEntity {

    private String description;

    private Long foodId;

    private Short step;
}
