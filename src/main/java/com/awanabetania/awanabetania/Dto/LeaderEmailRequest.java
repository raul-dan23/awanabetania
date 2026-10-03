package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** A leader's Google address; empty or null removes it (Google sign-in stops working for them). */
public record LeaderEmailRequest(@Email @Size(max = 255) String email) {
}
