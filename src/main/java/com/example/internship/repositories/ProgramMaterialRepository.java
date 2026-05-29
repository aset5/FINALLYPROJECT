package com.example.internship.repositories;

import com.example.internship.models.ProgramMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgramMaterialRepository extends JpaRepository<ProgramMaterial, Long> {
    List<ProgramMaterial> findByInternshipIdOrderBySortOrderAscIdAsc(Long internshipId);

    Optional<ProgramMaterial> findByFilePath(String filePath);

    void deleteByInternshipId(Long internshipId);
}
