package com.example.internship.integration;

import com.example.internship.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CertificateVerificationIntegrationTest extends IntegrationTestBase {

    @Test
    void verify_returnsValidForCompletedUniversityProgram() throws Exception {
        var university = data.university("KBTU");
        var student = data.student("cert_student", university);
        var program = data.approvedUniversityProgram(university, "Backend Bootcamp");
        data.completedUniversityApplication(student, program, "K7M9P2XQ4R1N");

        mockMvc.perform(get("/api/certificates/verify/IPRO-2026-K7M9P2XQ4R1N"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.studentName").value("Student cert_student"))
                .andExpect(jsonPath("$.programTitle").value("Backend Bootcamp"))
                .andExpect(jsonPath("$.universityName").value("KBTU"));
    }

    @Test
    void verify_returnsInvalidForUnknownNumber() throws Exception {
        mockMvc.perform(get("/api/certificates/verify/IPRO-2026-UNKNOWN1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void verify_returnsInvalidForMalformedNumber() throws Exception {
        mockMvc.perform(get("/api/certificates/verify/not-a-certificate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value(containsString("формат")));
    }
}
