package com.example.internship.repositories;

import com.example.internship.models.Application;
import com.example.internship.models.ApplicationStatus;
import com.example.internship.models.Internship;
import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByInternshipCompanyId(Long companyId);

    List<Application> findByStudent(User student);

    void deleteByStudent(User student);

    void deleteByInternship(Internship internship);

    void deleteByInternshipId(Long internshipId);

    boolean existsByStudentAndInternship(User student, Internship internship);

    long countByInternship(Internship internship);

    boolean existsByStudentAndStatus(User student, ApplicationStatus status);

    boolean existsByStudentIdAndStatus(Long studentId, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.student s
            LEFT JOIN FETCH s.university
            JOIN FETCH a.internship i
            LEFT JOIN FETCH i.university
            LEFT JOIN FETCH i.company
            WHERE a.id = :id
            """)
    Optional<Application> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.student s
            LEFT JOIN FETCH s.university
            JOIN FETCH a.internship i
            LEFT JOIN FETCH i.university
            LEFT JOIN FETCH i.company
            WHERE a.certificateToken = :token
            """)
    Optional<Application> findByCertificateTokenWithDetails(@Param("token") String token);

    @Query("""
            SELECT COUNT(a) FROM Application a
            JOIN a.internship i
            WHERE a.student.id = :studentId
            AND i.company IS NOT NULL
            AND a.status = :status
            """)
    long countByStudentIdAndCompanyInternshipAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") ApplicationStatus status);
}
