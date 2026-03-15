package com.example.internship.services;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String code) throws MessagingException {
        String subject = "Тіркелуді растау";
        // Мына сілтеме localhost-та жұмыс істейді
        String verifyURL = "http://localhost:8080/verify?code=" + code;

        String content = "<h3>Құрметті пайдаланушы!</h3>"
                + "<p>Тіркелуді аяқтау үшін төмендегі сілтемені басыңыз:</p>"
                + "<a href=\"" + verifyURL + "\">ПОШТАНЫ РАСТАУ</a>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }
}