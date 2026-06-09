package com.example.internship.integration;

import com.example.internship.models.ApplicationStatus;
import com.example.internship.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeApiIntegrationTest extends IntegrationTestBase {

    @Test
    void home_returnsStudentProgressFlags() throws Exception {
        var university = data.university("Home University");
        var student = data.student("home_student", university);
        var program = data.approvedUniversityProgram(university, "Visible Program");
        var companyOwner = data.companyUser("home_company");
        var company = data.company(companyOwner, "Home Corp", "123123123123");
        var job = data.approvedCompanyJob(company, "Visible Job");

        data.application(student, program, ApplicationStatus.IN_PROGRESS);
        data.application(student, job, ApplicationStatus.ACCEPTED);

        mockMvc.perform(get("/api/home").with(user("home_student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveUniversityProgram").value(true))
                .andExpect(jsonPath("$.acceptedCompanyInternships").value(1))
                .andExpect(jsonPath("$.maxCompanyInternships").value(2))
                .andExpect(jsonPath("$.uniPrograms[0].title").value("Visible Program"))
                .andExpect(jsonPath("$.companyJobs[0].title").value("Visible Job"));
    }
}
