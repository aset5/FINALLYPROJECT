package com.example.internship.repositories;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.Company; // Импортируй модель Company
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {

    // Поиск вакансий по ID компании (для CompanyController)
    List<Internship> findByCompanyId(Long companyId);

    // Поиск и удаление по объекту Company (вместо Object)
    List<Internship> findByCompany(Company company);
    void deleteByCompany(Company company);

    List<Internship> findByStatus(InternshipStatus status);
    List<Internship> findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
            InternshipStatus status1, String title,
            InternshipStatus status2, String description
    );
    List<Internship> findByTitleContainingIgnoreCaseOrCityContainingIgnoreCase(String title, String city);

}