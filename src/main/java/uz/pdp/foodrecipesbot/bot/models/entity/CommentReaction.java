package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;
import uz.pdp.foodrecipesbot.bot.models.enums.ReactionStatus;

@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Table(name = "comment_reactions")
public class CommentReaction extends BaseEntity {

    @ManyToOne
    private User user;

    @ManyToOne
    private Comment comment;

    @Enumerated(value = EnumType.STRING)
    private ReactionStatus reactionStatus;
}
