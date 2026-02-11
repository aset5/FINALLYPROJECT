package com.example.internship.repositories;

import com.example.internship.models.InternshipApplicationModel;
import com.example.internship.models.Company;
import com.example.internship.models.Internship;
import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipApplicationRepository extends JpaRepository<InternshipApplicationModel, Long> {

    // Поиск всех откликов на вакансии конкретной компании
    List<InternshipApplicationModel> findByInternship_Company(Company company);

    // Проверка, откликался ли уже этот студент на эту вакансию
    boolean existsByStudentAndInternship(User student, Internship internship);
}