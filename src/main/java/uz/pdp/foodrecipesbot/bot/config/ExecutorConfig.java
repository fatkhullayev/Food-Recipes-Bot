package uz.pdp.foodrecipesbot.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    static boolean executor = false;

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(14); // 30 o'rniga 4, chunki Telegram bot uchun yetarli
    }
}
