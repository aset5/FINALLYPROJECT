package com.example.internship.repositories;

import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Находит пользователя по логину (используется при авторизации)
    Optional<User> findByUsername(String username);
}