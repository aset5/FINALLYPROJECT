package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
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
    private final MessageRepository messageRepository; // Добавлено

    public StudentController(ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             InternshipRepository internshipRepository,
                             MessageRepository messageRepository) { // Обновлено
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.internshipRepository = internshipRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/my-applications")
    public String showMyApps(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Application> apps = applicationRepository.findByStudent(student);
        model.addAttribute("myApplications", apps);
        return "student/application";
    }

    @PostMapping("/apply/{internshipId}")
    public String apply(@PathVariable Long internshipId, Principal principal, RedirectAttributes redirectAttributes) {
        Internship internship = internshipRepository.findById(internshipId).orElse(null);
        if (internship == null) {
            return "redirect:/?error=not_found";
        }

        User student = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        if (applicationRepository.existsByStudentAndInternship(student, internship)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Вы уже откликались на эту вакансию!");
            return "redirect:/";
        }

        Application app = new Application();
        app.setStudent(student);
        app.setInternship(internship);
        app.setAppliedAt(LocalDateTime.now());

        applicationRepository.save(app);

        redirectAttributes.addFlashAttribute("successMessage", "Отклик успешно отправлен!");
        return "redirect:/?success";
    }

    // --- НОВЫЕ МЕТОДЫ ДЛЯ ЧАТА ---

    @GetMapping("/messages/{internshipId}")
    public String chatPage(@PathVariable Long internshipId, Model model, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

        // Получаем пользователя компании (кому будем писать)
        User companyUser = internship.getCompany().getUser();

        // Загружаем историю переписки
        List<Message> history = messageRepository.findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                internshipId, me.getId(), companyUser.getId(),
                internshipId, companyUser.getId(), me.getId()
        );

        model.addAttribute("history", history);
        model.addAttribute("companyUser", companyUser);
        model.addAttribute("internshipId", internshipId);
        return "student/chat"; // Создадим этот шаблон ниже
    }

    @PostMapping("/messages/send")
    public String sendMessage(@RequestParam Long internshipId,
                              @RequestParam Long receiverId,
                              @RequestParam String content,
                              Principal principal) {
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
        return "redirect:/student/messages/" + internshipId;
    }

    // --- ПРОФИЛЬ ---

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "student/profile";
    }

    @PostMapping("/profile/update")
    public String updateResume(@RequestParam("resume") String resume, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setResume(resume);
        userRepository.save(user);
        return "redirect:/student/profile?success";
    }
}