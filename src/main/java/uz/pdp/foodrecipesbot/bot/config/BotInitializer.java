package uz.pdp.foodrecipesbot.bot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.pdp.foodrecipesbot.bot.tgBot.TelegramBot;
/*

@Slf4j
@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramBot telegramBot;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);

            log.info("Telegram bot successfully registered and started!");

        } catch (TelegramApiException e) {
            log.error("Error occurred while registering the bot: " + e.getMessage());
        }
    }
}*/
