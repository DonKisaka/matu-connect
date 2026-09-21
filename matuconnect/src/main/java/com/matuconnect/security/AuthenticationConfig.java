package com.matuconnect.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

/**
 * Exposes the {@link AuthenticationManager} that {@code AuthController} uses
 * to verify credentials.
 * <p>
 * Kept apart from {@link SecurityConfig} on purpose. A {@code @WebMvcTest}
 * slice needs to import the access rules to test them, but building an
 * authentication manager drags in the whole authentication provider chain —
 * including the {@code UserDetailsService} and its repository, neither of
 * which belongs in a controller slice. Splitting them lets a slice import the
 * rules alone.
 */
@Configuration
public class AuthenticationConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
