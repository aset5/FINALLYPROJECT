package com.example.internship.repositories;

import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Этот метод понадобится для авторизации (Spring Security)
    Optional<User> findByUsername(String username);
}