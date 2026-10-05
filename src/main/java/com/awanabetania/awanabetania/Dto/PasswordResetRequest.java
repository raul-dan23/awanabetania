package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.AccountKind;
import jakarta.validation.constraints.NotNull;

/** The account whose password the director resets. */
public record PasswordResetRequest(@NotNull AccountKind kind, @NotNull Integer id) {
}
