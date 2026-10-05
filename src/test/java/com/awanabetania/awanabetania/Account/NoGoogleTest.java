package com.awanabetania.awanabetania.Account;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The app before Google sign-in is set up (no client id): the Google button is not offered,
 * and a leader the director adds gets a temporary password, even when an address is given,
 * so they can sign in right away.
 */
@SpringBootTest(properties = "auth.google.client-id=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoGoogleTest {

    private static final String PIN = "0000";

    @Autowired private MockMvc mvc;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private final ObjectMapper json = new ObjectMapper();

    private String directorToken;

    @BeforeEach
    void seed() throws Exception {
        for (String username : new String[]{"nogoogle.director", "ioanavasile"}) {
            leaderRepository.findByUsername(username).ifPresent(l -> {
                l.getDepartments().clear();
                leaderRepository.delete(l);
            });
        }
        Leader director = new Leader();
        director.setUsername("nogoogle.director");
        director.setName("Dir");
        director.setSurname("Ector");
        director.setRole("Director");
        director.setPassword(passwordEncoder.encode("director-pass"));
        leaderRepository.save(director);
        directorToken = token(login("nogoogle.director", "director-pass"));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"LEADER\"}".formatted(username, password)));
    }

    private String token(ResultActions login) throws Exception {
        return json.readTree(login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    @DisplayName("the login screen gets no client id, and Google sign-in answers 404")
    void googleIsOff() throws Exception {
        mvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientId").doesNotExist());
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"x.y.z\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a leader added with an address still gets a temporary password, signs in with it and must replace it")
    void invitedLeaderGetsTemporaryPassword() throws Exception {
        JsonNode added = json.readTree(mvc.perform(post("/api/admin/leaders")
                        .header("Authorization", "Bearer " + directorToken).header("X-Admin-Pin", PIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ioana\",\"surname\":\"Vasile\",\"email\":\"ioana@gmail.com\",\"role\":\"LEADER\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(added.get("email").asText()).isEqualTo("ioana@gmail.com");
        String username = added.get("username").asText();
        String temporary = added.get("temporaryPassword").asText();
        assertThat(temporary).matches("[a-z2-9]{4}-[a-z2-9]{4}");

        String leaderToken = token(login(username, temporary)
                .andExpect(jsonPath("$.mustChangePassword").value(true)));
        mvc.perform(post("/api/account/password").header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"%s\",\"newPassword\":\"parola-mea\"}".formatted(temporary)))
                .andExpect(status().isOk());
        login(username, "parola-mea")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }
}
