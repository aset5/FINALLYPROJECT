package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.TelegramBotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InternshipRepository internshipRepository;
    private final MessageRepository messageRepository;
    private final TelegramBotService telegramBotService;

    public StudentController(ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             InternshipRepository internshipRepository,
                             MessageRepository messageRepository,
                             TelegramBotService telegramBotService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.internshipRepository = internshipRepository;
        this.messageRepository = messageRepository;
        this.telegramBotService = telegramBotService;
    }

    @GetMapping("/my-applications")
    public String showMyApps(Model model, Principal principal) {
        User student = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Application> apps = applicationRepository.findByStudent(student);
        model.addAttribute("myApplications", apps);
        return "student/application";
    }

    @PostMapping("/apply/{internshipId}")
    public String apply(@PathVariable Long internshipId, Principal principal, RedirectAttributes redirectAttributes) {
        Internship internship = internshipRepository.findById(internshipId).orElse(null);
        User student = userRepository.findByUsername(principal.getName()).orElseThrow();

        if (applicationRepository.existsByStudentAndInternship(student, internship)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Вы уже откликались на эту вакансию!");
            return "redirect:/";
        }

        Application app = new Application();
        app.setStudent(student);
        app.setInternship(internship);
        app.setAppliedAt(LocalDateTime.now());
        applicationRepository.save(app);

        // Уведомление HR о новом отклике
        User hr = internship.getCompany().getUser();
        if (hr.getTelegramChatId() != null) {
            telegramBotService.sendNotification(hr.getTelegramChatId(),
                    "⚡️ Новый отклик на вашу вакансию \"" + internship.getTitle() + "\" от студента " + student.getUsername());
        }

        redirectAttributes.addFlashAttribute("successMessage", "Отклик успешно отправлен!");
        return "redirect:/?success";
    }

    @GetMapping("/messages/{internshipId}")
    public String chatPage(@PathVariable Long internshipId, Model model, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId).orElseThrow();
        User companyUser = internship.getCompany().getUser();

        List<Message> history = messageRepository.findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                internshipId, me.getId(), companyUser.getId(),
                internshipId, companyUser.getId(), me.getId()
        );

        model.addAttribute("history", history);
        model.addAttribute("companyUser", companyUser);
        model.addAttribute("internshipId", internshipId);
        return "student/chat";
    }

    @PostMapping("/messages/send")
    public String sendMessage(@RequestParam Long internshipId, @RequestParam Long receiverId, @RequestParam String content, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId).orElseThrow();

        Message msg = new Message();
        msg.setSender(me);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setInternship(internship);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        if (receiver.getTelegramChatId() != null) {
            telegramBotService.sendNotification(receiver.getTelegramChatId(),
                    "✉️ Студент " + me.getUsername() + " ответил по вакансии \"" + internship.getTitle() + "\":\n" + content);
        }

        return "redirect:/student/messages/" + internshipId;
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "student/profile";
    }
}