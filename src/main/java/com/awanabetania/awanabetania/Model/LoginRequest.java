package com.awanabetania.awanabetania.Model;

import lombok.Data;

/**
 * DTO carrying login credentials from the React frontend to {@code AuthController}.
 * The {@code username} field accepts either the generated username or the legacy
 * plain name (for backward compatibility). The {@code role} field determines
 * which user table (Child or Leader) is searched.
 */
@Data
public class LoginRequest {

    /** Username or display name entered in the login form. */
    private String username;

    /** Plain-text password entered by the user. */
    private String password;

    /**
     * Role selected on the login screen.
     * Accepted values: "CHILD", "LEADER", "DIRECTOR".
     */
    private String role;
}
