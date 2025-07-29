package uz.pdp.foodrecipesbot.bot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.pdp.foodrecipesbot.bot.models.entity.Category;
import uz.pdp.foodrecipesbot.bot.repository.CategoryRepository;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<String> categories = Arrays.asList("Indian", "Italian", "Uzbek", "Chinese", "Mexican", "Japanese", "Desserts", "Salads");
            for (String catName : categories) {
                categoryRepository.save(Category.builder().name(catName).build());
            }
            System.out.println("Boshlang'ich kategoriyalar yaratildi.");
        }
    }
}