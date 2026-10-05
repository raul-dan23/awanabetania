package com.awanabetania.awanabetania.Dto;

/**
 * A freshly generated password, shown once to the director to pass on. It is not stored
 * anywhere in readable form; the owner must replace it at their next login.
 */
public record TemporaryPasswordResponse(String username, String temporaryPassword) {
}
