package com.civiclens.auth;

import com.civiclens.auth.dto.AuthResponse;
import com.civiclens.auth.dto.LoginRequest;
import com.civiclens.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AuthService authService;

    @MockBean
    private com.civiclens.security.JwtProvider jwtProvider;

    @Test
    void register_returnsTokenAndUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("u@example.com");
        request.setPassword("password123");
        AuthResponse response = AuthResponse.builder()
                .token("jwt")
                .userId(1L)
                .email("u@example.com")
                .displayName("User")
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.email").value("u@example.com"));
    }

    @Test
    void login_returnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("u@example.com");
        request.setPassword("pass");
        AuthResponse response = AuthResponse.builder().token("jwt").userId(1L).email("u@example.com").build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }
}
