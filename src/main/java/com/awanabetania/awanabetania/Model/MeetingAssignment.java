package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the assignment of a leader to a department for a specific meeting evening.
 * An assignment can be created as a nomination (status "PENDING") by the director,
 * or as a direct assignment (status "ACCEPTED"). The leader can accept or decline
 * a pending nomination, which either sets the status to "ACCEPTED" or deletes the record.
 */
@Entity
@Table(name = "meeting_assignments")
@Getter
@Setter
@NoArgsConstructor
public class MeetingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The meeting evening for which this assignment is planned. */
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    /** The department the leader will serve in during this meeting. */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /** The leader who is assigned or nominated. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leader_id")
    private Leader leader;

    /** Assignment status: "PENDING" (nominated), "ACCEPTED", or "DECLINED" (deleted on decline). */
    private String status;
}
