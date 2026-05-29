package com.example.internship.services;

import com.example.internship.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
public class TelegramBotService extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final UserRepository userRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botUsername;

    public TelegramBotService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    void preparePolling() {
        try {
            execute(DeleteWebhook.builder().dropPendingUpdates(false).build());
            log.info("Telegram bot @{}: long polling ready (webhook cleared)", botUsername);
        } catch (TelegramApiException e) {
            log.warn("Telegram bot: could not delete webhook before polling: {}", e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (messageText.startsWith("/start")) {
            handleStartCommand(messageText, chatId);
        }
    }

    private void handleStartCommand(String messageText, Long chatId) {
        try {
            String payload = messageText.replaceFirst("/start\\s*", "").trim();

            if (payload.startsWith("user_")) {
                Long userId = Long.parseLong(payload.substring("user_".length()));

                userRepository.findById(userId).ifPresentOrElse(user -> {
                    user.setTelegramChatId(chatId);
                    userRepository.save(user);
                    sendNotification(chatId, "✅ Успешно! Теперь уведомления от INTERN.PRO будут приходить сюда.");
                }, () -> sendNotification(chatId, "❌ Пользователь не найден. Проверьте ссылку из профиля на сайте."));
            } else {
                sendNotification(chatId,
                        "Добро пожаловать! Чтобы подключить уведомления, перейдите по ссылке из вашего профиля на сайте.");
            }
        } catch (NumberFormatException e) {
            sendNotification(chatId, "❌ Неверный формат ссылки. Откройте бота через профиль на сайте INTERN.PRO.");
        } catch (Exception e) {
            log.error("Telegram /start handler failed", e);
            sendNotification(chatId, "Произошла ошибка при привязке аккаунта.");
        }
    }

    public void sendNotification(Long chatId, String text) {
        if (chatId == null || text == null || text.isBlank()) {
            return;
        }
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn("Telegram send failed for chatId={}: {}", chatId, e.getMessage());
        }
    }
}
