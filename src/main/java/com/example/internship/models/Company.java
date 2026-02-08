package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String bin;

    // СВЯЗЬ: Один аккаунт User может владеть одной Company
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "company")
    private List<Internship> internships;
}