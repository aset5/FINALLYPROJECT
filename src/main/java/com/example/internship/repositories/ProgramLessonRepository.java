package com.example.internship.repositories;

import com.example.internship.models.ProgramLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramLessonRepository extends JpaRepository<ProgramLesson, Long> {
    List<ProgramLesson> findByInternshipIdOrderBySortOrderAscIdAsc(Long internshipId);

    void deleteByInternshipId(Long internshipId);
}
