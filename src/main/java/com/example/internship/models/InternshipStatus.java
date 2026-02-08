package com.example.internship.models;

public enum InternshipStatus {
    PENDING,   // На модерации (создана компанией, ждет админа)
    APPROVED,  // Одобрена (видна студентам)
    REJECTED   // Отклонена админом
}