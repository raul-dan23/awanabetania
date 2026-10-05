package com.awanabetania.awanabetania.Account;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Forgotten passwords: the director resets an account to a temporary password (passwords
 * are one-way hashes, so they are never shown), and the owner replaces it after logging in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetTest {

    private static final String PIN = "0000";

    @Autowired private MockMvc mvc;
    @Autowired private ChildRepository childRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private final ObjectMapper json = new ObjectMapper();

    private Child child;
    private Leader leader;
    private String directorToken;

    @BeforeEach
    void seed() throws Exception {
        childRepository.findByUsername("reset.child").ifPresent(childRepository::delete);
        Child c = new Child();
        c.setName("Ana");
        c.setSurname("Forgetful");
        c.setUsername("reset.child");
        c.setPassword(passwordEncoder.encode("old-password"));
        child = childRepository.save(c);

        leader = saveLeader("reset.leader", "LEADER", "leader-password");
        saveLeader("reset.director", "Director", "director-password");
        directorToken = token(login("reset.director", "director-password", "LEADER"));
    }

    private Leader saveLeader(String username, String role, String password) {
        leaderRepository.findByUsername(username).ifPresent(l -> {
            l.getDepartments().clear();
            leaderRepository.delete(l);
        });
        Leader l = new Leader();
        l.setUsername(username);
        l.setName(username);
        l.setSurname("Test");
        l.setRole(role);
        l.setPassword(passwordEncoder.encode(password));
        return leaderRepository.save(l);
    }

    private ResultActions login(String username, String password, String role) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}".formatted(username, password, role)));
    }

    private String token(ResultActions login) throws Exception {
        return body(login.andExpect(status().isOk())).get("token").asText();
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    private String reset(String kind, int id) throws Exception {
        JsonNode response = body(call(post("/api/admin/reset-password").header("X-Admin-Pin", PIN), directorToken,
                "{\"kind\":\"%s\",\"id\":%d}".formatted(kind, id))
                .andExpect(status().isOk()));
        return response.get("temporaryPassword").asText();
    }

    @Test
    @DisplayName("a reset child logs in with the temporary password and must choose a new one")
    void childResetAndChange() throws Exception {
        String temporary = reset("CHILD", child.getId());
        assertThat(temporary).matches("[a-z2-9]{4}-[a-z2-9]{4}");

        login("reset.child", "old-password", "CHILD").andExpect(status().isUnauthorized());
        JsonNode session = body(login("reset.child", temporary, "CHILD").andExpect(status().isOk()));
        assertThat(session.get("mustChangePassword").asBoolean()).isTrue();
        String kid = session.get("token").asText();

        call(post("/api/account/password"), kid,
                "{\"currentPassword\":\"%s\",\"newPassword\":\"my-own-pass\"}".formatted(temporary))
                .andExpect(status().isOk());

        login("reset.child", temporary, "CHILD").andExpect(status().isUnauthorized());
        JsonNode after = body(login("reset.child", "my-own-pass", "CHILD").andExpect(status().isOk()));
        assertThat(after.get("mustChangePassword").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("a leader can be reset the same way")
    void leaderReset() throws Exception {
        String temporary = reset("LEADER", leader.getId());
        JsonNode session = body(login("reset.leader", temporary, "LEADER").andExpect(status().isOk()));
        assertThat(session.get("mustChangePassword").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("changing a password needs the current one and a valid new one; mistakes keep the session")
    void changeValidation() throws Exception {
        String kid = token(login("reset.child", "old-password", "CHILD"));

        call(post("/api/account/password"), kid, "{\"currentPassword\":\"wrong\",\"newPassword\":\"long-enough\"}")
                .andExpect(status().isForbidden());
        call(post("/api/account/password"), kid, "{\"currentPassword\":\"old-password\",\"newPassword\":\"abc\"}")
                .andExpect(status().isBadRequest());
        call(post("/api/account/password"), kid, "{\"currentPassword\":\"old-password\"}")
                .andExpect(status().isBadRequest());

        // Still logged in after the failed attempts.
        call(get("/api/children/" + child.getId()), kid, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("only a director with the PIN can reset, and only existing accounts")
    void resetAccess() throws Exception {
        String body = "{\"kind\":\"CHILD\",\"id\":%d}".formatted(child.getId());

        call(post("/api/admin/reset-password").header("X-Admin-Pin", "9999"), directorToken, body)
                .andExpect(status().isForbidden());
        String leaderToken = token(login("reset.leader", "leader-password", "LEADER"));
        call(post("/api/admin/reset-password").header("X-Admin-Pin", PIN), leaderToken, body)
                .andExpect(status().isForbidden());
        String kid = token(login("reset.child", "old-password", "CHILD"));
        call(post("/api/admin/reset-password").header("X-Admin-Pin", PIN), kid, body)
                .andExpect(status().isForbidden());

        call(post("/api/admin/reset-password").header("X-Admin-Pin", PIN), directorToken,
                "{\"kind\":\"CHILD\",\"id\":999999}").andExpect(status().isNotFound());
        call(post("/api/admin/reset-password").header("X-Admin-Pin", PIN), directorToken,
                "{\"kind\":\"ROBOT\",\"id\":1}").andExpect(status().isBadRequest());

        login("reset.child", "old-password", "CHILD").andExpect(status().isOk());
    }

    @Test
    @DisplayName("passwords can no longer be decrypted: the endpoint is gone")
    void decryptEndpointRemoved() throws Exception {
        call(post("/api/admin/decrypt-password").header("X-Admin-Pin", PIN), directorToken,
                "{\"password\":\"anything\"}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a password set from the profile is hashed and clears a pending reset")
    void profilePasswordChange() throws Exception {
        String temporary = reset("CHILD", child.getId());
        String kid = token(login("reset.child", temporary, "CHILD"));

        call(put("/api/children/" + child.getId()), kid,
                "{\"name\":\"Ana\",\"surname\":\"Forgetful\",\"username\":\"reset.child\",\"password\":\"from-profile\"}")
                .andExpect(status().isOk());

        Child stored = childRepository.findById(child.getId()).orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2");
        assertThat(stored.isPasswordChangeRequired()).isFalse();
        call(put("/api/children/" + child.getId()), kid,
                "{\"name\":\"Ana\",\"surname\":\"Forgetful\",\"username\":\"reset.child\",\"password\":\"abc\"}")
                .andExpect(status().isBadRequest());
    }
}
