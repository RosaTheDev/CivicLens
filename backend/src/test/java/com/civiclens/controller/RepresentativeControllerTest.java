package com.civiclens.controller;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.DonorSummary;
import com.civiclens.domain.Party;
import com.civiclens.domain.Representative;
import com.civiclens.domain.Stance;
import com.civiclens.domain.User;
import com.civiclens.domain.UserStance;
import com.civiclens.service.CurrentUserService;
import com.civiclens.service.DonorSummaryService;
import com.civiclens.service.RecentBillsService;
import com.civiclens.service.RepresentativeService;
import com.civiclens.service.UserStanceService;
import com.civiclens.service.WatchlistService;
import com.civiclens.security.JwtAuthenticationFilter;
import com.civiclens.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RepresentativeController.class)
@AutoConfigureMockMvc(addFilters = false)
class RepresentativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private RepresentativeService representativeService;

    @MockBean
    private WatchlistService watchlistService;

    @MockBean
    private DonorSummaryService donorSummaryService;

    @MockBean
    private UserStanceService userStanceService;

    @MockBean
    private RecentBillsService recentBillsService;

    // Satisfy security infrastructure for this slice test
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void getRepresentativesByZip_returnsList() throws Exception {
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .district("12")
                .party(Party.DEMOCRATIC)
                .build();
        when(representativeService.getRepresentativesByZip("94110"))
                .thenReturn(List.of(rep));

        mockMvc.perform(get("/api/representatives")
                        .param("zip", "94110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Jane Smith"))
                .andExpect(jsonPath("$[0].state").value("CA"));
    }

    @Test
    void getRepresentative_returnsSingleRepOr404() throws Exception {
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .district("12")
                .party(Party.DEMOCRATIC)
                .build();
        when(representativeService.getById(1L)).thenReturn(rep);
        when(representativeService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/representatives/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Smith"));

        mockMvc.perform(get("/api/representatives/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void donorSummary_returnsSummary() throws Exception {
        DonorSummary summary = DonorSummary.builder()
                .id(1L)
                .source("FEC (mock)")
                .cycleYear(2024)
                .build();
        when(donorSummaryService.getForRepresentative(1L, 2024)).thenReturn(summary);

        mockMvc.perform(get("/api/donor-summaries")
                        .param("representativeId", "1")
                        .param("cycleYear", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("FEC (mock)"))
                .andExpect(jsonPath("$.cycleYear").value(2024));
    }

    @Test
    void getRecentBills_returnsBillsOr404() throws Exception {
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .build();
        when(representativeService.getById(1L)).thenReturn(rep);
        RecentBillsService.RecentBill bill = new RecentBillsService.RecentBill(
                "https://congress.gov/bill/1", "A bill", "Description");
        when(recentBillsService.getRecentBillsForRepresentative(rep))
                .thenReturn(List.of(bill));

        mockMvc.perform(get("/api/representatives/1/recent-bills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("A bill"));

        when(representativeService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/representatives/999/recent-bills"))
                .andExpect(status().isNotFound());
    }

    @Test
    void me_returnsCurrentUser() throws Exception {
        User user = User.builder()
                .id(1L)
                .email("u@example.com")
                .displayName("User One")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@example.com"))
                .andExpect(jsonPath("$.displayName").value("User One"));
    }

    @Test
    void setStanceOnRepresentative_returnsUserStance() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .build();
        User user = User.builder().id(1L).email("u@example.com").build();
        UserStance stance = UserStance.builder()
                .id(10L)
                .user(user)
                .representative(rep)
                .stance(Stance.SUPPORT)
                .note("Great on climate")
                .build();

        when(userStanceService.setStanceOnRepresentative(1L, 1L, Stance.SUPPORT, "Great on climate"))
                .thenReturn(stance);

        mockMvc.perform(post("/api/stances")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("representativeId", "1")
                        .param("stance", "SUPPORT")
                        .param("note", "Great on climate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stance").value("SUPPORT"))
                .andExpect(jsonPath("$.note").value("Great on climate"));
    }
}

