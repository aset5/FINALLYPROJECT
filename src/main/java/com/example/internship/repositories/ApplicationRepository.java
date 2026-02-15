package com.example.internship.repositories;

import com.example.internship.models.Application;
import com.example.internship.models.Internship;
import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByInternshipCompanyId(Long companyId);
    List<Application> findByStudent(User student);

    void deleteByStudent(User student);
    void deleteByInternship(Internship internship);
    void deleteByInternshipId(Long internshipId);
    boolean existsByStudentAndInternship(User student, Internship internship);
}