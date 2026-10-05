package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSeasonRequest(@NotBlank @Size(max = 60) String name) {
}
