package com.matuconnect.security;


import com.matuconnect.model.User;
import com.matuconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridges the application's {@link User} entity to Spring Security's
 * {@link UserDetails}.
 * <p>
 * The stored {@code Role} enum becomes a {@code ROLE_}-prefixed authority,
 * which is the prefix {@code hasRole("ADMIN")} expects — writing the prefix
 * into the database instead would leak a framework convention into the
 * schema, and would break silently if the convention ever changed.
 * <p>
 * The failure message is deliberately identical whether the username is
 * unknown or the password is wrong, so the endpoint cannot be used to
 * enumerate which accounts exist.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
