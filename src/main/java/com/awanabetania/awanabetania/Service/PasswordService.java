package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Dto.TemporaryPasswordResponse;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.AccountKind;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Passwords: the length rule, the owner's own change, and the director's reset.
 * <p>
 * Passwords are stored only as BCrypt hashes, which cannot be turned back into the
 * password. A forgotten password is therefore never shown; the director resets it to a
 * temporary one instead, which the owner must replace at the next login.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    public static final int MIN_LENGTH = 6;
    /** BCrypt reads at most 72 bytes; 64 characters stays clear of that with diacritics. */
    public static final int MAX_LENGTH = 64;

    /** No look-alikes (0/o, 1/l/i), so a password read out over the phone is typed right. */
    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private final ChildRepository childRepository;
    private final LeaderRepository leaderRepository;
    private final PasswordEncoder passwordEncoder;

    /** @throws ApiException 400 unless the password has {@value MIN_LENGTH}–{@value MAX_LENGTH} characters */
    public static void requireValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw ApiException.badRequest("The password must have between " + MIN_LENGTH
                    + " and " + MAX_LENGTH + " characters.");
        }
    }

    /** The hash to store for a password the account owner chose. */
    public String hash(String password) {
        requireValid(password);
        return passwordEncoder.encode(password);
    }

    /**
     * Gives the account a new random password and asks for a new one at the next login.
     * The reset is logged with who did it.
     *
     * @throws ApiException 404 for an unknown account
     */
    @Transactional
    public TemporaryPasswordResponse reset(AccountKind kind, Integer id, AuthUser director) {
        String temporary = generate();
        String username;
        if (kind == AccountKind.CHILD) {
            Child child = childRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Child not found."));
            child.setPassword(passwordEncoder.encode(temporary));
            child.setPasswordChangeRequired(true);
            username = childRepository.save(child).getUsername();
        } else {
            Leader leader = leaderRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Leader not found."));
            setTemporary(leader, temporary);
            username = leaderRepository.save(leader).getUsername();
        }
        log.info("Password reset for {} #{} ({}) by leader #{}", kind, id, username, director.id());
        return new TemporaryPasswordResponse(username, temporary);
    }

    /**
     * Gives a new leader account a random password that must be replaced at the first login
     * (the caller saves the leader), and returns it to be shown once.
     */
    public String issueTemporary(Leader leader) {
        String temporary = generate();
        setTemporary(leader, temporary);
        return temporary;
    }

    private void setTemporary(Leader leader, String temporary) {
        leader.setPassword(passwordEncoder.encode(temporary));
        leader.setPasswordChangeRequired(true);
    }

    /**
     * The caller replaces their own password, proving they know the current one, so a
     * session left open on a shared device cannot be used to lock its owner out.
     *
     * @throws ApiException 400 new password too short or long; 403 current password wrong
     */
    @Transactional
    public void change(AuthUser me, String currentPassword, String newPassword) {
        if (me.isChild()) {
            Child child = childRepository.findById(me.id()).orElseThrow();
            checkCurrent(child.getPassword(), currentPassword);
            child.setPassword(hash(newPassword));
            child.setPasswordChangeRequired(false);
            childRepository.save(child);
        } else {
            Leader leader = leaderRepository.findById(me.id()).orElseThrow();
            checkCurrent(leader.getPassword(), currentPassword);
            leader.setPassword(hash(newPassword));
            leader.setPasswordChangeRequired(false);
            leaderRepository.save(leader);
        }
    }

    private void checkCurrent(String storedHash, String candidate) {
        // 403, not 401: the browser client logs the user out on 401.
        if (storedHash == null || !storedHash.startsWith("$2") || !passwordEncoder.matches(candidate, storedHash)) {
            throw ApiException.forbidden("The current password is not correct.");
        }
    }

    /** Eight characters in two groups, e.g. {@code k7ma-p3xh}: about 40 bits, enough for a one-time password. */
    static String generate() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
