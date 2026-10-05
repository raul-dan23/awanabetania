package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A shop product to create or update.
 *
 * @param available ignored on creation (new products are available); on update, omitting it
 *                  keeps the current value
 */
public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @PositiveOrZero Integer pointPrice,
        @Size(max = 255) String category,
        Boolean available) {
}
