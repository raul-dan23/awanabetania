package com.awanabetania.awanabetania.Security;

import com.awanabetania.awanabetania.Exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the admin PIN ({@code X-Admin-Pin}) that guards the Control Center, product
 * management and Olympiad sessions, on top of the caller's login.
 * <p>
 * A wrong PIN is answered with 403, not 401: the browser client treats 401 as an expired
 * session and logs the user out, which used to happen to anyone who mistyped the PIN.
 */
@Component
public class AdminPinVerifier {

    private final String pin;

    public AdminPinVerifier(@Value("${admin.pin}") String pin) {
        this.pin = pin;
    }

    /** @throws ApiException 403 when the PIN is missing or wrong */
    public void verify(String candidate) {
        if (!SharedSecrets.matches(pin, candidate)) {
            throw ApiException.forbidden("Incorrect PIN.");
        }
    }
}
