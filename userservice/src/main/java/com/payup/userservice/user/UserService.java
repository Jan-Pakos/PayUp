package com.payup.userservice.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User registerUser(String email, String rawPassword, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }
        return userRepository.save(User.forLocalAuth(email, passwordEncoder.encode(rawPassword), name));
    }

    public boolean checkPassword(User user, String rawPassword) {
        if (user.getPassword() == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    // Finds an existing user by OAuth2 identity, falling back to email match,
    // or creates a new user when none exists.
    public User findOrCreateOAuth2User(String email, String name, String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setProvider(provider);
                    existing.setProviderId(providerId);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.forOAuth2(email, name, provider, providerId)))
            );
    }
}
