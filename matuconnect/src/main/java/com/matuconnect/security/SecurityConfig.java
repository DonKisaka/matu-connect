package com.matuconnect.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Access rules for the MatuConnect API.
 *
 * <h2>Who can reach what</h2>
 * Browsing the network is deliberately public: the map, stop search, route
 * planning and the chat agent all work signed out, because requiring an
 * account to look up a matatu would defeat the point of the system. Signing
 * in adds a personal journey history; the reporting endpoints are for
 * administrators only.
 *
 * <h2>Sessions rather than tokens</h2>
 * The browser reaches this API through the Next.js rewrite proxy, so from its
 * point of view the API is same-origin and an ordinary session cookie works.
 * That avoids hand-rolling JWT issuing, refresh and revocation for no gain —
 * there is no third-party client to serve.
 *
 * <h2>CSRF</h2>
 * Because authentication rides on a cookie, the browser attaches it to
 * cross-site requests too, so CSRF protection is kept on rather than
 * disabled. The token is published in a readable {@code XSRF-TOKEN} cookie
 * and echoed back by the frontend in {@code X-XSRF-TOKEN} — the standard
 * double-submit pattern. Safe methods are exempt by definition, so the map
 * and reports still load with no client involvement.
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt: salted per password and deliberately slow, so a leaked table
     * cannot be attacked at the speed a general-purpose hash would allow.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Spring Security defers CSRF token loading by default: the token — and
        // therefore the cookie — is only materialised if something reads it
        // during the request. Nothing does in a JSON API, so no cookie was ever
        // issued and no client could have performed a POST at all, making sign-in
        // impossible. Setting the request-attribute name to null opts out of that
        // deferral so the token is resolved, and the cookie written, every time.
        //
        // The plain handler is used rather than the XOR default because the
        // client echoes the cookie value back verbatim; the XOR handler masks the
        // rendered token per request, which only makes sense when a server-side
        // template embeds it.
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        // withHttpOnlyFalse so the frontend can read the token and
                        // echo it back; the cookie is not the credential, the
                        // session is, so exposing it to script is the intent.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfRequestHandler))

                .authorizeHttpRequests(auth -> auth
                        // Public browsing — no account required.
                        .requestMatchers(HttpMethod.GET,
                                "/api/stops/**",
                                "/api/routes/**",
                                "/api/coverage",
                                "/api/coverage/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()

                        // Registration and sign-in must be reachable signed out.
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // Reporting and route editing are administrator concerns.
                        // Namespaced so this stays one matcher rather than
                        // annotations scattered about.
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Anything personal requires an account.
                        .requestMatchers("/api/me/**").authenticated()

                        .anyRequest().authenticated())

                // An unauthenticated API call should be told so, not redirected to
                // a login page it cannot render.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))

                // No form login or HTTP Basic: sign-in goes through AuthController
                // so the client exchanges JSON like every other endpoint.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
