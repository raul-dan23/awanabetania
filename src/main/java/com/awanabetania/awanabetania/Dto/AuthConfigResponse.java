package com.awanabetania.awanabetania.Dto;

/**
 * What the login screen needs to know before anyone logs in.
 *
 * @param googleClientId the public client id for the Google button; null hides the button
 */
public record AuthConfigResponse(String googleClientId) {
}
