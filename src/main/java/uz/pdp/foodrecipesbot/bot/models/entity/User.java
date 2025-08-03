package uz.pdp.foodrecipesbot.bot.models.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.foodrecipesbot.bot.models.enums.BotState;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long telegramId;

    private String userName;
    private String phoneNumber;
    private String bio;

    @Enumerated(EnumType.STRING)
    private BotState botState;

    private Long currentRecipeId;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Recipe> createdRecipes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_saved_recipes",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "recipe_id")
    )
    private List<Recipe> savedRecipes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_followers",
            joinColumns = @JoinColumn(name = "following_id"),
            inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    private List<User> followers = new ArrayList<>();
    @ManyToMany(mappedBy = "followers", fetch = FetchType.EAGER)
    private List<User> following = new ArrayList<>();
}