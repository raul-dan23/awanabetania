package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Handles self-service account deletion requests.
 * The flow is two-step: the user requests deletion here, which generates a random
 * 6-character confirmation code stored on their account and sends a director notification.
 * The director shares the code with the user, who then calls the delete endpoint
 * on {@code ChildController} or {@code LeaderController} to complete the deletion.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * Initiates an account deletion request for the caller's own account. Generates a deletion
     * code, stores it on the account, and creates a director notification containing the code.
     * The primary admin account (leader ID=1) cannot be deleted this way.
     * <p>
     * The account is taken from the token. The "id" and "role" fields the client still sends
     * are ignored: trusting them let anyone start a deletion for any account.
     *
     * @param payload ignored; kept so existing clients keep working
     * @return 200 with a message instructing the user to contact the director;
     *         400 if the account does not exist or is the protected admin account
     */
    @PostMapping("/request-deletion")
    @Transactional
    public ResponseEntity<?> requestDeletion(@RequestBody Map<String, Object> payload) {
        AuthUser me = AuthUser.current();
        Integer id = me.id();
        String role = me.kind();

        if ("LEADER".equalsIgnoreCase(role) && id == 1) {
            return ResponseEntity.badRequest().body("The primary administrator account cannot be deleted.");
        }

        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String userName;

        if ("CHILD".equalsIgnoreCase(role)) {
            Child c = childRepository.findById(id).orElse(null);
            if (c == null) return ResponseEntity.badRequest().body("Child not found.");
            c.setDeletionCode(code);
            childRepository.save(c);
            userName = c.getName() + " " + c.getSurname() + " (Child)";
        } else {
            Leader l = leaderRepository.findById(id).orElse(null);
            if (l == null) return ResponseEntity.badRequest().body("Leader not found.");
            l.setDeletionCode(code);
            leaderRepository.save(l);
            userName = l.getName() + " " + l.getSurname() + " (Leader)";
        }

        String adminMsg = String.format("DELETION REQUEST: %s. Confirmation code: %s", userName, code);
        Notification n = new Notification();
        n.setMessage(adminMsg);
        n.setType("ALERT");
        n.setVisibleTo("DIRECTOR");
        n.setDate(LocalDate.now());
        n.setIsVisible(true);
        notificationRepository.save(n);

        return ResponseEntity.ok("Request submitted. Contact the director for the confirmation code.");
    }
}
