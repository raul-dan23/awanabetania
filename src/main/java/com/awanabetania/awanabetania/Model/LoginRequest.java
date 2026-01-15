package com.awanabetania.awanabetania.Model;

import lombok.Data;

/**
 * Aceasta clasa este ca un pachet simplu de date.
 * O folosim cand cineva incearca sa se logheze in aplicatie.
 * Ea primeste datele din formularul de pe site (React) si le aduce in Java.
 */
@Data
public class LoginRequest {

    /** Numele sau userul cu care vrea sa intre in cont */
    private String username;

    /** Parola secreta introdusa */
    private String password;

    /** Rolul selectat (Lider, Director sau Copil) pentru a sti unde sa il trimitem */
    private String role;
}