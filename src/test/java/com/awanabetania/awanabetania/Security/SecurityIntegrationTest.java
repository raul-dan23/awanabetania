package com.awanabetania.awanabetania.Security;

import com.awanabetania.awanabetania.Model.AESUtil;
import com.awanabetania.awanabetania.Model.Leader;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the guarantees introduced with JWT authentication: protected endpoints reject
 * anonymous callers, login issues a usable token without leaking credential material, and
 * pre-existing AES passwords keep working while being migrated to BCrypt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaderRepository;
    private final ObjectMapper json = new ObjectMapper();

    /** A leader whose password is still stored in the old reversible AES form. */
    @BeforeEach
    void seedLegacyLeader() {
        leaderRepository.findByUsername("legacy.user").ifPresent(leaderRepository::delete);
        Leader l = new Leader();
        l.setName("Legacy");
        l.setSurname("User");
        l.setUsername("legacy.user");
        l.setRole("Director");
        l.setPassword(AESUtil.encrypt("parolaVeche"));
        leaderRepository.save(l);
    }

    private String login(String username, String password, String role) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","role":"%s"}
                                """.formatted(username, password, role)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("A protected endpoint rejects a request with no token")
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/children")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/children/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A protected endpoint rejects a forged or corrupted token")
    void forgedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/children").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login issues a token that opens protected endpoints")
    void loginIssuesUsableToken() throws Exception {
        String token = login("legacy.user", "parolaVeche", "DIRECTOR");
        assertThat(token).isNotBlank();

        mvc.perform(get("/api/children").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login response never contains the password")
    void loginResponseHidesPassword() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"legacy.user","password":"parolaVeche","role":"DIRECTOR"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        JsonNode user = json.readTree(body).get("user");
        assertThat(user.has("password")).isFalse();
        assertThat(body).doesNotContain(AESUtil.encrypt("parolaVeche"));
        assertThat(body).doesNotContain("parolaVeche");
    }

    @Test
    @DisplayName("A legacy AES password still logs in, and is rehashed to BCrypt")
    void legacyPasswordIsMigratedToBcrypt() throws Exception {
        assertThat(leaderRepository.findByUsername("legacy.user").orElseThrow()
                .getPassword()).doesNotStartWith("$2");

        login("legacy.user", "parolaVeche", "DIRECTOR");

        String stored = leaderRepository.findByUsername("legacy.user").orElseThrow().getPassword();
        assertThat(stored).startsWith("$2");

        // The same password keeps working after the migration.
        assertThat(login("legacy.user", "parolaVeche", "DIRECTOR")).isNotBlank();
    }

    @Test
    @DisplayName("A wrong password is rejected")
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"legacy.user","password":"gresita","role":"DIRECTOR"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Leaders cannot register themselves, whatever code they send")
    void leadersCannotSelfRegister() throws Exception {
        for (String role : new String[]{"LEADER", "DIRECTOR", "Lider"}) {
            mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"%s","name":"X","surname":"Y","password":"long-enough",
                                     "registrationCode":"AWANA2024"}
                                    """.formatted(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Guest arbiters can still reach every endpoint their scoring tab uses")
    void olimpiadaGuestEndpointsStayPublic() throws Exception {
        // Anything other than 401 proves the route is not access-controlled; a guest
        // arbiter has no account and therefore never carries a token.
        mvc.perform(get("/api/olimpiada/session/NOPE"))
                .andExpect(status().is(not(401)));
        mvc.perform(get("/api/olimpiada/session/NOPE/round-status"))
                .andExpect(status().is(not(401)));
        mvc.perform(get("/api/olimpiada/session/NOPE/compare"))
                .andExpect(status().is(not(401)));
        mvc.perform(post("/api/olimpiada/session/NOPE/score")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is(not(401)));
        mvc.perform(post("/api/olimpiada/session/NOPE/extra")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is(not(401)));
    }

    @Test
    @DisplayName("Olimpiada admin routes are not public")
    void olimpiadaAdminRoutesRequireAuth() throws Exception {
        mvc.perform(get("/api/olimpiada/sessions")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/olimpiada/sessions")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private static org.hamcrest.Matcher<Integer> not(int status) {
        return org.hamcrest.Matchers.not(org.hamcrest.Matchers.is(status));
    }
}
