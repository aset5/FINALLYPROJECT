package com.example.internship.repositories;

import com.example.internship.models.ProgramMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramMaterialRepository extends JpaRepository<ProgramMaterial, Long> {
    List<ProgramMaterial> findByInternshipIdOrderBySortOrderAscIdAsc(Long internshipId);

    void deleteByInternshipId(Long internshipId);
}
