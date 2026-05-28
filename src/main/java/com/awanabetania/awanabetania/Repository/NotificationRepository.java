package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for {@link Notification} entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /**
     * Returns all visible notifications addressed to the given audience string
     * (e.g. "ALL", "DIRECTOR", or a specific leader ID), ordered newest first.
     */
    @Query("SELECT n FROM Notification n WHERE n.visibleTo = ?1 AND n.isVisible = true ORDER BY n.id DESC")
    List<Notification> findByVisibleTo(String role);

    /**
     * Returns all visible notifications of a given type that are linked to a specific child.
     * Used to prevent duplicate reward notifications.
     */
    @Query("SELECT n FROM Notification n WHERE n.childId = ?1 AND n.type = ?2 AND n.isVisible = true")
    List<Notification> findActiveByChildAndType(Integer childId, String type);

    /**
     * Returns all visible notifications addressed to a specific leader ID or to everyone ("ALL"),
     * ordered newest first. Used by the notification bell in the frontend.
     *
     * @param leaderId the string representation of the leader's ID
     * @param all      the broadcast audience constant ("ALL")
     */
    @Query("SELECT n FROM Notification n WHERE (n.visibleTo = ?1 OR n.visibleTo = ?2) AND n.isVisible = true ORDER BY n.id DESC")
    List<Notification> findMyActiveNotifications(String leaderId, String all);

    /** Deletes all notifications associated with the given child ID. */
    void deleteByChildId(Integer childId);

    /** Deletes all notifications addressed to the given audience string. */
    void deleteByVisibleTo(String visibleTo);
}
