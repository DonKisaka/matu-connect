package com.matuconnect.controller;


import com.matuconnect.model.Role;
import com.matuconnect.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request/response shapes for registration and sign-in.
 */

/**
 * Self-service sign-up. Note there is deliberately no role field: a caller
 * must not be able to make themselves an administrator by adding one to the
 * JSON. New accounts are always {@link Role#COMMUTER}; promotion is an
 * administrative act, not a registration option.
 */
record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be 3-100 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        String password
) {
}

/**
 * Sign-in credentials. Length constraints are intentionally absent — an
 * existing password must be accepted as stored, and rejecting it early on
 * length would tell an attacker something about the account.
 */
record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {
}

/**
 * The authenticated user as the client sees them. Carries no password field
 * of any kind, not even the hash.
 */
record AuthUserDto(Long id, String username, Role role) {

    static AuthUserDto from(User user) {
        return new AuthUserDto(user.getId(), user.getUsername(), user.getRole());
    }
}

/**
 * A failure the client is expected to show the user, such as a taken
 * username or rejected credentials. Deliberately carries only a message —
 * no stack trace, no field hints an attacker could learn from.
 */
record ErrorDto(String message) {
}
