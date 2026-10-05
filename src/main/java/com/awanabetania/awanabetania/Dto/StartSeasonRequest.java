package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Closes the current season and starts a new one. {@code currentSeasonId} is the season the
 * director saw when confirming: if it is no longer the current one (a second click, or
 * another director was faster), nothing happens.
 */
public record StartSeasonRequest(@NotNull Integer currentSeasonId,
                                 @NotBlank @Size(max = 60) String name) {
}
