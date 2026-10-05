package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a leader (volunteer or director) of the Awana club.
 * Leaders can belong to one or more departments, hold different roles
 * (LEADER, COORDONATOR, DIRECTOR), and receive evaluations from directors.
 * Passwords are stored AES-encrypted.
 */
@Entity
@Table(name = "leaders")
@Getter
@Setter
@NoArgsConstructor
public class Leader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String role;

    /**
     * Departments this leader belongs to.
     * Fetched eagerly so department data is available during login without an extra query.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "leaders_departments",
            joinColumns = @JoinColumn(name = "leader_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    /** Average rating computed from visible evaluations; updated after every feedback save. */
    private Float rating = 0.0f;

    /**
     * Google address the director invited; "Continue with Google" lets this leader in.
     * Stored in lower case. A leader without one signs in with a password.
     */
    @Column(unique = true)
    private String email;

    /**
     * Google's permanent id for the account, bound at the first Google sign-in, so that a
     * later change of address on Google's side does not lock the leader out.
     */
    @JsonIgnore
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Accepted on input (login/registration) but never serialised back to the client.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Short deletion-confirmation code generated on demand; must match to allow account removal. */
    // Never serialised: knowing the code is what authorises the deletion.
    @JsonIgnore
    @Column(name = "deletion_code")
    private String deletionCode;

    /**
     * Set when a director resets the password: the temporary password works, but the app
     * asks for a new one right after login. Cleared when the account owner sets a password.
     */
    @JsonIgnore
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    /** Whether a Google account is bound; shown in the Control Center and the profile. */
    @JsonProperty(value = "googleLinked", access = JsonProperty.Access.READ_ONLY)
    public boolean isGoogleLinked() {
        return googleSub != null;
    }

    /**
     * Convenience constructor used by {@code DataInitializer} for seeding the admin account.
     *
     * @param name        first name
     * @param surname     last name
     * @param role        role string (e.g. "DIRECTOR")
     * @param password    plain-text password (will be stored as-is during seeding)
     * @param phoneNumber contact phone number
     */
    public Leader(String name, String surname, String role, String password, String phoneNumber) {
        this.name = name;
        this.surname = surname;
        this.role = role;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }
}
