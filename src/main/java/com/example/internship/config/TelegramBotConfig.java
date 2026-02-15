package com.example.internship.config;

import com.example.internship.notification.TelegramNotificationBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramNotificationBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}