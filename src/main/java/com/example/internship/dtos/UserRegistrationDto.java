package com.example.internship.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegistrationDto {

    @NotBlank(message = "Электрондық пошта бос болмауы керек")
    @Email(message = "Пошта форматы дұрыс емес")
    private String email;

    @NotBlank(message = "Құпия сөз бос болмауы керек")
    @Size(min = 8, max = 20, message = "Құпия сөз 8 бен 20 символ аралығында болуы керек")
    /* RegEx түсіндірмесі:
       (?=.*[0-9]) - кемінде бір цифр
       (?=.*[a-z]) - кемінде бір кіші әріп
       (?=.*[A-Z]) - кемінде бір бас әріп
       (?=.*[@#$%^&+=!]) - кемінде бір арнайы символ
    */
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Құпия сөзде кемінде бір бас әріп, бір кіші әріп, бір цифр және бір арнайы символ болуы қажет")
    private String password;
}