package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Binds an NFC card to a child.
 *
 * @param childId the card's new owner
 * @param uid     the card's hardware UID as the reader reports it, e.g. {@code A1B2C3D4}
 */
public record CardAssignmentRequest(
        @NotNull Integer childId,
        @NotBlank @Size(max = 64) String uid) {
}
