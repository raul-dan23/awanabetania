package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A leader the director adds.
 *
 * @param email the Google account they sign in with; optional. Without it, or while Google
 *              sign-in is not configured, the leader gets a temporary password instead
 * @param role  LEADER, COORDONATOR or DIRECTOR
 */
public record InviteLeaderRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "LEADER|COORDONATOR|DIRECTOR") String role,
        @Size(max = 30) String phoneNumber) {
}
