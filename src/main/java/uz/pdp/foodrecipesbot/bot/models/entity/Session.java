package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.pdp.foodrecipesbot.bot.models.base.Auditable;
import uz.pdp.foodrecipesbot.bot.models.enums.SessionState;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "sessions")
public class Session extends Auditable {
    @Id
    protected Long userId;

    private Integer messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionState state;

    private Boolean _active;


    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

}
