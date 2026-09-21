package com.matuconnect.controller;


import com.matuconnect.model.Role;
import com.matuconnect.model.User;
import com.matuconnect.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Registration and sign-in.
 * <p>
 * Sign-in is handled here rather than by {@code formLogin} so the client
 * exchanges JSON like it does everywhere else. Authenticating alone is not
 * enough: the resulting {@link SecurityContext} has to be written to the
 * session explicitly, otherwise it lives only for the current request and the
 * user appears signed out again on the next one.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorDto("That username is already taken"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Always COMMUTER. Promotion to ADMIN is an administrative act and is
        // never something a registration payload can ask for.
        user.setRole(Role.COMMUTER);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        log.info("Registered new user '{}'", saved.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthUserDto.from(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            // One message for both "no such user" and "wrong password", so the
            // endpoint cannot be used to discover which accounts exist.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorDto("Invalid username or password"));
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Without this the authentication would not outlive the request.
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(AuthUserDto.from(user));
    }

    /**
     * Who the caller currently is. Reaching this at all requires an
     * authenticated session, so a 401 here is how the frontend learns it is
     * signed out.
     */
    @GetMapping("/me")
    public AuthUserDto me(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return AuthUserDto.from(user);
    }
}
