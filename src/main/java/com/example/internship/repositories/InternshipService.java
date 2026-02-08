package com.example.internship.repositories;

import com.example.internship.dto.InternshipDTO;

import java.util.List;

public interface InternshipService {
    List<InternshipDTO> getAllActiveInternships();
    void applyForInternship(Long studentId, Long internshipId);
}