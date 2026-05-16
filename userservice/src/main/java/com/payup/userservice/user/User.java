package com.payup.userservice.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class User {

    @Id
    private Long id;
    private String email;
    private String password;    // null for OAuth2-only users
    private String name;
    private String provider;    // "local" | "google"
    private String providerId;  // provider's subject claim
    private String role;

    public User() {}

    public User(String email, String password, String name, String provider, String providerId, String role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public static User forLocalAuth(String email, String hashedPassword, String name) {
        return new User(email, hashedPassword, name, "local", null, "ROLE_USER");
    }

    public static User forOAuth2(String email, String name, String provider, String providerId) {
        return new User(email, null, name, provider, providerId, "ROLE_USER");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
