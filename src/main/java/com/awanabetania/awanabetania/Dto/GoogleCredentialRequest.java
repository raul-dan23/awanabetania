package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The ID token the Google button returns to the browser ({@code response.credential}). */
public record GoogleCredentialRequest(@NotBlank @Size(max = 4096) String credential) {
}
