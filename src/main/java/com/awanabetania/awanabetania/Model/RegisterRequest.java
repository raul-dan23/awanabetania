package com.awanabetania.awanabetania.Model;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO carrying new-account data from the registration form to {@code AuthController}.
 * Used for both Child and Leader registrations; role-specific fields are populated
 * only when {@code role} matches the corresponding type.
 */
@Data
public class RegisterRequest {

    /** First name (used for both children and leaders). */
    private String name;

    /** Last name (used for both children and leaders). */
    private String surname;

    /** Plain-text password chosen by the user (AES-encrypted before storage). */
    private String password;

    /**
     * Account type being created: "CHILD" or "LEADER".
     * For leaders, this value is also stored as the {@code role} field on the entity.
     */
    private String role;

    // --- Leader-only fields ---

    /** Leader's contact phone number. */
    private String phoneNumber;

    /** IDs of departments the new leader wishes to join. */
    private List<Integer> departmentIds;

    /** Registration code required for leader sign-up (validated against a hard-coded list). */
    private String registrationCode;

    // --- Child-only fields ---

    /** Child's date of birth (used to calculate age). */
    private LocalDate birthDate;

    /** Full name of the child's parent or guardian. */
    private String parentName;

    /** Contact phone number for the parent or guardian. */
    private String parentPhone;
}
