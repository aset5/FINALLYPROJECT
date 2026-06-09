package com.example.internship.repositories;

import com.example.internship.models.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    @Query("SELECT p FROM LessonProgress p JOIN FETCH p.lesson WHERE p.application.id = :applicationId")
    List<LessonProgress> findByApplicationId(@Param("applicationId") Long applicationId);

    Optional<LessonProgress> findByApplicationIdAndLessonId(Long applicationId, Long lessonId);

    long countByApplicationId(Long applicationId);

    @Modifying
    @Query("DELETE FROM LessonProgress lp WHERE lp.application.student.id = :studentId")
    void deleteByApplication_Student_Id(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM LessonProgress lp WHERE lp.application.internship.id = :internshipId")
    void deleteByApplication_Internship_Id(@Param("internshipId") Long internshipId);
}
