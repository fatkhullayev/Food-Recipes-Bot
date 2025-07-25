package uz.pdp.foodrecipesbot.bot.config;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.pdp.foodrecipebot.bot.handlers.CallbackHandler;
import uz.pdp.foodrecipebot.bot.handlers.MessageHandler;

import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
public class BotLauncher implements CommandLineRunner {

    private final TelegramBot bot;
    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;
    private final ExecutorService executorService;

    @Override
    public void run(String... args) {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (ExecutorConfig.executor) continue;
                executorService.execute(() -> {
                    if (update.message() != null) {
                        messageHandler.accept(update.message());

                    } else if (update.callbackQuery() != null) {
                        callbackHandler.accept(update.callbackQuery());

                    } else System.err.println("Unknown message -> " + update);
                });
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}
