package com.matuconnect.security;


import com.matuconnect.model.Role;
import com.matuconnect.model.User;
import com.matuconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Creates the first administrator, since registration deliberately cannot
 * grant the ADMIN role and the reporting endpoints would otherwise be
 * unreachable by anyone.
 * <p>
 * Runs only when no administrator exists, so restarting never resets a
 * password that has since been changed.
 * <p>
 * If no password is configured, one is generated and written to the log once,
 * the way Spring Boot handles its own default user. A committed default
 * password would be worse than useless: it would ship an known-credential
 * administrator account to anyone who cloned the repository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${matuconnect.admin.username:admin}")
    private String adminUsername;

    /** Supply via MATUCONNECT_ADMIN_PASSWORD, or leave unset to have one generated. */
    @Value("${matuconnect.admin.password:}")
    private String configuredPassword;

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.debug("An administrator already exists — not creating another.");
            return;
        }
        if (userRepository.existsByUsername(adminUsername)) {
            log.warn("Username '{}' is taken by a non-admin account; no administrator created. "
                    + "Set matuconnect.admin.username to something else.", adminUsername);
            return;
        }

        boolean generated = configuredPassword == null || configuredPassword.isBlank();
        String password = generated ? generatePassword() : configuredPassword;

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);

        if (generated) {
            log.warn("""

                    ================================================================
                     Created administrator '{}' with a generated password:

                         {}

                     This is shown once and cannot be recovered. Set
                     MATUCONNECT_ADMIN_PASSWORD to choose your own instead.
                    ================================================================
                    """, adminUsername, password);
        } else {
            log.info("Created administrator '{}' from the configured password.", adminUsername);
        }
    }

    private static String generatePassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
