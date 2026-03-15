package com.example.internship.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank; // ОСЫЛАРДЫ ТЕКСЕР
import jakarta.validation.constraints.Pattern;  // ОСЫЛАРДЫ ТЕКСЕР
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private boolean enabled = false; // Почта расталғанша жүйеге кіре алмайды
    private String verificationCode; // UUID түріндегі бірегей код

    @Column(nullable = false)
    @NotBlank(message = "Құпия сөз бос болмауы керек")
    @Size(min = 8, message = "Құпия сөз кемінде 8 символдан тұруы керек")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Құпия сөзде кемінде бір бас әріп, бір кіші әріп, бір цифр және бір арнайы символ болуы қажет")
    private String password;

    @ManyToOne // Один университет может иметь несколько админов
    @JoinColumn(name = "university_id")
    private University university;

    // Добавляем отсутствующие поля:
    private String fullName;
    private String email;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String resume; // Текстовое описание

    private String resumePath; // Путь к PDF файлу

    @Enumerated(EnumType.STRING)
    private Role role; // STUDENT, COMPANY, ADMIN



    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Company company;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    // С аннотацией @Data от Lombok геттеры и сеттеры создаются автоматически.
    // Если Lombok не подхватывается, оставь ручные методы ниже:

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }
}