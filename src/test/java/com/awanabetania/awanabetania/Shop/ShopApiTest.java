package com.awanabetania.awanabetania.Shop;

import com.awanabetania.awanabetania.Model.Bon;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The fair shop over HTTP: receipts (bons), point spending, NFC cards and the admin PIN.
 * <p>
 * Besides the happy paths, these pin down the contract the frontend relies on: the JSON
 * shape of each response, errors as {@code application/problem+json} with a readable
 * {@code detail}, and a 4xx (never a 500) for any malformed input.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopApiTest {

    private static final String PIN = "0000";              // admin.pin in application-test.properties
    private static final String NFC_TOKEN = "test-nfc-token";

    @Autowired private MockMvc mvc;
    @Autowired private ChildRepository childRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private BonRepository bonRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private final ObjectMapper json = new ObjectMapper();

    private String leaderToken;
    private String directorToken;
    private Child child;

    @BeforeEach
    void seed() throws Exception {
        bonRepository.deleteAll();
        leaderToken = token(saveLeader("shop.leader", "Ana", "Seller", "LEADER"));
        directorToken = token(saveLeader("shop.director", "Dan", "Boss", "Director"));

        childRepository.findByUsername("shop.child").ifPresent(childRepository::delete);
        Child c = new Child();
        c.setName("Maria");
        c.setSurname("Pop");
        c.setUsername("shop.child");
        c.setSeasonPoints(1000);
        c.setNfcUid(null);
        child = childRepository.save(c);
    }

    private Leader saveLeader(String username, String name, String surname, String role) {
        leaderRepository.findByUsername(username).ifPresent(l -> {
            l.getDepartments().clear();
            leaderRepository.delete(l);
        });
        Leader l = new Leader();
        l.setUsername(username);
        l.setName(name);
        l.setSurname(surname);
        l.setRole(role);
        l.setPassword(passwordEncoder.encode("parola"));
        return leaderRepository.save(l);
    }

    private String token(Leader l) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"parola\",\"role\":\"LEADER\"}"
                                .formatted(l.getUsername())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    private ResultActions asLeader(MockHttpServletRequestBuilder request, String body) throws Exception {
        return call(request, leaderToken, body);
    }

    /** Creates a receipt over the API and returns its id. */
    private int createBon(int totalPoints) throws Exception {
        String body = asLeader(post("/api/bons"),
                "{\"childId\":%d,\"items\":\"[]\",\"totalPoints\":%d}".formatted(child.getId(), totalPoints))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asInt();
    }

    private int points() {
        return childRepository.findById(child.getId()).orElseThrow().getSeasonPoints();
    }

    private String bonStatus(int id) {
        return bonRepository.findById(id).map(Bon::getStatus).map(Object::toString).orElseThrow();
    }

    /** Every error is RFC 7807 problem JSON with a human-readable detail. */
    private static ResultActions isProblem(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status))
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
              .andExpect(jsonPath("$.status").value(status))
              .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Nested
    @DisplayName("Creating a receipt")
    class Create {

        @Test
        @DisplayName("returns the receipt in the shape the shop screen reads")
        void responseShape() throws Exception {
            asLeader(post("/api/bons"),
                    "{\"childId\":%d,\"items\":\"[{\\\"name\\\":\\\"Suc\\\"}]\",\"totalPoints\":300}"
                            .formatted(child.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.childId").value(child.getId()))
                    .andExpect(jsonPath("$.childName").value("Maria Pop"))
                    .andExpect(jsonPath("$.childPoints").value(1000))
                    .andExpect(jsonPath("$.items").value("[{\"name\":\"Suc\"}]"))
                    .andExpect(jsonPath("$.totalPoints").value(300))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.createdAt").isString());
        }

        @Test
        @DisplayName("records the logged-in leader as seller, whatever name the client sends")
        void sellerComesFromToken() throws Exception {
            asLeader(post("/api/bons"),
                    "{\"childId\":%d,\"leaderName\":\"Someone Else\",\"items\":\"[]\",\"totalPoints\":10}"
                            .formatted(child.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.leaderName").value("Ana Seller"));
        }

        @Test
        @DisplayName("rejects missing or non-positive values with 400")
        void validation() throws Exception {
            isProblem(asLeader(post("/api/bons"), "{\"items\":\"[]\",\"totalPoints\":10}"), 400);
            isProblem(asLeader(post("/api/bons"),
                    "{\"childId\":%d,\"items\":\"[]\",\"totalPoints\":-100}".formatted(child.getId())), 400);
            isProblem(asLeader(post("/api/bons"),
                    "{\"childId\":%d,\"items\":\"[]\",\"totalPoints\":0}".formatted(child.getId())), 400);
            isProblem(asLeader(post("/api/bons"),
                    "{\"childId\":%d,\"totalPoints\":10}".formatted(child.getId())), 400);
        }

        @Test
        @DisplayName("answers 400, not 500, to malformed JSON or wrong types")
        void malformedInput() throws Exception {
            isProblem(asLeader(post("/api/bons"), "{\"childId\":\"abc\",\"items\":\"[]\",\"totalPoints\":10}"), 400);
            isProblem(asLeader(post("/api/bons"), "{not json"), 400);
        }

        @Test
        @DisplayName("answers 404 for an unknown child")
        void unknownChild() throws Exception {
            isProblem(asLeader(post("/api/bons"), "{\"childId\":999999,\"items\":\"[]\",\"totalPoints\":10}"), 404);
        }
    }

    @Nested
    @DisplayName("Approving and rejecting")
    class Approve {

        @Test
        @DisplayName("approval deducts the points once and reports the new balance")
        void approveDeductsOnce() throws Exception {
            int id = createBon(300);

            asLeader(post("/api/bons/" + id + "/approve"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remainingPoints").value(700))
                    .andExpect(jsonPath("$.childName").value("Maria Pop"))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            isProblem(asLeader(post("/api/bons/" + id + "/approve"), null), 409);
            assertThat(points()).isEqualTo(700);
            assertThat(bonStatus(id)).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("a receipt larger than the balance is refused and nothing changes")
        void insufficientPoints() throws Exception {
            int id = createBon(1500);

            isProblem(asLeader(post("/api/bons/" + id + "/approve"), null), 409)
                    .andExpect(jsonPath("$.detail", containsString("1000")));

            assertThat(points()).isEqualTo(1000);
            assertThat(bonStatus(id)).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("a rejected receipt can be neither rejected again nor approved")
        void rejectIsFinal() throws Exception {
            int id = createBon(100);

            asLeader(post("/api/bons/" + id + "/reject"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").isNotEmpty());
            isProblem(asLeader(post("/api/bons/" + id + "/reject"), null), 409);
            isProblem(asLeader(post("/api/bons/" + id + "/approve"), null), 409);

            assertThat(points()).isEqualTo(1000);
            assertThat(bonStatus(id)).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("an unknown receipt is 404")
        void unknownBon() throws Exception {
            isProblem(asLeader(post("/api/bons/999999/approve"), null), 404);
            isProblem(asLeader(post("/api/bons/999999/reject"), null), 404);
            isProblem(asLeader(post("/api/bons/abc/approve"), null), 400);
        }

        @Test
        @DisplayName("the pending list holds only pending receipts")
        void pendingList() throws Exception {
            int approved = createBon(100);
            int pending = createBon(200);
            asLeader(post("/api/bons/" + approved + "/approve"), null).andExpect(status().isOk());

            asLeader(get("/api/bons/pending"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", contains(pending)))
                    .andExpect(jsonPath("$[0].childPoints").value(900));
            asLeader(get("/api/bons/all"), null)
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("NFC bridge endpoints")
    class Nfc {

        private ResultActions nfc(MockHttpServletRequestBuilder request, String body) throws Exception {
            request.header("X-NFC-Token", NFC_TOKEN);
            if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
            return mvc.perform(request);
        }

        @Test
        @DisplayName("registers a card, reads the balance and spends points")
        void happyPath() throws Exception {
            nfc(post("/api/nfc/register"), "{\"childId\":%d,\"uid\":\"A1B2C3D4\"}".formatted(child.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid").value("A1B2C3D4"));
            nfc(get("/api/nfc/A1B2C3D4"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seasonPoints").value(1000));
            nfc(post("/api/nfc/A1B2C3D4/spend"), "{\"amount\":250}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.spent").value(250))
                    .andExpect(jsonPath("$.remainingPoints").value(750));
        }

        @Test
        @DisplayName("bad amounts are 400 and overspending is 409, never 500")
        void spendValidation() throws Exception {
            nfc(post("/api/nfc/register"), "{\"childId\":%d,\"uid\":\"CAFE01\"}".formatted(child.getId()))
                    .andExpect(status().isOk());

            isProblem(nfc(post("/api/nfc/CAFE01/spend"), "{\"amount\":\"abc\"}"), 400);
            isProblem(nfc(post("/api/nfc/CAFE01/spend"), "{\"amount\":-5}"), 400);
            isProblem(nfc(post("/api/nfc/CAFE01/spend"), "{}"), 400);
            isProblem(nfc(post("/api/nfc/CAFE01/spend"), "{\"amount\":5000}"), 409);
            isProblem(nfc(post("/api/nfc/UNKNOWN/spend"), "{\"amount\":5}"), 404);
            assertThat(points()).isEqualTo(1000);
        }

        @Test
        @DisplayName("a wrong or missing token is refused")
        void token() throws Exception {
            mvc.perform(get("/api/nfc/A1B2C3D4").header("X-NFC-Token", "wrong"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/nfc/A1B2C3D4"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Admin PIN")
    class AdminPin {

        @Test
        @DisplayName("a wrong PIN is 403, so the frontend does not log the director out")
        void wrongPinIsForbiddenNotUnauthorized() throws Exception {
            isProblem(call(post("/api/admin/verify-pin"), directorToken, "{\"pin\":\"9999\"}"), 403);
            isProblem(call(get("/api/olimpiada/sessions").header("X-Admin-Pin", "9999"), directorToken, null), 403);
            isProblem(call(post("/api/products").header("X-Admin-Pin", "9999"), directorToken,
                    "{\"name\":\"Suc\",\"pointPrice\":100}"), 403);

            // The session survives the mistake.
            call(get("/api/bons/pending"), directorToken, null).andExpect(status().isOk());
        }

        @Test
        @DisplayName("the right PIN still works")
        void rightPin() throws Exception {
            call(post("/api/admin/verify-pin"), directorToken, "{\"pin\":\"%s\"}".formatted(PIN))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("assigning a card from the Control Center moves it off its previous owner")
        void adminCardAssignment() throws Exception {
            Child other = new Child();
            other.setName("Ion");
            other.setSurname("Old");
            other.setUsername("shop.other");
            childRepository.findByUsername("shop.other").ifPresent(childRepository::delete);
            other.setNfcUid("FEED01");
            other = childRepository.save(other);

            call(post("/api/admin/nfc-register").header("X-Admin-Pin", PIN), directorToken,
                    "{\"childId\":%d,\"uid\":\"FEED01\"}".formatted(child.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Maria Pop"));

            assertThat(childRepository.findById(other.getId()).orElseThrow().getNfcUid()).isNull();
            assertThat(childRepository.findById(child.getId()).orElseThrow().getNfcUid()).isEqualTo("FEED01");

            isProblem(call(post("/api/admin/nfc-register").header("X-Admin-Pin", PIN), directorToken,
                    "{\"childId\":%d,\"uid\":\"\"}".formatted(child.getId())), 400);
        }
    }

    @Nested
    @DisplayName("Products")
    class Products {

        @Test
        @DisplayName("are created, updated and deleted with the PIN, and validated")
        void crud() throws Exception {
            String created = call(post("/api/products").header("X-Admin-Pin", PIN), directorToken,
                    "{\"name\":\"Suc\",\"pointPrice\":150,\"category\":\"Bauturi\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true))
                    .andReturn().getResponse().getContentAsString();
            int id = json.readTree(created).get("id").asInt();

            call(put("/api/products/" + id).header("X-Admin-Pin", PIN), directorToken,
                    "{\"name\":\"Suc mare\",\"pointPrice\":200,\"category\":\"Bauturi\",\"available\":false}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Suc mare"))
                    .andExpect(jsonPath("$.available").value(false));

            isProblem(call(post("/api/products").header("X-Admin-Pin", PIN), directorToken,
                    "{\"name\":\"\",\"pointPrice\":-1}"), 400);

            call(delete("/api/products/" + id).header("X-Admin-Pin", PIN), directorToken, null)
                    .andExpect(status().isOk());
            isProblem(call(delete("/api/products/" + id).header("X-Admin-Pin", PIN), directorToken, null), 404);
        }
    }
}
