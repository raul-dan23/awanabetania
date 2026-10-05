package com.awanabetania.awanabetania.Account;

import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.GoogleIdTokenVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * "Continue with Google" for leaders, and leaders added by the director.
 * <p>
 * Google's signing key is replaced by one generated here, so the tests can mint ID tokens.
 * Everything else is the production path: the same issuer, audience and expiry checks,
 * the email-verified check and the leader lookup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleSignInTest {

    private static final String PIN = "0000";
    private static final RSAKey GOOGLE_KEY = rsaKey();
    private static final RSAKey OTHER_KEY = rsaKey();

    /** Stands in for Google's published keys, with the production validators. */
    @TestConfiguration
    static class FakeGoogleKeys {
        @Bean
        @Primary
        JwtDecoder testGoogleIdTokenDecoder(@Value("${auth.google.client-id}") String clientId) throws JOSEException {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(GOOGLE_KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(GoogleIdTokenVerifier.validator(clientId));
            return decoder;
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Value("${auth.google.client-id}") private String clientId;
    private final ObjectMapper json = new ObjectMapper();

    private String directorToken;

    @BeforeEach
    void seed() throws Exception {
        for (String username : new String[]{"google.director", "mariaionescu", "google.leader", "dangheorghe", "farageorgescu"}) {
            leaderRepository.findByUsername(username).ifPresent(l -> {
                l.getDepartments().clear();
                leaderRepository.delete(l);
            });
        }
        Leader director = new Leader();
        director.setUsername("google.director");
        director.setName("Dir");
        director.setSurname("Ector");
        director.setRole("Director");
        director.setPassword(passwordEncoder.encode("director-pass"));
        leaderRepository.save(director);
        directorToken = token(passwordLogin("google.director", "director-pass", "LEADER"));
    }

    // ---------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------

    private static RSAKey rsaKey() {
        try {
            return new RSAKeyGenerator(2048).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** An ID token as Google would issue it for this app; {@code tweak} changes claims. */
    private String idToken(String sub, String email, Consumer<JWTClaimsSet.Builder> tweak, RSAKey key) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(clientId)
                .subject(sub)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", "Test Person")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)));
        tweak.accept(claims);
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private String idToken(String sub, String email) throws Exception {
        return idToken(sub, email, c -> { }, GOOGLE_KEY);
    }

    private ResultActions googleLogin(String credential) throws Exception {
        return mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("credential", credential))));
    }

    private ResultActions passwordLogin(String username, String password, String role) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}".formatted(username, password, role)));
    }

    private String token(ResultActions login) throws Exception {
        return json.readTree(login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("token").asText();
    }

    private ResultActions asDirector(MockHttpServletRequestBuilder request, String body) throws Exception {
        request.header("Authorization", "Bearer " + directorToken).header("X-Admin-Pin", PIN);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    private int invite(String name, String surname, String email) throws Exception {
        String body = asDirector(post("/api/admin/leaders"),
                "{\"name\":\"%s\",\"surname\":\"%s\",\"email\":\"%s\",\"role\":\"LEADER\"}".formatted(name, surname, email))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asInt();
    }

    // ---------------------------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("the login screen can read the public client id without logging in")
    void configIsPublic() throws Exception {
        mvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientId").value(clientId));
    }

    @Test
    @DisplayName("an invited leader gets in with Google, then by their Google id even if the address changes")
    void invitedLeaderSignsIn() throws Exception {
        int id = invite("Maria", "Ionescu", "Maria.Ionescu@Gmail.com");

        String body = googleLogin(idToken("google-sub-maria", "maria.ionescu@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(id))
                .andExpect(jsonPath("$.user.googleLinked").value(true))
                .andExpect(jsonPath("$.user.googleSub").doesNotExist())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn().getResponse().getContentAsString();
        String session = json.readTree(body).get("token").asText();
        mvc.perform(get("/api/bons/pending").header("Authorization", "Bearer " + session))
                .andExpect(status().isOk());

        // Same Google account, new address on Google's side: still the same leader.
        googleLogin(idToken("google-sub-maria", "maria.new@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(id));
    }

    @Test
    @DisplayName("a Google account that is not on the leader list is refused with 403")
    void unknownAccountRefused() throws Exception {
        googleLogin(idToken("google-sub-stranger", "stranger@gmail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("stranger@gmail.com")));
    }

    @Test
    @DisplayName("tokens not meant for this app, expired, unverified or forged are refused")
    void invalidTokensRefused() throws Exception {
        invite("Dan", "Gheorghe", "dan@gmail.com");

        googleLogin(idToken("s", "dan@gmail.com", c -> c.audience("another-app.apps.googleusercontent.com"), GOOGLE_KEY))
                .andExpect(status().isForbidden());
        googleLogin(idToken("s", "dan@gmail.com", c -> c.issuer("https://evil.example.com"), GOOGLE_KEY))
                .andExpect(status().isForbidden());
        // Issued 2h ago, expired 1h ago: well past the 60s allowance for clock differences
        googleLogin(idToken("s", "dan@gmail.com", c -> c.issueTime(Date.from(Instant.now().minusSeconds(7200)))
                .expirationTime(Date.from(Instant.now().minusSeconds(3600))), GOOGLE_KEY))
                .andExpect(status().isForbidden());
        googleLogin(idToken("s", "dan@gmail.com", c -> c.claim("email_verified", false), GOOGLE_KEY))
                .andExpect(status().isForbidden());
        googleLogin(idToken("s", "dan@gmail.com", c -> { }, OTHER_KEY))
                .andExpect(status().isForbidden());
        googleLogin("not-a-jwt").andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"\"}"))
                .andExpect(status().isBadRequest());

        assertThat(leaderRepository.findByEmailIgnoreCase("dan@gmail.com").orElseThrow().isGoogleLinked()).isFalse();
    }

    @Test
    @DisplayName("changing a leader's address unbinds the Google account used so far")
    void changingEmailUnbinds() throws Exception {
        int id = invite("Maria", "Ionescu", "maria@gmail.com");
        googleLogin(idToken("old-sub", "maria@gmail.com")).andExpect(status().isOk());

        asDirector(put("/api/admin/leaders/" + id + "/email"), "{\"email\":\"maria.work@gmail.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria.work@gmail.com"))
                .andExpect(jsonPath("$.googleLinked").value(false));

        googleLogin(idToken("old-sub", "maria@gmail.com")).andExpect(status().isForbidden());
        googleLogin(idToken("new-sub", "maria.work@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(id));
    }

    @Test
    @DisplayName("inviting checks the PIN, the director role, the address and the role")
    void inviteValidation() throws Exception {
        invite("Maria", "Ionescu", "maria@gmail.com");

        asDirector(post("/api/admin/leaders"), "{\"name\":\"A\",\"surname\":\"B\",\"email\":\"MARIA@gmail.com\",\"role\":\"LEADER\"}")
                .andExpect(status().isConflict());
        asDirector(post("/api/admin/leaders"), "{\"name\":\"A\",\"surname\":\"B\",\"email\":\"not-an-email\",\"role\":\"LEADER\"}")
                .andExpect(status().isBadRequest());
        asDirector(post("/api/admin/leaders"), "{\"name\":\"A\",\"surname\":\"B\",\"email\":\"a@b.ro\",\"role\":\"BOSS\"}")
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/leaders").header("Authorization", "Bearer " + directorToken)
                        .header("X-Admin-Pin", "9999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"surname\":\"B\",\"email\":\"a@b.ro\",\"role\":\"LEADER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("with Google configured, a leader added with an address gets no password; one added without gets a temporary password")
    void inviteWithAndWithoutAddress() throws Exception {
        JsonNode google = json.readTree(asDirector(post("/api/admin/leaders"),
                        "{\"name\":\"Maria\",\"surname\":\"Ionescu\",\"email\":\"maria@gmail.com\",\"role\":\"LEADER\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(google.get("temporaryPassword").isNull()).isTrue();
        assertThat(leaderRepository.findById(google.get("id").asInt()).orElseThrow().getPassword()).isNull();

        JsonNode noAddress = json.readTree(asDirector(post("/api/admin/leaders"),
                        "{\"name\":\"Fara\",\"surname\":\"Georgescu\",\"email\":\"\",\"role\":\"LEADER\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String temporary = noAddress.get("temporaryPassword").asText();
        assertThat(temporary).matches("[a-z2-9]{4}-[a-z2-9]{4}");
        assertThat(noAddress.get("email").isNull()).isTrue();
        passwordLogin(noAddress.get("username").asText(), temporary, "LEADER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    @DisplayName("an existing leader links Google from their profile, then signs in with it")
    void leaderLinksOwnAccount() throws Exception {
        Leader l = new Leader();
        l.setUsername("google.leader");
        l.setName("Ion");
        l.setSurname("Vechi");
        l.setRole("LEADER");
        l.setPassword(passwordEncoder.encode("leader-pass"));
        leaderRepository.save(l);
        String session = token(passwordLogin("google.leader", "leader-pass", "LEADER"));

        mvc.perform(post("/api/account/google").header("Authorization", "Bearer " + session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"%s\"}".formatted(idToken("ion-sub", "Ion.Vechi@gmail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ion.vechi@gmail.com"))
                .andExpect(jsonPath("$.googleLinked").value(true));

        googleLogin(idToken("ion-sub", "ion.vechi@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("google.leader"));

        // Another leader cannot take the same Google account.
        String director = directorToken;
        mvc.perform(post("/api/account/google").header("Authorization", "Bearer " + director)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"%s\"}".formatted(idToken("ion-sub", "ion.vechi@gmail.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("children cannot link Google, and child registration still works")
    void childrenStayOnPasswords() throws Exception {
        childRepository.findByUsername("googlekid").ifPresent(childRepository::delete);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CHILD\",\"name\":\"Google\",\"surname\":\"Kid\",\"password\":\"kid-pass\"}"))
                .andExpect(status().isOk());
        String kid = token(passwordLogin("googlekid", "kid-pass", "CHILD"));

        mvc.perform(post("/api/account/google").header("Authorization", "Bearer " + kid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"%s\"}".formatted(idToken("kid-sub", "kid@gmail.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the dashboard's director contacts do not show a child the directors' Google addresses")
    void dashboardHidesDirectorEmails() throws Exception {
        Leader director = leaderRepository.findByUsername("google.director").orElseThrow();
        director.setEmail("dir.ector@gmail.com");
        leaderRepository.save(director);

        childRepository.findByUsername("dashkid").ifPresent(childRepository::delete);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CHILD\",\"name\":\"Dash\",\"surname\":\"Kid\",\"password\":\"kid-pass\"}"))
                .andExpect(status().isOk());
        String kid = token(passwordLogin("dashkid", "kid-pass", "CHILD"));

        String body = mvc.perform(get("/api/dashboard/stats").header("Authorization", "Bearer " + kid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directors[?(@.surname == 'Ector')].name").value(contains("Dir")))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("dir.ector@gmail.com").doesNotContain("google.director");
    }
}
