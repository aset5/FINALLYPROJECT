package com.example.internship.repositories;

import com.example.internship.models.Role;
import com.example.internship.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByUsernameContainingIgnoreCase(String username);
    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    long countByEnabledFalseAndRoleIn(Collection<Role> roles);

    List<User> findByEnabledFalseAndRoleInOrderByIdAsc(Collection<Role> roles);
}