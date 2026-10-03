package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A leader the director adds; they sign in with the Google account at {@code email}.
 *
 * @param role LEADER, COORDONATOR or DIRECTOR
 */
public record InviteLeaderRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "LEADER|COORDONATOR|DIRECTOR") String role,
        @Size(max = 30) String phoneNumber) {
}
