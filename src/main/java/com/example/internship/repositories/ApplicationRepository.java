package com.example.internship.repositories;

import com.example.internship.models.Application;
import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudent(User student);

    List<Application> findByInternshipCompanyId(Long companyId);

}