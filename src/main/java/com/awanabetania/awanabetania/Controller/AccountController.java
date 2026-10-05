package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.ChangePasswordRequest;
import com.awanabetania.awanabetania.Dto.GoogleCredentialRequest;
import com.awanabetania.awanabetania.Dto.LeaderAccountResponse;
import com.awanabetania.awanabetania.Dto.MessageResponse;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Service.GoogleSignInService;
import com.awanabetania.awanabetania.Service.PasswordService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AccountController {

    private final LeaderRepository leaderRepository;
    private final ChildRepository childRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordService passwordService;
    private final GoogleSignInService googleSignInService;

    /**
     * Links the caller's Google account to their leader account, so they can use
     * "Continue with Google" from now on. Leaders only (SecurityConfig). 403 invalid token,
     * 404 Google sign-in not configured, 409 Google account or address used by another leader.
     */
    @PostMapping("/google")
    public LeaderAccountResponse linkGoogle(@Valid @RequestBody GoogleCredentialRequest request) {
        return LeaderAccountResponse.from(googleSignInService.link(AuthUser.current(), request.credential()));
    }

    /**
     * The caller replaces their own password; also how a temporary password from a reset is
     * replaced. 400 new password too short or long; 403 current password wrong.
     */
    @PostMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passwordService.change(AuthUser.current(), request.currentPassword(), request.newPassword());
        return new MessageResponse("Password changed.");
    }

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
