package com.example.internship.models;


public enum ApplicationStatus {
    PENDING,    // Ожидание ответа от компании
    ACCEPTED,
    APPROVED,
    IN_PROGRESS,// В процессе обучения/тестирования
    COMPLETED,
    VERIFIED,// Сразу используем этот// Студент одобрен (оффер/интервью)
    REJECTED    // Отказ
}
