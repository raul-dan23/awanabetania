package com.awanabetania.awanabetania.Model;

/**
 * Possible lifecycle states for a {@link ChildManual}.
 */
public enum ManualStatus {

    /** The child is currently working through this manual. */
    ACTIVE,

    /** The child has finished all lessons in this manual. */
    COMPLETED,

    /** The manual was lost or voided; no longer in use. */
    LOST
}
