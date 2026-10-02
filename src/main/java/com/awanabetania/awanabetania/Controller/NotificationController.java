package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Provides notification management for leaders: listing their active notifications,
 * creating manual notifications, and soft-deleting (dismissing) individual entries.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Returns all active notifications visible to the calling leader,
     * including public ("ALL") and personal (leader's own ID) entries.
     * <p>
     * The leader comes from the token. The {@code leaderId} parameter is ignored: passing
     * {@code leaderId=DIRECTOR} used to return the director-only feed, deletion codes included.
     *
     * @param leaderId ignored; kept so existing clients keep working
     * @return list of active {@link Notification} records
     */
    @GetMapping
    public List<Notification> getMyNotifications(@RequestParam(required = false) String leaderId) {
        return notificationRepository.findMyActiveNotifications(
                String.valueOf(AuthUser.current().id()), "ALL");
    }

    /**
     * Creates a manual notification. Used for testing or direct injection from the frontend.
     * The date is always set to today; {@code isVisible} defaults to {@code true}.
     *
     * @param notification notification data from the request body
     * @return the saved {@link Notification} entity
     */
    @PostMapping("/add")
    public Notification addNotification(@RequestBody Notification notification) {
        notification.setDate(LocalDate.now());
        notification.setIsVisible(true);
        return notificationRepository.save(notification);
    }

    /**
     * Soft-deletes a notification by setting {@code isVisible=false}.
     * The notification remains in the database but no longer appears in the feed.
     *
     * @param id the notification's primary key
     * @return 200 on success; 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Integer id) {
        return notificationRepository.findById(id)
                .map(notification -> {
                    notification.setIsVisible(false);
                    notificationRepository.save(notification);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
