package com.example.internship.integration;

import com.example.internship.models.Company;
import com.example.internship.repositories.CompanyRepository;
import com.example.internship.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyProfileSecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void saveProfile_updatesOnlyCurrentUsersCompany() throws Exception {
        var owner1 = data.companyUser("company_owner_1");
        var owner2 = data.companyUser("company_owner_2");
        Company victim = data.company(owner2, "Victim Corp", "111111111111");

        mockMvc.perform(post("/api/company/profile")
                        .with(user("company_owner_1").roles("COMPANY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Owner One LLC","bin":"222222222222"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Owner One LLC"))
                .andExpect(jsonPath("$.userId").value(owner1.getId()));

        Company savedForOwner1 = companyRepository.findByUserId(owner1.getId());
        Company untouchedVictim = companyRepository.findById(victim.getId()).orElseThrow();

        assertThat(savedForOwner1.getUser().getId()).isEqualTo(owner1.getId());
        assertThat(savedForOwner1.getName()).isEqualTo("Owner One LLC");
        assertThat(untouchedVictim.getName()).isEqualTo("Victim Corp");
        assertThat(untouchedVictim.getBin()).isEqualTo("111111111111");
    }

    @Test
    void saveProfile_rejectsBlankName() throws Exception {
        data.companyUser("company_invalid");

        mockMvc.perform(post("/api/company/profile")
                        .with(user("company_invalid").roles("COMPANY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"bin\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
