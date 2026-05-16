package com.payup.userservice.auth.oauth2;

import com.payup.userservice.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final String frontendRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService,
                                              @Value("${app.frontend-redirect-url}") String frontendRedirectUrl) {
        this.jwtService = jwtService;
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String token = jwtService.generateToken(principal.getUserId(), email, principal.getRole());

        // Invalidate the transient OAuth2 flow session before redirecting
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(request, response,
            frontendRedirectUrl + "/oauth2/callback?token=" + token);
    }
}
