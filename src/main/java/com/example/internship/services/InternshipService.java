package com.example.internship.services;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.repositories.InternshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InternshipService {

    @Autowired
    private InternshipRepository repository;

    public List<Internship> getApprovedInternships() {
        return repository.findByStatus(InternshipStatus.APPROVED);
    }

    public List<Internship> getPendingInternships() {
        return repository.findByStatus(InternshipStatus.PENDING);
    }

    public Internship getInternshipById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Не найдено"));
    }

    // Метод одобрения (публикации) админом
    public void approve(Long id) {
        Internship internship = repository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.APPROVED);
        repository.save(internship);
    }

    // Метод создания черновика компанией
    public void createInternship(Internship internship) {
        internship.setStatus(InternshipStatus.PENDING);
        repository.save(internship);
    }
}