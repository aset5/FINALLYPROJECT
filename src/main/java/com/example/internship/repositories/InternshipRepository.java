package com.example.internship.repositories;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {

    // Для главной страницы: загружаем только те, у которых статус APPROVED
    List<Internship> findByStatus(InternshipStatus status);

    // Можно добавить поиск по городу для фильтров в будущем
    List<Internship> findByCityIgnoreCase(String city);
        @EntityGraph(attributePaths = {"company", "company.user"})
        List<Internship> findAllByStatus(InternshipStatus status);
    }
