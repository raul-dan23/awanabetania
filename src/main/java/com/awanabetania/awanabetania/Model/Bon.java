package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Represents a purchase receipt (bon) created at the end-of-season fair.
 * A selling leader creates the bon with a list of items and a point total; a cashier leader
 * then approves or rejects it after verifying the child's NFC card. Approval deducts
 * {@code totalPoints} from the child's {@code seasonPoints} balance.
 *
 * <p>Status transitions: PENDING → APPROVED or PENDING → REJECTED.</p>
 */
@Entity
@Table(name = "bons")
@Getter @Setter @NoArgsConstructor
public class Bon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The season this row belongs to (see {@link Season}); set by the server, never by the client. */
    @JsonIgnore
    @Column(name = "season_id")
    private Integer seasonId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "child_id")
    @JsonIgnoreProperties({"manuals", "progress", "password", "deletionCode", "hibernateLazyInitializer"})
    private Child child;

    /** Name of the leader who created this receipt. */
    @Column(name = "leader_name")
    private String leaderName;

    /** JSON-encoded list of selected products. */
    @Column(columnDefinition = "TEXT")
    private String items;

    /** Total point cost of all items in this receipt. */
    @Column(name = "total_points")
    private Integer totalPoints;

    /** Current status: PENDING, APPROVED, or REJECTED. */
    // Stored as text in the existing VARCHAR column (Hibernate would otherwise expect a
    // MySQL ENUM column, and schema validation would fail).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private BonStatus status = BonStatus.PENDING;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Timestamp when the receipt was approved; {@code null} until approved. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
