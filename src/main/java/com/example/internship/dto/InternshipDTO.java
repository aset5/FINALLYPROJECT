package com.example.internship.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class InternshipDTO {
    private Long id;
    private String title;
    private String companyName; // Передаем только имя, а не весь объект компании
    private String city;
    private LocalDate startDate;
}