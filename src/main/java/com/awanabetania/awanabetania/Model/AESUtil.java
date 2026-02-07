package com.awanabetania.awanabetania.Model;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESUtil {

    // 🔑 CHEIA SECRETA - Trebuie să aibă FIX 16 caractere!
    // Aceasta este cheia pe care o vei folosi si in aplicatia de Admin.
    private static final String SECRET_KEY = "AwanaBetania2026";

    private static final String ALGORITHM = "AES";

    // Metoda de CRIPTARE (Parola -> asd876asd876...)
    public static String encrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedByteValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedByteValue); // Returnam Base64 ca sa fie text citibil
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Metoda de DECRIPTARE (asd876asd876... -> Parola)
    // O vom folosi in viitor in aplicatia de Admin
    public static String decrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedByteValue = cipher.doFinal(Base64.getDecoder().decode(value));
            return new String(decryptedByteValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}