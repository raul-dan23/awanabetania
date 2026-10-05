package com.awanabetania.awanabetania.Dto;

/** Result of spending points with a card. */
public record SpendResponse(String message, int spent, int remainingPoints, String name) {
}
