package com.payup.userservice.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payup.userservice.auth.dto.SignInRequest;
import com.payup.userservice.auth.dto.SignUpRequest;
import com.payup.userservice.user.EmailAlreadyExistsException;
import com.payup.userservice.user.User;
import com.payup.userservice.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// addFilters=false drops the Spring Security filter chain so this test focuses
// purely on controller logic — request mapping, validation, and error status codes.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;

    // ── POST /auth/signup ─────────────────────────────────────────────────────

    @Test
    void signUp_returns201WithToken_whenRequestIsValid() throws Exception {
        User saved = user(1L, "alice@example.com", "Alice");
        given(userService.registerUser("alice@example.com", "password1", "Alice")).willReturn(saved);
        given(jwtService.generateToken(1L, "alice@example.com", "ROLE_USER")).willReturn("signed.jwt.token");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignUpRequest("alice@example.com", "password1", "Alice"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void signUp_returns409_whenEmailAlreadyExists() throws Exception {
        given(userService.registerUser(eq("alice@example.com"), any(), any()))
            .willThrow(new EmailAlreadyExistsException("alice@example.com"));

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignUpRequest("alice@example.com", "password1", "Alice"))))
            .andExpect(status().isConflict());
    }

    @Test
    void signUp_returns400_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignUpRequest("not-an-email", "password1", "Alice"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_returns400_whenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignUpRequest("alice@example.com", "short", "Alice"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_returns400_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignUpRequest("alice@example.com", "password1", ""))))
            .andExpect(status().isBadRequest());
    }

    // ── POST /auth/signin ─────────────────────────────────────────────────────

    @Test
    void signIn_returns200WithToken_whenCredentialsAreValid() throws Exception {
        User existing = user(2L, "bob@example.com", "Bob");
        given(userService.findByEmail("bob@example.com")).willReturn(Optional.of(existing));
        given(userService.checkPassword(any(User.class), eq("mypassword"))).willReturn(true);
        given(jwtService.generateToken(2L, "bob@example.com", "ROLE_USER")).willReturn("bob.jwt.token");

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("bob@example.com", "mypassword"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("bob.jwt.token"))
            .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    @Test
    void signIn_returns401_whenPasswordIsWrong() throws Exception {
        User existing = user(2L, "bob@example.com", "Bob");
        given(userService.findByEmail("bob@example.com")).willReturn(Optional.of(existing));
        given(userService.checkPassword(any(User.class), eq("wrongpass"))).willReturn(false);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("bob@example.com", "wrongpass"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void signIn_returns401_whenUserDoesNotExist() throws Exception {
        given(userService.findByEmail(any())).willReturn(Optional.empty());

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("ghost@example.com", "password1"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void signIn_returns400_whenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("", "password1"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signIn_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private User user(Long id, String email, String name) {
        User u = new User(email, "$2a$10$hashed", name, "local", null, "ROLE_USER");
        u.setId(id);
        return u;
    }
}
