package com.awanabetania.awanabetania.Model;

/**
 * Life cycle of a fair receipt: created {@link #PENDING}, then exactly once either
 * {@link #APPROVED} (points deducted) or {@link #REJECTED}. Both end states are final.
 */
public enum BonStatus {
    PENDING,
    APPROVED,
    REJECTED
}
