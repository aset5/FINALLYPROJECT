package com.example.internship.repositories;


import com.example.internship.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Находит компанию по ID связанного с ней пользователя
    Company findByUserId(Long userId);
}
