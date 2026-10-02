package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.CardAssignedResponse;
import com.awanabetania.awanabetania.Dto.CardAssignmentRequest;
import com.awanabetania.awanabetania.Dto.MessageResponse;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.AESUtil;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.AdminPinVerifier;
import com.awanabetania.awanabetania.Service.CardService;
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
     * Decrypts a legacy AES password for the admin panel. Accounts migrated to BCrypt
     * cannot be decrypted (by design); for those this answers 400.
     */
    @PostMapping("/decrypt-password")
    public Map<String, String> decryptPassword(@RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                               @RequestBody Map<String, String> payload) {
        pinVerifier.verify(pin);
        String encrypted = payload.get("password");
        if (encrypted == null || encrypted.isEmpty()) {
            throw ApiException.badRequest("No encrypted password provided.");
        }
        String plain = AESUtil.decrypt(encrypted);
        if (plain == null) {
            throw ApiException.badRequest("This password cannot be decrypted.");
        }
        return Map.of("realPassword", plain);
    }
}
