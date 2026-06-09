package com.example.internship.integration;

import com.example.internship.models.ApplicationStatus;
import com.example.internship.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentBusinessRulesIntegrationTest extends IntegrationTestBase {

    @Test
    void applyToCompanyJob_requiresVerifiedStudent() throws Exception {
        var university = data.university("ENU");
        var student = data.student("unverified_student", university);
        var companyOwner = data.companyUser("job_company_owner");
        var company = data.company(companyOwner, "Tech Corp", "999999999999");
        var job = data.approvedCompanyJob(company, "Java Intern");

        mockMvc.perform(post("/api/student/apply/{id}", job.getId())
                        .with(user("unverified_student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyToCompanyJob_succeedsForVerifiedStudent() throws Exception {
        var university = data.university("SDU");
        var student = data.student("verified_student", university);
        var companyOwner = data.companyUser("verified_job_owner");
        var company = data.company(companyOwner, "Data Corp", "888888888888");
        var job = data.approvedCompanyJob(company, "Data Analyst Intern");

        var verifiedProgram = data.approvedUniversityProgram(university, "Analytics");
        data.application(student, verifiedProgram, ApplicationStatus.VERIFIED);

        mockMvc.perform(post("/api/student/apply/{id}", job.getId())
                        .with(user("verified_student").roles("STUDENT")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/apply/{id}", job.getId())
                        .with(user("verified_student").roles("STUDENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void companyCannotAcceptThirdInternshipForSameStudent() throws Exception {
        var university = data.university("AITU");
        var student = data.student("busy_student", university);
        var owner1 = data.companyUser("accept_owner_1");
        var owner2 = data.companyUser("accept_owner_2");
        var owner3 = data.companyUser("accept_owner_3");
        var company1 = data.company(owner1, "C1", "111111111111");
        var company2 = data.company(owner2, "C2", "222222222222");
        var company3 = data.company(owner3, "C3", "333333333333");

        var job1 = data.approvedCompanyJob(company1, "Job 1");
        var job2 = data.approvedCompanyJob(company2, "Job 2");
        var job3 = data.approvedCompanyJob(company3, "Job 3");

        data.application(student, job1, ApplicationStatus.ACCEPTED);
        data.application(student, job2, ApplicationStatus.ACCEPTED);
        var app3 = data.application(student, job3, ApplicationStatus.PENDING);

        mockMvc.perform(post("/api/company/applications/{id}/accept", app3.getId())
                        .with(user("accept_owner_3").roles("COMPANY")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentCannotEnrollInSecondUniversityProgram() throws Exception {
        var university = data.university("Nazarbayev University");
        var student = data.student("uni_student", university);
        var program1 = data.approvedUniversityProgram(university, "Program A");
        var program2 = data.approvedUniversityProgram(university, "Program B");

        data.application(student, program1, ApplicationStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/student/apply/{id}", program2.getId())
                        .with(user("uni_student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
