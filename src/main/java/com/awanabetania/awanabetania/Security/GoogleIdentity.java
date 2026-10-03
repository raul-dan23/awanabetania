package com.awanabetania.awanabetania.Security;

/**
 * Who Google says signed in, taken from a verified ID token.
 *
 * @param subject Google's permanent account id ({@code sub})
 * @param email   the verified address, lower case
 * @param name    display name, may be null
 */
public record GoogleIdentity(String subject, String email, String name) {
}
