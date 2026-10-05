package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.DataInitializer;
import com.awanabetania.awanabetania.Dto.InviteLeaderRequest;
import com.awanabetania.awanabetania.Dto.InvitedLeaderResponse;
import com.awanabetania.awanabetania.Dto.LeaderAccountResponse;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Security.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Leader accounts as the director manages them. Leaders no longer register themselves with
 * a code (whoever had a code could register as director); the director adds them with
 * their role and, for "Continue with Google", their Google address.
 */
@Service
@RequiredArgsConstructor
public class LeaderAccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(LeaderAccountService.class);

    private final LeaderRepository leaderRepository;
    private final PasswordService passwordService;
    private final GoogleIdTokenVerifier googleVerifier;

    /**
     * Adds a leader. With a Google address and Google sign-in configured, they sign in with
     * Google and get no password. Otherwise they get a temporary password, returned once for
     * the director to pass on, which they replace at the first login; once Google is set up
     * they can link it from their profile.
     *
     * @throws ApiException 409 when the address is already used
     */
    @Transactional
    public InvitedLeaderResponse invite(InviteLeaderRequest request, AuthUser director) {
        String email = request.email() == null || request.email().isBlank() ? null : normalize(request.email());
        if (email != null && leaderRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict("Another leader already uses " + email + ".");
        }
        Leader leader = new Leader();
        leader.setName(request.name().trim());
        leader.setSurname(request.surname().trim());
        leader.setRole(request.role());
        leader.setEmail(email);
        leader.setPhoneNumber(request.phoneNumber());
        leader.setUsername(uniqueUsername(leader.getName(), leader.getSurname()));
        leader.setRating(0.0f);
        String temporary = email == null || !googleVerifier.enabled() ? passwordService.issueTemporary(leader) : null;
        Leader saved = leaderRepository.save(leader);
        log.info("Leader #{} ({}) added by leader #{}, signing in with {}", saved.getId(), saved.getUsername(),
                director.id(), temporary == null ? "Google" : "a temporary password");
        return InvitedLeaderResponse.from(saved, temporary);
    }

    /**
     * Sets or removes a leader's Google address. Changing it also unbinds the Google
     * account used so far, so only the new address can sign in.
     *
     * @throws ApiException 404 unknown leader; 409 address used by another leader
     */
    @Transactional
    public LeaderAccountResponse setEmail(Integer leaderId, String newEmail) {
        Leader leader = leaderRepository.findById(leaderId)
                .orElseThrow(() -> ApiException.notFound("Leader not found."));
        String email = newEmail == null || newEmail.isBlank() ? null : normalize(newEmail);

        if (email != null) {
            leaderRepository.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getId().equals(leaderId))
                    .ifPresent(other -> { throw ApiException.conflict("Another leader already uses " + email + "."); });
        }
        if (email == null ? leader.getEmail() != null : !email.equals(leader.getEmail())) {
            leader.setGoogleSub(null);
        }
        leader.setEmail(email);
        return LeaderAccountResponse.from(leaderRepository.save(leader));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String uniqueUsername(String name, String surname) {
        String base = DataInitializer.generateCleanUsername(name, surname);
        String candidate = base;
        while (leaderRepository.findByUsername(candidate).isPresent()) {
            candidate = base + RANDOM.nextInt(1000);
        }
        return candidate;
    }
}
