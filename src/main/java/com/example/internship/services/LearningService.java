package com.example.internship.services;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningService {

    public static final int QUIZ_PASS_PERCENT = 70;
    public static final int MIN_PASS_GRADE = 70;
    public static final int MODULE_WEIGHT = 40;
    public static final int QUIZ_WEIGHT = 60;
    public static final int PARTIAL_MODULE_SCORE = 50;
    public static final int PERFECT_MODULE_SCORE = 100;

    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;
    private final ProgramLessonRepository lessonRepository;
    private final ProgramMaterialRepository materialRepository;
    private final LessonProgressRepository progressRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CertificateNumberService certificateNumberService;

    public LearningService(ApplicationRepository applicationRepository,
                           InternshipRepository internshipRepository,
                           ProgramLessonRepository lessonRepository,
                           ProgramMaterialRepository materialRepository,
                           LessonProgressRepository progressRepository,
                           QuizQuestionRepository quizQuestionRepository,
                           CertificateNumberService certificateNumberService) {
        this.applicationRepository = applicationRepository;
        this.internshipRepository = internshipRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
        this.progressRepository = progressRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.certificateNumberService = certificateNumberService;
    }

    @Transactional(readOnly = true)
    public Application requireStudentApplication(Long applicationId, String username) {
        Application app = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!app.getStudent().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Internship internship = app.getInternship();
        if (internship.getUniversity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не программа обучения");
        }
        if (internship.getCompany() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Обучение доступно только для программ ВУЗа");
        }
        return app;
    }

    public void assertAdminOwnsInternship(Internship internship, User admin) {
        if (admin.getUniversity() == null
                || internship.getUniversity() == null
                || !internship.getUniversity().getId().equals(admin.getUniversity().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public Internship loadInternshipForAdmin(Long internshipId, User admin) {
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertAdminOwnsInternship(internship, admin);
        return internship;
    }

    @Transactional
    public void requireReModeration(Internship internship) {
        if (internship.getStatus() != InternshipStatus.PENDING) {
            internship.setStatus(InternshipStatus.PENDING);
            internshipRepository.save(internship);
        }
    }

    @Transactional(readOnly = true)
    public LearningDetailResponse getLearningDetail(Application app) {
        return buildLearningDetail(app, false);
    }

    @Transactional(readOnly = true)
    public LearningDetailResponse getLearningDetailForAdmin(Application app) {
        return buildLearningDetail(app, true);
    }

    private LearningDetailResponse buildLearningDetail(Application app, boolean forAdmin) {
        Long internshipId = app.getInternship().getId();
        List<ProgramLesson> lessons = lessonRepository.findByInternshipIdOrderBySortOrderAscIdAsc(internshipId);
        Map<Long, LessonProgress> progressByLesson = progressRepository.findByApplicationId(app.getId()).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), Function.identity()));

        List<ProgramLessonResponse> lessonResponses = lessons.stream()
                .map(l -> ProgramLessonResponse.from(l, progressByLesson.get(l.getId()), forAdmin))
                .toList();

        List<ProgramMaterialResponse> materials = materialRepository
                .findByInternshipIdOrderBySortOrderAscIdAsc(internshipId)
                .stream()
                .map(ProgramMaterialResponse::from)
                .toList();

        List<QuizQuestion> quizQuestions = quizQuestionRepository
                .findByInternshipIdOrderBySortOrderAscIdAsc(internshipId);
        List<QuizQuestionResponse> quizResponses = quizQuestions.stream()
                .map(QuizQuestionResponse::forStudent)
                .toList();

        int completedCount = (int) lessonResponses.stream().filter(ProgramLessonResponse::completed).count();
        int progressPercent = computeProgressPercent(lessons.size(), completedCount);
        boolean quizPassed = Boolean.TRUE.equals(app.getQuizPassed());
        boolean hasQuiz = !quizQuestions.isEmpty();

        List<LessonProgress> progresses = new ArrayList<>(progressByLesson.values());
        GradeInfoResponse grades = computeGrades(app, progresses, hasQuiz);
        boolean canComplete = canComplete(app, lessons.size(), completedCount, hasQuiz, quizPassed, grades);

        UserResponse universityContact = null;
        User uniUser = app.getInternship().getUniversity().getUser();
        if (uniUser != null) {
            universityContact = UserResponse.from(uniUser);
        }

        return new LearningDetailResponse(
                ApplicationResponse.from(app),
                InternshipResponse.from(app.getInternship()),
                lessonResponses,
                materials,
                quizResponses,
                progressPercent,
                quizPassed,
                app.getQuizScorePercent(),
                canComplete,
                QUIZ_PASS_PERCENT,
                hasQuiz,
                universityContact,
                grades
        );
    }

    public GradeInfoResponse computeGrades(Application app, List<LessonProgress> progresses, boolean hasQuiz) {
        int moduleAverage = computeModuleAverage(progresses);
        Integer finalTest = app.getQuizScorePercent();
        int overall = computeOverallGrade(moduleAverage, finalTest, hasQuiz);
        String letter = letterGrade(overall);
        boolean gradeOk = overall >= MIN_PASS_GRADE;
        return new GradeInfoResponse(
                moduleAverage,
                finalTest,
                overall,
                letter,
                MODULE_WEIGHT,
                QUIZ_WEIGHT,
                MIN_PASS_GRADE,
                gradeOk
        );
    }

    public static int computeModuleAverage(List<LessonProgress> progresses) {
        if (progresses.isEmpty()) {
            return 0;
        }
        double avg = progresses.stream().mapToInt(LessonProgress::getScorePercent).average().orElse(0);
        return (int) Math.round(avg);
    }

    public static int computeOverallGrade(int moduleAverage, Integer quizScore, boolean hasQuiz) {
        if (!hasQuiz) {
            return moduleAverage;
        }
        int quiz = quizScore != null ? quizScore : 0;
        return (int) Math.round(moduleAverage * (MODULE_WEIGHT / 100.0) + quiz * (QUIZ_WEIGHT / 100.0));
    }

    public static String letterGrade(int percent) {
        if (percent >= 90) {
            return "Отлично";
        }
        if (percent >= 80) {
            return "Хорошо";
        }
        if (percent >= 70) {
            return "Удовлетворительно";
        }
        return "Недостаточно";
    }

    private int computeProgressPercent(int totalLessons, int completedLessons) {
        if (totalLessons == 0) {
            return 0;
        }
        return (int) Math.round((completedLessons * 100.0) / totalLessons);
    }

    private boolean allLessonsCompleted(int lessonCount, int completedCount) {
        return lessonCount > 0 && completedCount >= lessonCount;
    }

    private boolean canComplete(Application app,
                                int lessonCount,
                                int completedCount,
                                boolean hasQuiz,
                                boolean quizPassed,
                                GradeInfoResponse grades) {
        if (app.getStatus() == ApplicationStatus.COMPLETED || app.getStatus() == ApplicationStatus.VERIFIED) {
            return false;
        }
        boolean lessonsDone = allLessonsCompleted(lessonCount, completedCount);
        boolean quizOk = !hasQuiz || quizPassed;
        return lessonsDone && quizOk && grades.gradeRequirementMet();
    }

    @Transactional
    public Map<String, Object> completeLesson(Application app, Long lessonId, Integer answerIndex) {
        ProgramLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!lesson.getInternship().getId().equals(app.getInternship().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Optional<LessonProgress> existing = progressRepository.findByApplicationIdAndLessonId(app.getId(), lessonId);
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Модуль уже пройден");
        }

        int score;
        boolean correct;
        if (lesson.hasCheckQuestion()) {
            if (answerIndex == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ответьте на контрольный вопрос модуля");
            }
            if (answerIndex < 0 || answerIndex > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный ответ");
            }
            correct = answerIndex.equals(lesson.getCheckCorrectIndex());
            score = correct ? PERFECT_MODULE_SCORE : PARTIAL_MODULE_SCORE;
        } else {
            correct = true;
            score = PERFECT_MODULE_SCORE;
        }

        LessonProgress progress = new LessonProgress();
        progress.setApplication(app);
        progress.setLesson(lesson);
        progress.setScorePercent(score);
        progress.setAttempts(1);
        progressRepository.save(progress);

        LearningDetailResponse detail = getLearningDetail(app);
        return Map.of(
                "correct", correct,
                "moduleScore", score,
                "message", correct
                        ? "Верно! Модуль засчитан на " + score + "%"
                        : "Ответ неверный. Модуль засчитан на " + score + "% — итоговая оценка будет ниже",
                "detail", detail
        );
    }

    @Transactional
    public Map<String, Object> submitQuiz(Application app, Map<String, Integer> answers) {
        if (app.getQuizScorePercent() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Тест уже сдан. Повторная попытка невозможна.");
        }

        List<QuizQuestion> questions = quizQuestionRepository
                .findByInternshipIdOrderBySortOrderAscIdAsc(app.getInternship().getId());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тест не настроен");
        }

        int correct = 0;
        for (QuizQuestion q : questions) {
            Integer chosen = answers.get(String.valueOf(q.getId()));
            if (chosen == null) {
                chosen = answers.get(q.getId().toString());
            }
            if (chosen != null && chosen == q.getCorrectIndex()) {
                correct++;
            }
        }

        int score = (int) Math.round((correct * 100.0) / questions.size());
        boolean passed = score >= QUIZ_PASS_PERCENT;
        app.setQuizScorePercent(score);
        app.setQuizPassed(passed);
        applicationRepository.save(app);

        LearningDetailResponse detail = getLearningDetail(app);
        return Map.of(
                "scorePercent", score,
                "passed", passed,
                "requiredPercent", QUIZ_PASS_PERCENT,
                "detail", detail
        );
    }

    @Transactional
    public ApplicationResponse completeProgram(Application app) {
        LearningDetailResponse detail = getLearningDetail(app);
        if (!detail.canComplete()) {
            int total = detail.lessons().size();
            long done = detail.lessons().stream().filter(ProgramLessonResponse::completed).count();
            if (total == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Программа ещё не содержит модулей — дождитесь материалов от ВУЗа");
            }
            if (done < total) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Завершите все модули (" + done + " из " + total + ")");
            }
            if (detail.hasQuiz() && !detail.quizPassed()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Пройдите итоговый тест (нужно не менее " + QUIZ_PASS_PERCENT + "%)");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Итоговая оценка " + detail.grades().overallGradePercent()
                            + "% — нужно не менее " + MIN_PASS_GRADE + "%");
        }
        app.setStatus(ApplicationStatus.COMPLETED);
        app.setCompletedAt(LocalDateTime.now());
        app.setFinalGradePercent(detail.grades().overallGradePercent());
        app.setGradeLetter(detail.grades().gradeLetter());
        certificateNumberService.assignCertificateToken(app);
        applicationRepository.save(app);
        return ApplicationResponse.from(app);
    }

    public User universityContact(Internship internship) {
        if (internship.getUniversity() == null || internship.getUniversity().getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Куратор ВУЗа не назначен");
        }
        return internship.getUniversity().getUser();
    }
}
