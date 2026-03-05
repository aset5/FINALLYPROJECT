package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.TelegramBotService;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/company")
public class CompanyController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final MessageRepository messageRepository;
    private final TelegramBotService telegramBotService;

    public CompanyController(InternshipRepository internshipRepository,
                             UserRepository userRepository,
                             ApplicationRepository applicationRepository,
                             CompanyRepository companyRepository,
                             MessageRepository messageRepository,
                             TelegramBotService telegramBotService) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.messageRepository = messageRepository;
        this.telegramBotService = telegramBotService;
    }

    @GetMapping("/dashboard")
    public String companyDashboard(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ДОБАВЬ ЭТУ СТРОКУ, чтобы кнопка Telegram работала
        model.addAttribute("user", user);

        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) {
            model.addAttribute("myInternships", Collections.emptyList());
            model.addAttribute("candidates", Collections.emptyList());
            return "company/dashboard";
        }

        model.addAttribute("myInternships", internshipRepository.findByCompanyId(company.getId()));
        model.addAttribute("candidates", applicationRepository.findByInternshipCompanyId(company.getId()));

        return "company/dashboard";
    }

    @PostMapping("/add")
    public String addInternship(Internship internship, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) return "redirect:/company/setup-profile?error=no_company";

        internship.setCompany(company);
        internship.setStatus(InternshipStatus.PENDING);
        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @Transactional
    @PostMapping("/delete/{id}")
    public String deleteInternship(@PathVariable Long id) {
        applicationRepository.deleteByInternshipId(id);
        internshipRepository.deleteById(id);
        return "redirect:/company/dashboard";
    }

    @GetMapping("/setup-profile")
    public String setupProfilePage(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Company existingCompany = companyRepository.findByUserId(user.getId());

        model.addAttribute("company", existingCompany != null ? existingCompany : new Company());
        return "company/setup-profile";
    }

    @PostMapping("/setup-profile")
    public String saveProfile(@ModelAttribute("company") Company company, Principal principal) {
        System.out.println("Начинаю сохранение профиля для: " + principal.getName());

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        company.setUser(user);
        companyRepository.save(company);

        System.out.println("Профиль сохранен. Делаю редирект на дашборд...");
        return "redirect:/company/dashboard";
    }

    @PostMapping("/applications/{id}/accept")
    public String acceptApplication(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();

        // Статусты бірізділікке келтіреміз (мысалы, ACCEPTED)
        app.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.save(app);

        // Студентке чат ашылғаны туралы хабарлама жіберу
        if (app.getStudent().getTelegramChatId() != null) {
            telegramBotService.sendNotification(app.getStudent().getTelegramChatId(),
                    "✅ Компания сіздің өтініміңізді қабылдады! Енді чат арқылы сөйлесе аласыз.");
        }
        return "redirect:/company/dashboard";
    }

    @PostMapping("/applications/{id}/reject")
    public String rejectApplication(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();
        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
        return "redirect:/company/dashboard";
    }

    @GetMapping("/messages/{internshipId}/{studentId}")
    public String chatPage(@PathVariable Long internshipId, @PathVariable Long studentId, Model model, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        User student = userRepository.findById(studentId).orElseThrow();

        List<Message> history = messageRepository.findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                internshipId, me.getId(), student.getId(),
                internshipId, student.getId(), me.getId()
        );

        model.addAttribute("history", history);
        model.addAttribute("student", student);
        model.addAttribute("internshipId", internshipId);
        return "company/chat";
    }

    @PostMapping("/messages/send")
    public String sendMessage(@RequestParam Long internshipId,
                              @RequestParam Long receiverId,
                              @RequestParam String content,
                              Principal principal) {

        // 1. Загружаем данные из базы
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId).orElseThrow();

        // 2. Создаем и сохраняем сообщение в БД
        Message msg = new Message();
        msg.setSender(me);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setInternship(internship);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        // 3. ОТЛАДКА: Выводим статус в консоль IDEA
        System.out.println("=== DEBUG TELEGRAM NOTIFICATION ===");
        System.out.println("Отправитель: " + me.getUsername());
        System.out.println("Получатель: " + receiver.getUsername());
        System.out.println("Telegram Chat ID получателя: " + receiver.getTelegramChatId());

        // 4. Проверка и отправка уведомления
        if (receiver.getTelegramChatId() != null) {
            try {
                telegramBotService.sendNotification(receiver.getTelegramChatId(),
                        "📩 Новое сообщение от компании " + internship.getCompany().getName() +
                                " по вакансии \"" + internship.getTitle() + "\":\n\n" + content);

                System.out.println("РЕЗУЛЬТАТ: Попытка отправить сообщение в Telegram успешна.");
            } catch (Exception e) {
                System.out.println("ОШИБКА: Не удалось отправить сообщение в Telegram: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("РЕЗУЛЬТАТ: Уведомление не отправлено, так как у пользователя " +
                    receiver.getUsername() + " поле telegramChatId = null.");
        }
        System.out.println("====================================");

        return "redirect:/company/messages/" + internshipId + "/" + receiverId;
    }

    @GetMapping("/student-profile/{id}")
    public String viewStudentProfile(@PathVariable Long id, Model model) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));
        model.addAttribute("student", student);
        return "company/student-view"; // Проблема может быть здесь
    }

    @PostMapping("/internships/{id}/close")
    public String closeInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.CLOSED); // Убедись, что в Enum есть CLOSED
        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @PostMapping("/internships/{id}/reopen")
    public String reopenInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.APPROVED); // Возвращаем в активные
        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @GetMapping("/internships/edit/{id}")
    public String editInternshipPage(@PathVariable Long id, Model model) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));
        model.addAttribute("internship", internship);
        return "company/edit-internship"; // Убедись, что такой HTML-файл есть
    }

    // 2. Сохранить изменения
    @PostMapping("/internships/edit/{id}")
    public String updateInternship(@PathVariable Long id, @ModelAttribute Internship updatedInternship) {
        Internship existing = internshipRepository.findById(id).orElseThrow();

        existing.setTitle(updatedInternship.getTitle());
        existing.setCity(updatedInternship.getCity());
        existing.setDescription(updatedInternship.getDescription());

        // После редактирования вакансия обычно уходит на повторную модерацию
        existing.setStatus(InternshipStatus.PENDING);

        internshipRepository.save(existing);
        return "redirect:/company/dashboard?msg=remoderation";
    }

    @GetMapping("/download/resume/{studentId}")
    @ResponseBody
    public ResponseEntity<Resource> downloadResume(@PathVariable Long studentId) {
        User student = userRepository.findById(studentId).orElseThrow();
        if (student.getResumePath() == null) return ResponseEntity.notFound().build();

        try {
            Path filePath = Paths.get("uploads/resumes/").resolve(student.getResumePath());
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + student.getResumePath() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/student/{id}")
    public String viewStudent(@PathVariable Long id, Model model) {
        User student = userRepository.findById(id).orElseThrow();
        model.addAttribute("student", student); // Передаем объект как "student"
        return "company/student-view";
    }

    @PostMapping("/company/application/approve/{id}")
    public String approveByCompany(@PathVariable Long id, RedirectAttributes ra) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Өтінім табылмады"));

        // Компания мақұлдаған кезде ғана статус өзгереді
// ACCEPTED орнына APPROVED қолданып көр
        app.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(app);

        ra.addFlashAttribute("successMessage", "Студент сәтті қабылданды!");
        return "redirect:/company/dashboard"; // Компанияның басты бетіне қайту
    }
}