package com.awanabetania.awanabetania.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A new fair receipt. The seller is not part of the request: it is the logged-in leader.
 *
 * @param childId     the buying child
 * @param items       the receipt lines as the shop screen sends them (a JSON string)
 * @param totalPoints the price in points; must be positive, or approving it would add points
 */
public record CreateBonRequest(
        @NotNull Integer childId,
        @NotNull @Size(max = 20_000) String items,
        @NotNull @Positive Integer totalPoints) {
}
