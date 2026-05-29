package com.example.internship.services;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Единая точка отправки уведомлений в Telegram.
 * Работает и когда бот отключён ({@code telegram.bot.enabled=false}).
 */
@Service
public class TelegramNotificationSender {

    private final ObjectProvider<TelegramBotService> telegramBotService;

    public TelegramNotificationSender(ObjectProvider<TelegramBotService> telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    public void sendNotification(Long chatId, String text) {
        telegramBotService.ifAvailable(bot -> bot.sendNotification(chatId, text));
    }
}
