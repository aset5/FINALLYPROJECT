package com.example.internship.integration;

import com.example.internship.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiSecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void homeEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCompanyInternships").value(2));
    }

    @Test
    void companyProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/company/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ACME\",\"bin\":\"123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void companyProfile_forbiddenForStudentRole() throws Exception {
        data.student("student_sec", data.university("Sec University"));

        mockMvc.perform(post("/api/company/profile")
                        .with(user("student_sec").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ACME\",\"bin\":\"123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_forbiddenForCompanyRole() throws Exception {
        data.companyUser("company_sec");

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                        .with(user("company_sec").roles("COMPANY")))
                .andExpect(status().isForbidden());
    }

    @Test
    void spaRoot_returnsFrontendShell() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
