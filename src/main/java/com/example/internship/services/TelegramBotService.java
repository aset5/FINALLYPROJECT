package com.example.internship.services;

import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botUsername;

    public TelegramBotService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        // ПРОВЕРКА: есть ли сообщение и текст
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            // Логика обработки команды /start с параметром
            if (messageText.startsWith("/start")) {
                handleStartCommand(messageText, chatId);
            }
        }
    }

    private void handleStartCommand(String messageText, Long chatId) {
        try {
            // Извлекаем "user_123"
            String payload = messageText.replace("/start ", "").trim();

            if (payload.startsWith("user_")) {
                Long userId = Long.parseLong(payload.replace("user_", ""));

                // Находим пользователя и сохраняем его Chat ID
                userRepository.findById(userId).ifPresent(user -> {
                    user.setTelegramChatId(chatId);
                    userRepository.save(user);
                });

                sendNotification(chatId, "✅ Успешно! Теперь уведомления от INTERN.PRO будут приходить сюда.");
            } else {
                sendNotification(chatId, "Добро пожаловать! Чтобы подключить уведомления, перейдите по ссылке из вашего профиля на сайте.");
            }
        } catch (Exception e) {
            sendNotification(chatId, "Произошла ошибка при привязке аккаунта.");
        }
    }

    // Универсальный метод для отправки сообщений
    public void sendNotification(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}