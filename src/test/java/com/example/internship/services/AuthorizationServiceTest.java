package com.example.internship.services;

import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.ProgramLessonRepository;
import com.example.internship.repositories.ProgramMaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private InternshipRepository internshipRepository;
    @Mock
    private ProgramLessonRepository lessonRepository;
    @Mock
    private ProgramMaterialRepository materialRepository;
    @Mock
    private StudentAchievementService studentAchievementService;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(
                applicationRepository,
                internshipRepository,
                lessonRepository,
                materialRepository,
                studentAchievementService
        );
    }

    @Test
    void assertStudentCanApply_blocksUnverifiedStudentFromCompanyJob() {
        User student = student(1L);
        Internship job = companyJob(10L, InternshipStatus.APPROVED);

        when(internshipRepository.findById(10L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByStudentIdAndStatus(1L, ApplicationStatus.VERIFIED)).thenReturn(false);

        assertThatThrownBy(() -> authorizationService.assertStudentCanApply(student, job))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("верифицированным");
    }

    @Test
    void assertStudentCanApply_allowsVerifiedStudentForCompanyJob() {
        User student = student(1L);
        Internship job = companyJob(10L, InternshipStatus.APPROVED);

        when(internshipRepository.findById(10L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByStudentIdAndStatus(1L, ApplicationStatus.VERIFIED)).thenReturn(true);

        authorizationService.assertStudentCanApply(student, job);
    }

    @Test
    void assertSingleUniversityEnrollment_blocksSecondProgram() {
        User student = student(1L);
        University university = university(5L);
        Internship activeProgram = universityProgram(100L, university);
        Internship newProgram = universityProgram(200L, university);

        Application active = new Application();
        active.setInternship(activeProgram);
        active.setStatus(ApplicationStatus.IN_PROGRESS);

        when(applicationRepository.findByStudent(student)).thenReturn(List.of(active));

        assertThatThrownBy(() -> authorizationService.assertSingleUniversityEnrollment(student, newProgram.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("одной программе");
    }

    @Test
    void assertStudentCanTakeInternshipSlot_blocksThirdAcceptedInternship() {
        User student = student(1L);
        when(applicationRepository.countByStudentIdAndCompanyInternshipAndStatus(
                1L, ApplicationStatus.ACCEPTED)).thenReturn(2L);

        assertThat(authorizationService.canTakeMoreCompanyInternships(student)).isFalse();
        assertThatThrownBy(() -> authorizationService.assertStudentCanTakeInternshipSlot(student))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("2");
    }

    @Test
    void requireOpenInternship_rejectsNonApprovedVacancy() {
        Internship pending = companyJob(10L, InternshipStatus.PENDING);
        when(internshipRepository.findById(10L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authorizationService.requireOpenInternship(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("не одобрена");
    }

    private static User student(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.STUDENT);
        return user;
    }

    private static University university(Long id) {
        University university = new University();
        university.setId(id);
        university.setName("Test University");
        return university;
    }

    private static Internship companyJob(Long id, InternshipStatus status) {
        Internship internship = new Internship();
        internship.setId(id);
        internship.setCompany(new Company());
        internship.setStatus(status);
        return internship;
    }

    private static Internship universityProgram(Long id, University university) {
        Internship internship = new Internship();
        internship.setId(id);
        internship.setUniversity(university);
        internship.setStatus(InternshipStatus.APPROVED);
        return internship;
    }
}
