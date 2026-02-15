package com.example.internship.notification;

import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // ПРАВИЛЬНЫЙ ИМПОРТ
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramNotificationBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.name}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Autowired
    private UserRepository userRepository;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            // ЛОГ В КОНСОЛЬ: Мы увидим, что именно пришло от Телеграма
            System.out.println("СООБЩЕНИЕ ОТ ТГ: " + messageText + " | ChatID: " + chatId);

            if (messageText.startsWith("/start")) {
                // Если команда пришла с ID (например /start user_5)
                if (messageText.contains("user_")) {
                    try {
                        String userIdStr = messageText.split("user_")[1];
                        Long userId = Long.parseLong(userIdStr);

                        userRepository.findById(userId).ifPresent(user -> {
                            user.setTelegramChatId(chatId);
                            userRepository.save(user);

                            // Ответ пользователю в Телеграм
                            sendNotification(chatId, "✅ Успешно! Аккаунт " + user.getUsername() + " привязан.");
                            System.out.println("ПРИВЯЗКА УСПЕШНА: " + user.getUsername());
                        });
                    } catch (Exception e) {
                        sendNotification(chatId, "❌ Ошибка формата ID.");
                    }
                } else {
                    // Если просто нажали Старт без ссылки
                    sendNotification(chatId, "Привет! Пожалуйста, подключите уведомления через личный кабинет на сайте INTERN.PRO.");
                }
            }
        }
    }

    private void handleStartCommand(String messageText, Long chatId) {
        try {
            // Ожидаем формат /start user_123
            if (messageText.contains("user_")) {
                String payload = messageText.split("user_")[1];
                Long userId = Long.parseLong(payload);

                userRepository.findById(userId).ifPresent(user -> {
                    user.setTelegramChatId(chatId);
                    userRepository.save(user);
                });

                sendNotification(chatId, "✅ Уведомления INTERN.PRO подключены!");
            }
        } catch (Exception e) {
            sendNotification(chatId, "Ошибка при подключении уведомлений.");
        }
    }

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