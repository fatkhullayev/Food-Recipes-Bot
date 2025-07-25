package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;

@Entity
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Table(name = "follows")
    public class Follow extends BaseEntity {

        @ManyToOne
        private User follower;

        @ManyToOne
        private User user;

    }
