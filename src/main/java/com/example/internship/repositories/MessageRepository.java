package com.example.internship.repositories;

import com.example.internship.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // Найти сообщения между HR и студентом по конкретной вакансии
    List<Message> findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
            Long intId1, Long s1, Long r1, Long intId2, Long s2, Long r2);
}