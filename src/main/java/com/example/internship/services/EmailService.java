package com.example.internship.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.from:${spring.mail.username:}}")
    private String mailFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Отправляет 6-значный код. Если SMTP не настроен — пишет код в консоль (для разработки).
     */
    public void sendVerificationCode(String to, String code) throws MessagingException {
        if (!isMailConfigured()) {
            System.out.println("=================================================");
            System.out.println("  КОД ПОДТВЕРЖДЕНИЯ EMAIL: " + code);
            System.out.println("  Получатель: " + to);
            System.out.println("  (Настройте spring.mail.* в application-local.properties)");
            System.out.println("=================================================");
            return;
        }

        String subject = "INTERN.PRO — код подтверждения email";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2 style="color: #4f46e5;">Подтверждение регистрации</h2>
                  <p>Ваш код для регистрации на платформе <strong>INTERN.PRO</strong>:</p>
                  <p style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1e1b4b;">%s</p>
                  <p style="color: #64748b; font-size: 14px;">Код действителен 15 минут. Никому не сообщайте этот код.</p>
                </div>
                """.formatted(code);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }

    private boolean isMailConfigured() {
        return mailUsername != null
                && !mailUsername.isBlank()
                && !mailUsername.contains("your-email");
    }
}
