package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String bin;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

}