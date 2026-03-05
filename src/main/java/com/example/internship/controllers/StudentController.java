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

        // 1. Проверяем, рекомендовал ли университет этого студента (есть ли статус VERIFIED)
        boolean isVerified = apps.stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.VERIFIED);

        // 2. Если студент рекомендован, загружаем вакансии от КОМПАНИЙ (где university == null)
        if (isVerified) {
            // Предполагаем, что у компаний поле university пустое
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
    public String apply(@PathVariable Long internshipId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Internship internship = internshipRepository.findById(internshipId)
                    .orElseThrow(() -> new RuntimeException("Стажировка не найдена"));
            User student = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Студент не найден"));

            if (applicationRepository.existsByStudentAndInternship(student, internship)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Вы уже откликнулись.");
                return "redirect:/";
            }

            Application application = new Application();
            application.setStudent(student);
            application.setInternship(internship);
            application.setAppliedAt(LocalDateTime.now());

            // ЛОГИКА ОСЫ ЖЕРДЕ:
            if (internship.getUniversity() != null) {
                // Егер бұл Университет бағдарламасы болса - БІРДЕН МАҚҰЛДАУ
                application.setStatus(ApplicationStatus.APPROVED);
                redirectAttributes.addFlashAttribute("successMessage", "Вы успешно записаны на обучение!");
            } else {
                // Егер бұл Компания болса - КҮТУ (PENDING)
                application.setStatus(ApplicationStatus.PENDING);
                redirectAttributes.addFlashAttribute("successMessage", "Отклик отправлен. Ожидайте одобрения компании.");
            }

            applicationRepository.save(application);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: " + e.getMessage());
        }
        return "redirect:/student/my-applications";
    }
    @GetMapping("/messages/{internshipId}")
    public String chatPage(@PathVariable Long internshipId, Model model, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Стажировка не найдена"));

        // ТЕКСЕРУ: Студент қабылданды ма?
        boolean isAccepted = applicationRepository.findByStudent(me).stream()
                .anyMatch(app -> app.getInternship().getId().equals(internshipId)
                        && (app.getStatus() == ApplicationStatus.ACCEPTED
                        || app.getStatus() == ApplicationStatus.APPROVED));

        if (!isAccepted) {
            return "redirect:/student/my-applications?error=not_accepted";
        }

        User companyUser = internship.getCompany().getUser();

        // Хабарламалар тарихын жүктеу
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

    // ОСТАВИЛИ ТОЛЬКО ОДИН МЕТОД SEND MESSAGE
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

    // 1. Показать страницу редактирования профиля
    @GetMapping("/profile/update")
    public String editProfilePage(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "student/profile"; // Путь к HTML-файлу
    }

    // 2. Обработать сохранение данных
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User updatedData,
                                @RequestParam(value = "resumeFile", required = false) MultipartFile file,
                                Principal principal) throws IOException {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        // Тексттік мәліметтерді жаңарту
        user.setFullName(updatedData.getFullName());
        user.setEmail(updatedData.getEmail());
        user.setResume(updatedData.getResume());

        // Файлды өңдеу (PDF/DOCX)
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Файл атының қайталанбауы үшін UUID қосамыз
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            user.setResumePath(fileName); // Дерекқорға файл атын сақтаймыз
        }

        userRepository.save(user);
        return "redirect:/student/profile?success";
    }

    @PostMapping("/student/complete-training/{appId}")
    public String completeTraining(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId).orElseThrow();

        // После прохождения теста меняем статус
        app.setStatus(ApplicationStatus.COMPLETED);
        applicationRepository.save(app);

        return "redirect:/student/my-applications?finished";
    }



    @GetMapping("/learning/{id}") // Проверь, чтобы путь в ссылке и тут совпадал
    public String showLearningPage(@PathVariable("id") Long id, Model model) {
        // 1. Извлекаем из базы
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        // 2. Печатаем в консоль для проверки (посмотри в терминал IDEA после клика)
        System.out.println("DEBUG: Загружаем страницу для заявки ID: " + application.getId());

        // 3. ПЕРЕДАЕМ В МОДЕЛЬ (Ключевой момент!)
        // В кавычках должно быть ПУЛЯ В ПУЛЮ как в HTML
        model.addAttribute("application", application);
        model.addAttribute("internship", application.getInternship());

        return "student/learning";
    }

    @PostMapping("/complete-learning/{appId}")
    public String completeLearning(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId).orElseThrow();

        // Меняем статус на COMPLETED
        app.setStatus(ApplicationStatus.COMPLETED);
        applicationRepository.save(app);

        // Перенаправляем в кабинет, где теперь будет доступна кнопка вакансий
        return "redirect:/student/my-applications?success=completed";
    }

    @PostMapping("/verify-student/{appId}")
    public String verifyStudent(@PathVariable Long appId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (app.getStatus() == ApplicationStatus.COMPLETED) {
            // Устанавливаем статус, который означает "Прошел проверку качества"
            app.setStatus(ApplicationStatus.VERIFIED);
            applicationRepository.save(app);
        }

        return "redirect:/university-admin/dashboard";
    }

    @GetMapping("/job-market")
    public String showJobMarket(Model model, Principal principal) {
        User student = userRepository.findByUsername(principal.getName()).orElseThrow();

        // Проверяем: рекомендовал ли хоть один ВУЗ этого студента?
        boolean isVerified = applicationRepository.existsByStudentIdAndStatus(student.getId(), ApplicationStatus.VERIFIED);

        if (!isVerified) {
            // Если не верифицирован — отправляем назад с предупреждением
            return "redirect:/student/my-applications?access=denied";
        }

        // Загружаем только вакансии КОМПАНИЙ (у которых поле university == null)
        List<Internship> companyJobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .collect(Collectors.toList());

        model.addAttribute("jobs", companyJobs);
        return "student/job-market";
    }
}