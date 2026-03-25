package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.TelegramBotService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/student")
public class StudentController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InternshipRepository internshipRepository;
    private final MessageRepository messageRepository;
    private final TelegramBotService telegramBotService;
    private final String UPLOAD_DIR = "uploads/resumes/";

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

        boolean isVerified = apps.stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.VERIFIED);

        if (isVerified) {
            List<Internship> companyJobs = internshipRepository.findAll().stream()
                    .filter(i -> i.getCompany() != null)
                    .collect(Collectors.toList());
            model.addAttribute("companyJobs", companyJobs);
        }
        model.addAttribute("user", student);
        model.addAttribute("isVerified", isVerified);
        model.addAttribute("myApplications", apps);
        model.addAttribute("applications", apps);

        return "student/application";
    }
    @PostMapping("/apply/{internshipId}")
    @Transactional
    public String apply(@PathVariable Long internshipId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Internship internship = internshipRepository.findById(internshipId)
                    .orElseThrow(() -> new RuntimeException("Стажировка не найдена"));
            User student = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Студент не найден"));

            if (applicationRepository.existsByStudentAndInternship(student, internship)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Вы уже откликнулись на эту позицию.");
                return "redirect:/student/my-applications";
            }


            if (internship.getJoinedCount() >= internship.getMaxPlaces()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Кешіріңіз, бос орын таусылды!");
                return "redirect:/student/my-applications";
            }

            Application application = new Application();
            application.setStudent(student);
            application.setInternship(internship);
            application.setAppliedAt(LocalDateTime.now());

            if (internship.getUniversity() != null) {
                application.setStatus(ApplicationStatus.APPROVED);

                internship.setJoinedCount(internship.getJoinedCount() + 1);
                internshipRepository.save(internship);

                redirectAttributes.addFlashAttribute("successMessage", "Вы успешно записаны на обучение!");
            } else {
                application.setStatus(ApplicationStatus.PENDING);
                redirectAttributes.addFlashAttribute("successMessage", "Отклик отправлен. Ожидайте одобрения компании.");
            }

            applicationRepository.save(application);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при подаче заявки: " + e.getMessage());
        }

        return "redirect:/student/my-applications";
    }
    @GetMapping("/messages/{internshipId}")
    public String chatPage(@PathVariable Long internshipId, Model model, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Стажировка не найдена"));

        boolean isAccepted = applicationRepository.findByStudent(me).stream()
                .anyMatch(app -> app.getInternship().getId().equals(internshipId)
                        && (app.getStatus() == ApplicationStatus.ACCEPTED
                        || app.getStatus() == ApplicationStatus.APPROVED));

        if (!isAccepted) {
            return "redirect:/student/my-applications?error=not_accepted";
        }

        User companyUser = internship.getCompany().getUser();

        List<Message> history = messageRepository.findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                internshipId, me.getId(), companyUser.getId(),
                internshipId, companyUser.getId(), me.getId()
        );

        model.addAttribute("history", history);
        model.addAttribute("companyUser", companyUser);
        model.addAttribute("internshipId", internshipId);
        model.addAttribute("internshipTitle", internship.getTitle()); // Чат тақырыбы үшін
        model.addAttribute("currentUsername", me.getUsername()); // Хабарламаларды оң/солға бөлу үшін

        return "student/chat";
    }

    @PostMapping("/messages/send")
    public String sendMessageFromStudent(@RequestParam Long internshipId,
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

        if (receiver.getTelegramChatId() != null) {
            String notifyText = "🎓 Новый вопрос от студента " + me.getUsername() +
                    " по вакансии \"" + internship.getTitle() + "\":\n\n" + content;

            telegramBotService.sendNotification(receiver.getTelegramChatId(), notifyText);
        }

        return "redirect:/student/messages/" + internshipId;
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "student/profile";
    }

    @GetMapping("/profile/update")
    public String editProfilePage(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "student/profile"; // Путь к HTML-файлу
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User updatedData,
                                @RequestParam(value = "resumeFile", required = false) MultipartFile file,
                                Principal principal) throws IOException {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        user.setFullName(updatedData.getFullName());
        user.setEmail(updatedData.getEmail());
        user.setResume(updatedData.getResume());

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            user.setResumePath(fileName);
        }

        userRepository.save(user);
        return "redirect:/student/profile?success";
    }

    @PostMapping("/student/complete-training/{appId}")
    public String completeTraining(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId).orElseThrow();

        app.setStatus(ApplicationStatus.COMPLETED);
        applicationRepository.save(app);

        return "redirect:/student/my-applications?finished";
    }



    @GetMapping("/learning/{id}")
    public String showLearningPage(@PathVariable("id") Long id, Model model) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        System.out.println("DEBUG: Загружаем страницу для заявки ID: " + application.getId());

        model.addAttribute("application", application);
        model.addAttribute("internship", application.getInternship());

        return "student/learning";
    }

    @PostMapping("/complete-learning/{appId}")
    public String completeLearning(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId).orElseThrow();

        app.setStatus(ApplicationStatus.COMPLETED);
        applicationRepository.save(app);

        return "redirect:/student/my-applications?success=completed";
    }

    @PostMapping("/verify-student/{appId}")
    public String verifyStudent(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (app.getStatus() == ApplicationStatus.COMPLETED) {
            app.setStatus(ApplicationStatus.VERIFIED);
            applicationRepository.save(app);
        }

        return "redirect:/university-admin/dashboard";
    }

    @GetMapping("/job-market")
    public String showJobMarket(Model model, Principal principal) {
        User student = userRepository.findByUsername(principal.getName()).orElseThrow();

        boolean isVerified = applicationRepository.existsByStudentIdAndStatus(student.getId(), ApplicationStatus.VERIFIED);

        if (!isVerified) {
            return "redirect:/student/my-applications?access=denied";
        }

        List<Internship> companyJobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .collect(Collectors.toList());

        model.addAttribute("jobs", companyJobs);
        return "student/job-market";
    }
}