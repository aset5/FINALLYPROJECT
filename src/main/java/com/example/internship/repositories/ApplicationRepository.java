package com.example.internship.repositories;

import com.example.internship.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Для панели студента: найти все его отклики
    List<Application> findByStudentId(Long studentId);

    // Для админа/компании: найти всех кандидатов на конкретную вакансию
    List<Application> findByInternshipId(Long internshipId);
}