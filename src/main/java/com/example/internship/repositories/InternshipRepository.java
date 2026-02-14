package com.example.internship.repositories;

import com.example.internship.models.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {

    // Этот метод найдет все вакансии, принадлежащие конкретной компании по её ID
    List<Internship> findByCompanyId(Long companyId);

    // Также нам пригодится этот метод для главной страницы (только одобренные)
    List<Internship> findByStatus(com.example.internship.models.InternshipStatus status);
}