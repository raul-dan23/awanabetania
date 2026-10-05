package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotNull;

/**
 * The account owner's own password change. Length rules live in {@code PasswordService},
 * so registration, profile edits and this request share them.
 */
public record ChangePasswordRequest(@NotNull String currentPassword, @NotNull String newPassword) {
}
