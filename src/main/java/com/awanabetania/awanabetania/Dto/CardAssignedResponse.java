package com.awanabetania.awanabetania.Dto;

/** A card now bound to {@code childId}. */
public record CardAssignedResponse(String message, Integer childId, String name, String uid) {
}
