package com.example.internship.integration;

import com.example.internship.models.ApplicationStatus;
import com.example.internship.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningQuizIntegrationTest extends IntegrationTestBase {

    @Test
    void quizAllowsOnlyOneSubmission() throws Exception {
        var university = data.university("Quiz University");
        var student = data.student("quiz_student", university);
        var program = data.approvedUniversityProgram(university, "Quiz Program");
        var application = data.application(student, program, ApplicationStatus.IN_PROGRESS);
        var question = data.quizQuestion(program, 1);

        String answers = "{\"" + question.getId() + "\":1}";

        mockMvc.perform(post("/api/student/learning/{id}/quiz/submit", application.getId())
                        .with(user("quiz_student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePercent").value(100))
                .andExpect(jsonPath("$.passed").value(true));

        mockMvc.perform(post("/api/student/learning/{id}/quiz/submit", application.getId())
                        .with(user("quiz_student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answers))
                .andExpect(status().isBadRequest());
    }
}
