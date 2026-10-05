package com.awanabetania.awanabetania.Security;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Attack scenarios from the security audit. Each test plays an attacker and asserts the
 * safe outcome, so a regression that reopens one of these holes fails the build.
 * <p>
 * The attacker in most scenarios is a child account: anyone can create one through the
 * public registration form, without any code, so "authenticated" must never mean "trusted".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuditTest {

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private BonRepository bonRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private final ObjectMapper json = new ObjectMapper();

    private Leader director;
    private Leader leader;
    private Child victimChild;

    @BeforeEach
    void seed() {
        bonRepository.deleteAll();
        director = saveLeader("audit.director", "Director", "parolaDirector");
        leader = saveLeader("audit.leader", "LEADER", "parolaLider");

        childRepository.findByUsername("audit.victim").ifPresent(childRepository::delete);
        Child c = new Child();
        c.setName("Victim");
        c.setSurname("Child");
        c.setUsername("audit.victim");
        c.setPassword(passwordEncoder.encode("parolaCopil"));
        c.setSeasonPoints(100);
        victimChild = childRepository.save(c);
    }

    private Leader saveLeader(String username, String role, String password) {
        leaderRepository.findByUsername(username).ifPresent(l -> {
            l.getDepartments().clear();
            leaderRepository.delete(l);
        });
        Leader l = new Leader();
        l.setName(username);
        l.setSurname("Audit");
        l.setUsername(username);
        l.setRole(role);
        l.setPassword(passwordEncoder.encode(password));
        return leaderRepository.save(l);
    }

    private String login(String username, String password, String role) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","role":"%s"}
                                """.formatted(username, password, role)))
                .andReturn();
        if (res.getResponse().getStatus() != 200) return null;
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    /** A child token, obtained the way an outsider would: public registration, no code needed. */
    private String attackerChildToken() throws Exception {
        childRepository.findByUsername("hackerkid").ifPresent(childRepository::delete);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"CHILD","name":"Hacker","surname":"Kid","password":"secret1"}
                                """))
                .andExpect(status().isOk());
        String token = login("hackerkid", "secret1", "CHILD");
        assertThat(token).isNotNull();
        return token;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------------------------------------------------------------------------------
    // Account takeover / privilege escalation
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("A child cannot change a director's password and take over the account")
    void childCannotTakeOverDirector() throws Exception {
        String kid = attackerChildToken();

        mvc.perform(put("/api/leaders/" + director.getId())
                .header("Authorization", bearer(kid))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"x","surname":"y","username":"audit.director","password":"pwned"}
                        """));

        assertThat(login("audit.director", "pwned", "DIRECTOR"))
                .as("attacker-chosen password must not work")
                .isNull();
    }

    @Test
    @DisplayName("A leader cannot change another leader's password")
    void leaderCannotEditAnotherLeader() throws Exception {
        String token = login("audit.leader", "parolaLider", "LEADER");
        mvc.perform(put("/api/leaders/" + director.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x","surname":"y","username":"audit.director","password":"pwned"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A password changed from the profile is stored hashed, never in plain text")
    void profilePasswordIsHashed() throws Exception {
        String token = login("audit.leader", "parolaLider", "LEADER");
        mvc.perform(put("/api/leaders/" + leader.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"L","surname":"A","username":"audit.leader","password":"nouaParola"}
                                """))
                .andExpect(status().isOk());

        String stored = leaderRepository.findById(leader.getId()).orElseThrow().getPassword();
        assertThat(stored).startsWith("$2");
        assertThat(login("audit.leader", "nouaParola", "LEADER")).isNotNull();
    }

    @Test
    @DisplayName("The hard-coded master codes no longer delete leaders")
    void masterDeletionCodesAreGone() throws Exception {
        String kid = attackerChildToken();
        for (String code : new String[]{"ADMIN", "AWANA2024", "BETANIA"}) {
            mvc.perform(delete("/api/leaders/" + director.getId())
                    .param("code", code)
                    .header("Authorization", bearer(kid)));
        }
        String token = login("audit.leader", "parolaLider", "LEADER");
        mvc.perform(delete("/api/leaders/" + director.getId())
                .param("code", "ADMIN")
                .header("Authorization", bearer(token)));

        assertThat(leaderRepository.findById(director.getId())).isPresent();
    }

    @Test
    @DisplayName("Deletion codes are never serialised in API responses")
    void deletionCodesAreNotExposed() throws Exception {
        victimChild.setDeletionCode("SECRET");
        childRepository.save(victimChild);
        director.setDeletionCode("SECRET");
        leaderRepository.save(director);

        String token = login("audit.leader", "parolaLider", "LEADER");
        String children = mvc.perform(get("/api/children").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        String leaders = mvc.perform(get("/api/leaders").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();

        assertThat(children).doesNotContain("SECRET");
        assertThat(leaders).doesNotContain("SECRET");
    }

    @Test
    @DisplayName("A user cannot trigger a deletion request for someone else's account")
    void deletionRequestIsForOwnAccountOnly() throws Exception {
        String kid = attackerChildToken();
        mvc.perform(post("/api/account/request-deletion")
                .header("Authorization", bearer(kid))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":%d,\"role\":\"LEADER\"}".formatted(director.getId())));

        assertThat(leaderRepository.findById(director.getId()).orElseThrow().getDeletionCode()).isNull();
    }

    // ---------------------------------------------------------------------------------
    // Children must stay inside their own profile
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("A child cannot use leader-only endpoints")
    void childIsLockedOutOfLeaderEndpoints() throws Exception {
        String kid = attackerChildToken();

        mvc.perform(get("/api/children").header("Authorization", bearer(kid)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/leaders").header("Authorization", bearer(kid)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/scores/add").header("Authorization", bearer(kid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":1,\"extraPoints\":1000000}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/teams/add-manual-points").header("Authorization", bearer(kid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/bons").header("Authorization", bearer(kid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/notifications/add").header("Authorization", bearer(kid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"phishing\",\"visibleTo\":\"ALL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A child can still read and edit their own profile, but nobody else's")
    void childProfileIsSelfOnly() throws Exception {
        String kid = attackerChildToken();
        Integer ownId = childRepository.findByUsername("hackerkid").orElseThrow().getId();

        mvc.perform(get("/api/children/" + ownId).header("Authorization", bearer(kid)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/children/" + victimChild.getId()).header("Authorization", bearer(kid)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/children/" + victimChild.getId())
                        .header("Authorization", bearer(kid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"surname\":\"y\",\"username\":\"stolen\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/stickers").header("Authorization", bearer(kid)))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------------------------
    // Information disclosure through notifications
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("Director-only notifications are not readable by passing leaderId=DIRECTOR")
    void directorNotificationsNotReadableByParam() throws Exception {
        Notification n = new Notification();
        n.setMessage("DELETION REQUEST: X. Confirmation code: TOPSECRET");
        n.setVisibleTo("DIRECTOR");
        n.setDate(LocalDate.now());
        n.setIsVisible(true);
        notificationRepository.save(n);

        String token = login("audit.leader", "parolaLider", "LEADER");
        String body = mvc.perform(get("/api/notifications")
                        .param("leaderId", "DIRECTOR")
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("TOPSECRET");
    }

    @Test
    @DisplayName("A child whose id equals the director's id does not receive director alerts")
    void dashboardUsesTokenIdentity() throws Exception {
        Notification n = new Notification();
        n.setMessage("Confirmation code: DASHSECRET");
        n.setVisibleTo("DIRECTOR");
        n.setDate(LocalDate.now());
        n.setIsVisible(true);
        notificationRepository.save(n);

        String kid = attackerChildToken();
        String body = mvc.perform(get("/api/dashboard/stats")
                        .param("leaderId", String.valueOf(director.getId()))
                        .header("Authorization", bearer(kid)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("DASHSECRET");

        String dir = login("audit.director", "parolaDirector", "DIRECTOR");
        String dirBody = mvc.perform(get("/api/dashboard/stats")
                        .param("leaderId", String.valueOf(director.getId()))
                        .header("Authorization", bearer(dir)))
                .andReturn().getResponse().getContentAsString();
        assertThat(dirBody).contains("DASHSECRET");
    }

    // ---------------------------------------------------------------------------------
    // Fair shop (bons) — point integrity
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("A receipt with a negative total cannot be used to mint points")
    void negativeBonIsRejected() throws Exception {
        String token = login("audit.leader", "parolaLider", "LEADER");
        mvc.perform(post("/api/bons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":%d,\"items\":\"[]\",\"totalPoints\":-100000}"
                                .formatted(victimChild.getId())))
                .andExpect(status().isBadRequest());

        assertThat(childRepository.findById(victimChild.getId()).orElseThrow().getSeasonPoints())
                .isEqualTo(100);
    }

    // ---------------------------------------------------------------------------------
    // Robustness: malformed input must be a 4xx, not a server crash
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("Login with missing fields answers 400/401, not 500")
    void loginWithMissingFieldsDoesNotCrash() throws Exception {
        int status = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LEADER\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isBetween(400, 499);
    }

    @Test
    @DisplayName("Registration without a password answers 400, not 500")
    void registerWithoutPasswordDoesNotCrash() throws Exception {
        int status = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CHILD\",\"name\":\"A\",\"surname\":\"B\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(400);
    }

    @Test
    @DisplayName("A token keeps working only while its account exists")
    void tokenOfDeletedAccountIsRejected() throws Exception {
        String token = login("audit.leader", "parolaLider", "LEADER");
        leader.getDepartments().clear();
        leaderRepository.delete(leader);

        mvc.perform(get("/api/stickers").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }
}
