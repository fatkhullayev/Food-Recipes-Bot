package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.pdp.foodrecipesbot.bot.models.base.BaseEntity;

@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Table(name = "comments")
public class Comment extends BaseEntity {

    @ManyToOne
    private Food food;

    @Column(columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Boolean isActive = true;

//    @OneToOne(mappedBy = "comment", cascade = CascadeType.ALL)
//    private CommentReaction commentReaction;

}
