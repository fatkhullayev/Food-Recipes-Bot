package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications")
public class Notification extends BaseEntity {

    private String message;

    @ManyToOne
    private User user;

    @ManyToOne
    private User receiverUser;

    private Boolean isRead = false;

}
