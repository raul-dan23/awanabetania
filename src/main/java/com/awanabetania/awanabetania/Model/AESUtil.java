package com.awanabetania.awanabetania.Model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility component for symmetric AES-128 encryption and decryption of passwords.
 * The secret key is injected from the {@code aes.secret.key} application property,
 * which should be sourced from the {@code AES_SECRET_KEY} environment variable.
 * The key must be exactly 16 bytes (128-bit) when UTF-8 encoded.
 */
@Component
public class AESUtil {

    private static String SECRET_KEY;

    private static final String ALGORITHM = "AES";

    /**
     * Injects the AES secret key from application properties into the static field.
     * Spring calls this setter after bean creation.
     *
     * @param key the 16-byte AES secret key
     */
    @Value("${aes.secret.key}")
    public void setSecretKey(String key) {
        AESUtil.SECRET_KEY = key;
    }

    /**
     * Encrypts a plain-text value with AES-128 and returns a Base64-encoded ciphertext.
     *
     * @param value the plain-text string to encrypt
     * @return Base64-encoded encrypted string, or {@code null} if encryption fails
     */
    public static String encrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts a Base64-encoded AES-128 ciphertext back to plain text.
     *
     * @param value the Base64-encoded encrypted string
     * @return the decrypted plain-text string, or {@code null} if decryption fails
     */
    public static String decrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(value));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
