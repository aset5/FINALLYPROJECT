package com.example.internship.repositories;

import com.example.internship.models.Company;
import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    // Этот метод жизненно необходим для работы контроллера!
    Company findByUser(User user);
}