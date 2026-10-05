package com.awanabetania.awanabetania.Security;

import com.awanabetania.awanabetania.Exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Checks the ID token that "Sign in with Google" hands the browser.
 * <p>
 * The token is a JWT signed by Google. It is accepted only if the signature matches
 * Google's published keys, it was issued by Google, for this application's client id
 * (so a token obtained by another site cannot be replayed here), it has not expired,
 * and Google has verified the email address.
 * <p>
 * Only the client id is needed; the client secret plays no part in this flow.
 */
@Component
public class GoogleIdTokenVerifier {

    public static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifier.class);

    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleIdTokenVerifier(@Value("${auth.google.client-id:}") String clientId, JwtDecoder googleIdTokenDecoder) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.decoder = googleIdTokenDecoder;
    }

    /** Google sign-in is offered only once a client id is configured. */
    public boolean enabled() {
        return !clientId.isEmpty();
    }

    /** The public client id, for the browser's Google button; null when not configured. */
    public String clientId() {
        return enabled() ? clientId : null;
    }

    /**
     * @throws ApiException 404 when Google sign-in is not configured; 403 when the token is
     *                      invalid or the address unverified (never 401: the browser client
     *                      would log the user out)
     */
    public GoogleIdentity verify(String credential) {
        if (!enabled()) {
            throw ApiException.notFound("Google sign-in is not configured.");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(credential);
        } catch (JwtException e) {
            log.warn("Rejected Google ID token: {}", e.getMessage());
            throw ApiException.forbidden("Google sign-in failed. Please try again.");
        }

        Object verified = jwt.getClaim("email_verified");
        String email = jwt.getClaimAsString("email");
        if (!(Boolean.TRUE.equals(verified) || "true".equals(verified)) || email == null || email.isBlank()) {
            throw ApiException.forbidden("Google has not verified this email address.");
        }
        return new GoogleIdentity(jwt.getSubject(), email.trim().toLowerCase(Locale.ROOT), jwt.getClaimAsString("name"));
    }

    /** Issuer, audience and expiry checks applied on top of the signature. */
    public static OAuth2TokenValidator<Jwt> validator(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Not issued by Google", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Issued for another application", null));
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience);
    }
}
