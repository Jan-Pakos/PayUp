package com.payup.userservice.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    // ── findByEmail ─────────────────────────────────────────────────────────

    @Test
    void findByEmail_returnsUser_whenFound() {
        User user = localUser();
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(user));

        assertThat(userService.findByEmail("alice@example.com")).contains(user);
    }

    @Test
    void findByEmail_returnsEmpty_whenNotFound() {
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        assertThat(userService.findByEmail("nobody@example.com")).isEmpty();
    }

    // ── checkPassword ────────────────────────────────────────────────────────

    @Test
    void checkPassword_returnsTrue_whenPasswordMatches() {
        User user = localUser();
        given(passwordEncoder.matches("secret", user.getPassword())).willReturn(true);

        assertThat(userService.checkPassword(user, "secret")).isTrue();
    }

    @Test
    void checkPassword_returnsFalse_whenPasswordDoesNotMatch() {
        User user = localUser();
        given(passwordEncoder.matches("wrong", user.getPassword())).willReturn(false);

        assertThat(userService.checkPassword(user, "wrong")).isFalse();
    }

    @Test
    void checkPassword_returnsFalse_forOAuth2User_withNullPassword() {
        User oauthUser = oauth2User();

        assertThat(userService.checkPassword(oauthUser, "anything")).isFalse();
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ── registerUser ─────────────────────────────────────────────────────────

    @Test
    void registerUser_savesAndReturns_whenEmailIsNew() {
        given(userRepository.findByEmail("bob@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        User saved = new User("bob@example.com", "hashed", "Bob", "local", null, "ROLE_USER");
        given(userRepository.save(any())).willReturn(saved);

        User result = userService.registerUser("bob@example.com", "password1", "Bob");

        assertThat(result.getEmail()).isEqualTo("bob@example.com");
        assertThat(result.getName()).isEqualTo("Bob");
        verify(passwordEncoder).encode("password1");
    }

    @Test
    void registerUser_throws_whenEmailAlreadyExists() {
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(localUser()));

        assertThatThrownBy(() -> userService.registerUser("alice@example.com", "password1", "Alice"))
            .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    // ── findOrCreateOAuth2User ────────────────────────────────────────────────

    @Test
    void findOrCreateOAuth2User_returnsExisting_whenProviderIdMatches() {
        User existing = oauth2User();
        given(userRepository.findByProviderAndProviderId("google", "g-123")).willReturn(Optional.of(existing));

        User result = userService.findOrCreateOAuth2User("alice@example.com", "Alice", "google", "g-123");

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateOAuth2User_linksAndSaves_whenEmailMatchesButNoProviderId() {
        User emailMatch = localUser(); // local account, same email
        given(userRepository.findByProviderAndProviderId("google", "g-999")).willReturn(Optional.empty());
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(emailMatch));
        given(userRepository.save(emailMatch)).willReturn(emailMatch);

        userService.findOrCreateOAuth2User("alice@example.com", "Alice", "google", "g-999");

        assertThat(emailMatch.getProvider()).isEqualTo("google");
        assertThat(emailMatch.getProviderId()).isEqualTo("g-999");
        verify(userRepository).save(emailMatch);
    }

    @Test
    void findOrCreateOAuth2User_createsNew_whenNeitherProviderIdNorEmailMatches() {
        given(userRepository.findByProviderAndProviderId("google", "g-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        User created = new User("new@example.com", null, "New", "google", "g-new", "ROLE_USER");
        given(userRepository.save(any())).willReturn(created);

        User result = userService.findOrCreateOAuth2User("new@example.com", "New", "google", "g-new");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getProvider()).isEqualTo("google");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User localUser() {
        User u = new User("alice@example.com", "$2a$10$hashed", "Alice", "local", null, "ROLE_USER");
        u.setId(1L);
        return u;
    }

    private User oauth2User() {
        User u = new User("alice@example.com", null, "Alice", "google", "g-123", "ROLE_USER");
        u.setId(1L);
        return u;
    }
}
