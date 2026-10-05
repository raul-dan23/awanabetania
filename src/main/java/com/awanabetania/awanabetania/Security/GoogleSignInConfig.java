package com.awanabetania.awanabetania.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decoder for Google ID tokens. Google's signing keys are downloaded on first use and
 * cached; Google rotates them regularly, and the decoder fetches new ones when it meets a
 * key it does not know.
 */
@Configuration
public class GoogleSignInConfig {

    @Bean
    public JwtDecoder googleIdTokenDecoder(@Value("${auth.google.client-id:}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GoogleIdTokenVerifier.GOOGLE_KEYS).build();
        decoder.setJwtValidator(GoogleIdTokenVerifier.validator(clientId == null ? "" : clientId.trim()));
        return decoder;
    }
}
