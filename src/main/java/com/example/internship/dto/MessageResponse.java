package com.example.internship.dto;

import com.example.internship.models.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String content,
        LocalDateTime sentAt,
        String senderUsername,
        Long senderId
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getSentAt(),
                message.getSender().getUsername(),
                message.getSender().getId()
        );
    }
}
