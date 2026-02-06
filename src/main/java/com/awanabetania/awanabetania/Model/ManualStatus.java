package com.awanabetania.awanabetania.Model;

/**
 * Definește stările posibile ale unui manual.
 * Acest fișier este necesar pentru ca ChildManual și ChildController să funcționeze.
 */
public enum ManualStatus {
    /** Manualul este curent, copilul lucrează la el. */
    ACTIVE,

    /** Manualul a fost terminat cu succes. */
    COMPLETED,

    /** Manualul a fost pierdut sau anulat. */
    LOST
}