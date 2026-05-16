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

// addFilters=false drops the Spring Security filter chain so this slice test
// focuses purely on controller logic: request mapping, validation, error status codes.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;

    // ── POST /auth/signup ─────────────────────────────────────────────────────

    @Test
    void should_return_201_and_token_when_signup_request_is_valid() throws Exception {
        // given
        User saved = user(1L, "alice@example.com", "Alice");
        given(userService.registerUser("alice@example.com", "password1", "Alice")).willReturn(saved);
        given(jwtService.generateToken(1L, "alice@example.com", "ROLE_USER")).willReturn("signed.jwt.token");

        // when
        var result = mockMvc.perform(post("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SignUpRequest("alice@example.com", "password1", "Alice"))));

        // then
        result.andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void should_return_409_when_email_is_already_registered() throws Exception {
        // given
        given(userService.registerUser(eq("alice@example.com"), any(), any()))
            .willThrow(new EmailAlreadyExistsException("alice@example.com"));

        // when
        var result = mockMvc.perform(post("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SignUpRequest("alice@example.com", "password1", "Alice"))));

        // then
        result.andExpect(status().isConflict());
    }

    @Test
    void should_return_400_when_signup_email_is_invalid() throws Exception {
        // given
        var request = new SignUpRequest("not-an-email", "password1", "Alice");

        // when
        var result = mockMvc.perform(post("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_signup_password_is_too_short() throws Exception {
        // given
        var request = new SignUpRequest("alice@example.com", "short", "Alice");

        // when
        var result = mockMvc.perform(post("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_signup_name_is_blank() throws Exception {
        // given
        var request = new SignUpRequest("alice@example.com", "password1", "");

        // when
        var result = mockMvc.perform(post("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    // ── POST /auth/signin ─────────────────────────────────────────────────────

    @Test
    void should_return_200_and_token_when_credentials_are_valid() throws Exception {
        // given
        User existing = user(2L, "bob@example.com", "Bob");
        given(userService.findByEmail("bob@example.com")).willReturn(Optional.of(existing));
        given(userService.checkPassword(any(User.class), eq("mypassword"))).willReturn(true);
        given(jwtService.generateToken(2L, "bob@example.com", "ROLE_USER")).willReturn("bob.jwt.token");

        // when
        var result = mockMvc.perform(post("/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SignInRequest("bob@example.com", "mypassword"))));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("bob.jwt.token"))
            .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    @Test
    void should_return_401_when_password_is_wrong() throws Exception {
        // given
        User existing = user(2L, "bob@example.com", "Bob");
        given(userService.findByEmail("bob@example.com")).willReturn(Optional.of(existing));
        given(userService.checkPassword(any(User.class), eq("wrongpass"))).willReturn(false);

        // when
        var result = mockMvc.perform(post("/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SignInRequest("bob@example.com", "wrongpass"))));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_user_does_not_exist() throws Exception {
        // given
        given(userService.findByEmail(any())).willReturn(Optional.empty());

        // when
        var result = mockMvc.perform(post("/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SignInRequest("ghost@example.com", "password1"))));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_400_when_signin_email_is_blank() throws Exception {
        // given
        var request = new SignInRequest("", "password1");

        // when
        var result = mockMvc.perform(post("/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_signin_body_is_missing() throws Exception {
        // given — no request body

        // when
        var result = mockMvc.perform(post("/auth/signin")
            .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private User user(Long id, String email, String name) {
        User u = new User(email, "$2a$10$hashed", name, "local", null, "ROLE_USER");
        u.setId(id);
        return u;
    }
}
