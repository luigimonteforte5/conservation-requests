package com.luigimonteforte.conservationrequests.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luigimonteforte.conservationrequests.security.model.LoginRequest;
import com.luigimonteforte.conservationrequests.security.model.LoginResponse;
import com.luigimonteforte.conservationrequests.security.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with the token when the credentials are valid")
    void login_returns200WithToken_whenCredentialsAreValid() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "s3cret");
        when(authService.performLogin(any(LoginRequest.class))).thenReturn(new LoginResponse("a.jwt.token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a.jwt.token"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).performLogin(captor.capture());
        assertEquals(loginRequest, captor.getValue());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 400 and does not call the service when the credentials are blank")
    void login_returns400_whenCredentialsAreBlank() throws Exception {
        LoginRequest blankRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).performLogin(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 401 with a generic message when the credentials are rejected")
    void login_returns401WithGenericMessage_whenCredentialsAreRejected() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "wrong");
        when(authService.performLogin(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }
}
