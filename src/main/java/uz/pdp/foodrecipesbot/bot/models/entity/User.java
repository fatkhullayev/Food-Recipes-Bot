package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.pdp.foodrecipesbot.bot.models.base.Auditable;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bot_users")
public class User extends Auditable {
    @Id
    protected Long id;

    private Boolean _active;

    @Column(name = "full_name")
    private String fullName;

    private String username;

    private String bio;

}


