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

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    void should_return_user_when_email_exists() {
        // given
        User user = localUser();
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(user));

        // when
        Optional<User> result = userService.findByEmail("alice@example.com");

        // then
        assertThat(result).contains(user);
    }

    @Test
    void should_return_empty_when_email_does_not_exist() {
        // given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // when
        Optional<User> result = userService.findByEmail("nobody@example.com");

        // then
        assertThat(result).isEmpty();
    }

    // ── checkPassword ─────────────────────────────────────────────────────────

    @Test
    void should_return_true_when_password_matches() {
        // given
        User user = localUser();
        given(passwordEncoder.matches("secret", user.getPassword())).willReturn(true);

        // when
        boolean result = userService.checkPassword(user, "secret");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_when_password_does_not_match() {
        // given
        User user = localUser();
        given(passwordEncoder.matches("wrong", user.getPassword())).willReturn(false);

        // when
        boolean result = userService.checkPassword(user, "wrong");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void should_return_false_without_checking_encoder_when_user_has_no_password() {
        // given
        User oauthUser = oauth2User();

        // when
        boolean result = userService.checkPassword(oauthUser, "anything");

        // then
        assertThat(result).isFalse();
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ── registerUser ──────────────────────────────────────────────────────────

    @Test
    void should_save_and_return_user_when_email_is_not_taken() {
        // given
        given(userRepository.findByEmail("bob@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        User saved = new User("bob@example.com", "hashed", "Bob", "local", null, "ROLE_USER");
        given(userRepository.save(any())).willReturn(saved);

        // when
        User result = userService.registerUser("bob@example.com", "password1", "Bob");

        // then
        assertThat(result.getEmail()).isEqualTo("bob@example.com");
        assertThat(result.getName()).isEqualTo("Bob");
        verify(passwordEncoder).encode("password1");
    }

    @Test
    void should_throw_when_email_is_already_registered() {
        // given
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(localUser()));

        // when / then
        assertThatThrownBy(() -> userService.registerUser("alice@example.com", "password1", "Alice"))
            .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    // ── findOrCreateOAuth2User ────────────────────────────────────────────────

    @Test
    void should_return_existing_user_when_provider_id_matches() {
        // given
        User existing = oauth2User();
        given(userRepository.findByProviderAndProviderId("google", "g-123")).willReturn(Optional.of(existing));

        // when
        User result = userService.findOrCreateOAuth2User("alice@example.com", "Alice", "google", "g-123");

        // then
        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_link_provider_and_save_when_email_matches_but_no_provider_id() {
        // given
        User emailMatch = localUser();
        given(userRepository.findByProviderAndProviderId("google", "g-999")).willReturn(Optional.empty());
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(emailMatch));
        given(userRepository.save(emailMatch)).willReturn(emailMatch);

        // when
        userService.findOrCreateOAuth2User("alice@example.com", "Alice", "google", "g-999");

        // then
        assertThat(emailMatch.getProvider()).isEqualTo("google");
        assertThat(emailMatch.getProviderId()).isEqualTo("g-999");
        verify(userRepository).save(emailMatch);
    }

    @Test
    void should_create_new_user_when_no_matching_provider_id_or_email() {
        // given
        given(userRepository.findByProviderAndProviderId("google", "g-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        User created = new User("new@example.com", null, "New", "google", "g-new", "ROLE_USER");
        given(userRepository.save(any())).willReturn(created);

        // when
        User result = userService.findOrCreateOAuth2User("new@example.com", "New", "google", "g-new");

        // then
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
