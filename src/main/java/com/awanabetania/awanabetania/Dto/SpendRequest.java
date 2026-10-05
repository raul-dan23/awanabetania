package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Points to take from the card holder's season balance. */
public record SpendRequest(@NotNull @Positive Integer amount) {
}
