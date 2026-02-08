package com.example.internship.repositories;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {
    List<Internship> findByStatus(InternshipStatus status);}