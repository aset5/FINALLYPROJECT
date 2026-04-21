package com.example.internship.repositories;


import com.example.internship.models.University;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByName(String name);
    University findByUser(com.example.internship.models.User user);

    @Query("SELECT u FROM University u WHERE u.user.id = :userId")
    University findByUserId(@Param("userId") Long userId);}