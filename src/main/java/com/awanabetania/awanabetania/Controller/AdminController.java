package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.CardAssignedResponse;
import com.awanabetania.awanabetania.Dto.CardAssignmentRequest;
import com.awanabetania.awanabetania.Dto.MessageResponse;
import com.awanabetania.awanabetania.Dto.PasswordResetRequest;
import com.awanabetania.awanabetania.Dto.TemporaryPasswordResponse;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.AdminPinVerifier;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Service.CardService;
import com.awanabetania.awanabetania.Service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Control Center operations. Reaching them takes a director or coordinator login
 * ({@code SecurityConfig}) and the admin PIN ({@code X-Admin-Pin}); a wrong PIN is 403.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LeaderRepository leaderRepository;
    private final ChildRepository childRepository;
    private final CardService cardService;
    private final PasswordService passwordService;
    private final AdminPinVerifier pinVerifier;

    /** Checks the PIN before the Control Center unlocks. 200 "OK", or 403. */
    @PostMapping("/verify-pin")
    public String verifyPin(@RequestBody Map<String, String> payload) {
        pinVerifier.verify(payload.get("pin"));
        return "OK";
    }

    /** All leaders and children. */
    @GetMapping("/all-users")
    public Map<String, Object> allUsers(@RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        pinVerifier.verify(pin);
        Map<String, Object> response = new HashMap<>();
        response.put("leaders", leaderRepository.findAll());
        response.put("children", childRepository.findAll());
        return response;
    }

    /** Binds an NFC card to a child, taking it off any previous owner. 400, 403, 404. */
    @PostMapping("/nfc-register")
    public CardAssignedResponse assignCard(@RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                           @Valid @RequestBody CardAssignmentRequest request) {
        pinVerifier.verify(pin);
        Child child = cardService.assign(request.childId(), request.uid());
        return new CardAssignedResponse("Card associated.", child.getId(),
                child.getName() + " " + child.getSurname(), child.getNfcUid());
    }

    /** Removes a child's card. 403, 404. */
    @DeleteMapping("/nfc-remove/{childId}")
    public MessageResponse removeCard(@PathVariable Integer childId,
                                      @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        pinVerifier.verify(pin);
        cardService.remove(childId);
        return new MessageResponse("Card dissociated.");
    }

    /**
     * Gives an account a new random password, shown once to the director, which the owner
     * must replace at the next login. Passwords are stored as one-way hashes, so this is how
     * a forgotten password is handled. 400, 403, 404.
     */
    @PostMapping("/reset-password")
    public TemporaryPasswordResponse resetPassword(@RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                                  @Valid @RequestBody PasswordResetRequest request) {
        pinVerifier.verify(pin);
        return passwordService.reset(request.kind(), request.id(), AuthUser.current());
    }
}
