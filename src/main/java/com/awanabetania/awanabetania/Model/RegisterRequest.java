package com.awanabetania.awanabetania.Model;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Aceasta clasa este formularul de inregistrare (Sign Up).
 * Primeste toate datele necesare cand cineva vrea sa isi faca un cont nou.
 * Functioneaza atat pentru Copii cat si pentru Lideri.
 */
@Data
public class RegisterRequest {

    // --- Campuri pentru lideiri si copii ---

    /** Numele utilizatorului */
    private String name;

    /** Prenumele utilizatorului */
    private String surname;

    /** Parola aleasa pentru cont */
    private String password;

    /** Ce fel de cont este: CHILD (Copil) sau LEADER (Lider) */
    private String role;

    // --- Doar pentru lideiri ---

    /** Numarul de telefon al liderului */
    private String phoneNumber;

    /** Lista cu departamentele unde vrea sa slujeasca (bifate in formular) */
    private List<Integer> departmentIds;

    /** Codul secret (din fisierul codes.txt) ca sa nu se inregistreze oricine ca sef */
    private String registrationCode;

    // --- Doar pentru copii ---

    /** Data nasterii copilului */
    private LocalDate birthDate;

    /** Numele parintelui */
    private String parentName;

    /** Telefonul parintelui */
    private String parentPhone;
}