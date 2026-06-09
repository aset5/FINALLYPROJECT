package com.example.internship.repositories;

import com.example.internship.models.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByInternshipIdOrderBySortOrderAscIdAsc(Long internshipId);

    void deleteByInternshipId(Long internshipId);
}
