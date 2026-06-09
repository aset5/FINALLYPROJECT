package com.example.internship.support;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TestDataFactory {

    public static final String TEST_PASSWORD = "Test1234!";

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CompanyRepository companyRepository;
    private final InternshipRepository internshipRepository;
    private final ApplicationRepository applicationRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataFactory(UserRepository userRepository,
                           UniversityRepository universityRepository,
                           CompanyRepository companyRepository,
                           InternshipRepository internshipRepository,
                           ApplicationRepository applicationRepository,
                           QuizQuestionRepository quizQuestionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.universityRepository = universityRepository;
        this.companyRepository = companyRepository;
        this.internshipRepository = internshipRepository;
        this.applicationRepository = applicationRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User student(String username, University university) {
        User user = baseUser(username, Role.STUDENT);
        user.setUniversity(university);
        user.setFullName("Student " + username);
        return userRepository.save(user);
    }

    public User companyUser(String username) {
        return userRepository.save(baseUser(username, Role.COMPANY));
    }

    public User admin(String username) {
        User user = baseUser(username, Role.ADMIN);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public University university(String name) {
        University university = new University();
        university.setName(name);
        return universityRepository.save(university);
    }

    public Company company(User owner, String name, String bin) {
        Company company = new Company();
        company.setUser(owner);
        company.setName(name);
        company.setBin(bin);
        return companyRepository.save(company);
    }

    public Internship approvedUniversityProgram(University university, String title) {
        Internship internship = new Internship();
        internship.setUniversity(university);
        internship.setTitle(title);
        internship.setCity("Almaty");
        internship.setDescription("University program");
        internship.setStatus(InternshipStatus.APPROVED);
        internship.setMaxPlaces(30);
        return internshipRepository.save(internship);
    }

    public Internship approvedCompanyJob(Company company, String title) {
        Internship internship = new Internship();
        internship.setCompany(company);
        internship.setTitle(title);
        internship.setCity("Astana");
        internship.setDescription("Company vacancy");
        internship.setStatus(InternshipStatus.APPROVED);
        internship.setJob(true);
        return internshipRepository.save(internship);
    }

    public Application application(User student, Internship internship, ApplicationStatus status) {
        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setStatus(status);
        application.setAppliedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    public Application completedUniversityApplication(User student,
                                                      Internship program,
                                                      String certificateToken) {
        Application application = application(student, program, ApplicationStatus.COMPLETED);
        application.setCompletedAt(LocalDateTime.now());
        application.setFinalGradePercent(92);
        application.setGradeLetter("A");
        application.setCertificateToken(certificateToken);
        return applicationRepository.save(application);
    }

    public QuizQuestion quizQuestion(Internship internship, int correctIndex) {
        QuizQuestion question = new QuizQuestion();
        question.setInternship(internship);
        question.setSortOrder(1);
        question.setQuestionText("2 + 2 = ?");
        question.setOptionA("3");
        question.setOptionB("4");
        question.setOptionC("5");
        question.setOptionD("6");
        question.setCorrectIndex(correctIndex);
        return quizQuestionRepository.save(question);
    }

    private User baseUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        user.setEmail(username + "@test.example");
        return user;
    }
}
