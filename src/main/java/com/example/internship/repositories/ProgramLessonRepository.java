package com.example.internship.repositories;

import com.example.internship.models.ProgramLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgramLessonRepository extends JpaRepository<ProgramLesson, Long> {
    List<ProgramLesson> findByInternshipIdOrderBySortOrderAscIdAsc(Long internshipId);

    Optional<ProgramLesson> findByFilePath(String filePath);

    void deleteByInternshipId(Long internshipId);
}
