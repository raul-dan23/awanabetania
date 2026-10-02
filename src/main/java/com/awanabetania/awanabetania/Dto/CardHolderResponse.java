package com.awanabetania.awanabetania.Dto;

/** The owner of a scanned card and their spendable balance. */
public record CardHolderResponse(Integer id, String name, String surname, int seasonPoints, String uid) {
}
