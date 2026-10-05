package com.awanabetania.awanabetania.Season;

import com.awanabetania.awanabetania.Model.BonStatus;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.ChildManual;
import com.awanabetania.awanabetania.Model.ChildProgress;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.LeaderEvaluation;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Model.Score;
import com.awanabetania.awanabetania.Model.SeasonStatus;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildManualRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderEvaluationRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.awanabetania.awanabetania.Repository.SeasonRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;
import com.awanabetania.awanabetania.Service.SeasonService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seasons: a new season starts the children's and leaders' season data over, keeps the
 * people, and keeps the closed season readable. The scenario goes through the same
 * endpoints the app uses during a season, so it also checks that what they record is
 * tagged with the season.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeasonTest {

    private static final String PIN = "0000";

    @Autowired private MockMvc mvc;
    @Autowired private SeasonService seasonService;
    @Autowired private SeasonRepository seasonRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private ChildManualRepository manualRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private LeaderEvaluationRepository evaluationRepository;
    @Autowired private WarningRepository warningRepository;
    @Autowired private BonRepository bonRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private final ObjectMapper json = new ObjectMapper();

    private Leader director;
    private Leader leader;
    private String directorToken;
    private String leaderToken;

    @BeforeEach
    void seed() throws Exception {
        // Other test classes share this database: no meeting may be left open
        for (Meeting m : meetingRepository.findByIsCompletedFalseOrderByDateAsc()) {
            m.setIsCompleted(true);
            meetingRepository.save(m);
        }
        director = saveLeader("season.director", "Director");
        leader = saveLeader("season.leader", "LEADER");
        directorToken = token("season.director");
        leaderToken = token("season.leader");
    }

    // ---------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------

    private Leader saveLeader(String username, String role) {
        leaderRepository.findByUsername(username).ifPresent(l -> {
            evaluationRepository.deleteAll(evaluationRepository.findAll().stream()
                    .filter(e -> e.getLeader().getId().equals(l.getId())).toList());
            l.getDepartments().clear();
            leaderRepository.delete(l);
        });
        Leader l = new Leader();
        l.setUsername(username);
        l.setName(username.replace("season.", "").toUpperCase());
        l.setSurname("Season");
        l.setRole(role);
        l.setPassword(passwordEncoder.encode("pass-" + username));
        return leaderRepository.save(l);
    }

    private String token(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"pass-%s\",\"role\":\"LEADER\"}".formatted(username, username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    /** A child in the middle of a season, with everything a season gives them. */
    private Child childMidSeason() {
        Child c = new Child();
        c.setName("Maria");
        c.setSurname("Season" + UUID.randomUUID().toString().substring(0, 6));
        c.setParentName("Ion");
        c.setParentPhone("0711222333");
        c.setBirthDate(LocalDate.of(2016, 3, 1));
        c.setSeasonPoints(5000);
        c.setAttendanceStreak(4);
        c.setTotalAttendance(7);
        c.setLessonsCompleted(3);
        c.setBadgesCount(2);
        c.setHasShirt(true);
        c.setHasHat(true);
        c.setIsSuspended(false);
        c.setCurrentTeam("ROSU");
        ChildProgress p = new ChildProgress();
        p.setChild(c);
        p.setLastStickerId(6);
        p.setManualsCount(2);
        c.setProgress(p);
        c = childRepository.save(c);
        ChildManual m = new ChildManual();
        m.setChild(c);
        m.setName("Sparks 1");
        m.setStatus("ACTIVE");
        m.setStartDate(LocalDate.of(2025, 9, 15));
        manualRepository.save(m);
        return c;
    }

    private ResultActions as(String token, MockHttpServletRequestBuilder request, String body) throws Exception {
        request.header("Authorization", "Bearer " + token).header("X-Admin-Pin", PIN);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Meeting openMeeting(LocalDate date) throws Exception {
        int id = read(as(leaderToken, post("/api/meetings/add"),
                "{\"date\":\"%s\",\"description\":\"Club\"}".formatted(date))).get("id").asInt();
        return meetingRepository.findById(id).orElseThrow();
    }

    private void score(Child child) throws Exception {
        as(leaderToken, post("/api/scores/add"),
                "{\"childId\":%d,\"attended\":true,\"lesson\":true}".formatted(child.getId()))
                .andExpect(status().isOk());
    }

    private int bon(Child child, int points) throws Exception {
        return read(as(leaderToken, post("/api/bons"),
                "{\"childId\":%d,\"items\":\"[]\",\"totalPoints\":%d}".formatted(child.getId(), points))).get("id").asInt();
    }

    private int currentId() {
        return seasonService.currentId();
    }

    private ResultActions startSeason(int confirmedId, String name) throws Exception {
        return as(directorToken, post("/api/admin/seasons"),
                "{\"currentSeasonId\":%d,\"name\":\"%s\"}".formatted(confirmedId, name));
    }

    private String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------------------------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a new season starts points, streaks, rewards and ratings over, keeps the people, and archives the old season")
    void newSeasonResetsAndArchives() throws Exception {
        int oldSeason = currentId();
        Child child = childMidSeason();

        // A meeting of the season: the child is scored, gets a warning, the leader an evaluation
        Meeting meeting = openMeeting(LocalDate.now().minusDays(1));
        score(child);   // attended + lesson = 2000
        as(leaderToken, post("/api/warnings/add"),
                "{\"childId\":%d,\"description\":\"Noisy\",\"suspension\":true,\"remainingMeetings\":2}".formatted(child.getId()))
                .andExpect(status().isOk());
        as(directorToken, post("/api/feedback/save"),
                "{\"meetingId\":%d,\"directorId\":%d,\"generalRating\":5,\"generalFeedback\":\"Good\",\"evaluations\":[{\"leaderId\":%d,\"rating\":4,\"comment\":\"Punctual\"}]}"
                        .formatted(meeting.getId(), director.getId(), leader.getId()))
                .andExpect(status().isOk());
        as(leaderToken, post("/api/meetings/close/" + meeting.getId()), null).andExpect(status().isOk());

        // The fair: one receipt paid, one left waiting
        int paid = bon(child, 1500);
        as(leaderToken, post("/api/bons/" + paid + "/approve"), null).andExpect(status().isOk());
        int waiting = bon(child, 300);

        // A meeting planned for after the summer
        Meeting planned = openMeeting(LocalDate.now().plusMonths(3));

        Child before = childRepository.findById(child.getId()).orElseThrow();
        assertThat(before.getSeasonPoints()).isEqualTo(5000 + 2000 - 1500);
        assertThat(before.getIsSuspended()).isTrue();
        assertThat(leaderRepository.findById(leader.getId()).orElseThrow().getRating()).isEqualTo(4.0f);

        // What the director sees before confirming
        JsonNode preview = read(as(directorToken, get("/api/admin/seasons/preview"), null));
        assertThat(preview.get("current").get("id").asInt()).isEqualTo(oldSeason);
        assertThat(preview.get("openMeetingDate").isNull()).isTrue();
        assertThat(preview.get("pendingBons").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(preview.get("plannedMeetings").asLong()).isGreaterThanOrEqualTo(1);

        String name = uniqueName("Sezon");
        JsonNode next = read(startSeason(oldSeason, name));
        assertThat(next.get("name").asText()).isEqualTo(name);
        assertThat(next.get("active").asBoolean()).isTrue();
        int newSeason = next.get("id").asInt();
        assertThat(currentId()).isEqualTo(newSeason);

        // The child: same person, season data back to zero, stickers kept
        JsonNode kid = read(as(leaderToken, get("/api/children/" + child.getId()), null));
        assertThat(kid.get("name").asText()).isEqualTo("Maria");
        assertThat(kid.get("parentPhone").asText()).isEqualTo("0711222333");
        assertThat(kid.get("seasonPoints").asInt()).isZero();
        assertThat(kid.get("dailyPoints").asInt()).isZero();
        assertThat(kid.get("attendanceStreak").asInt()).isZero();
        assertThat(kid.get("totalAttendance").asInt()).isZero();
        assertThat(kid.get("lessonsCompleted").asInt()).isZero();
        assertThat(kid.get("badgesCount").asInt()).isZero();
        assertThat(kid.get("hasShirt").asBoolean()).isFalse();
        assertThat(kid.get("hasHat").asBoolean()).isFalse();
        assertThat(kid.get("hasManual").asBoolean()).isFalse();
        assertThat(kid.get("isSuspended").asBoolean()).isFalse();
        assertThat(kid.get("currentTeam").isNull()).isTrue();
        assertThat(kid.get("progress").get("lastStickerId").asInt()).isEqualTo(6);
        assertThat(kid.get("progress").get("manualsCount").asInt()).isZero();

        // What the app shows for the new season is empty
        assertThat(read(as(leaderToken, get("/api/scores/child/" + child.getId()), null))).isEmpty();
        assertThat(read(as(leaderToken, get("/api/warnings/child/" + child.getId()), null))).isEmpty();
        assertThat(read(as(leaderToken, get("/api/feedback/leader/" + leader.getId()), null))).isEmpty();
        assertThat(leaderRepository.findById(leader.getId()).orElseThrow().getRating()).isZero();
        assertThat(notificationRepository.findByVisibleTo(String.valueOf(leader.getId())))
                .extracting(Notification::getType).doesNotContain("FEEDBACK");

        // ...but nothing recorded was deleted
        List<Score> oldScores = scoreRepository.findByChildId(child.getId());
        assertThat(oldScores).hasSize(1).allSatisfy(s -> assertThat(s.getSeasonId()).isEqualTo(oldSeason));
        assertThat(evaluationRepository.findByLeaderIdAndSeasonIdAndIsVisibleTrueOrderByDateDesc(leader.getId(), oldSeason))
                .extracting(LeaderEvaluation::getComment).containsExactly("Punctual");

        // The receipt still waiting is rejected; the paid one stays paid
        assertThat(bonRepository.findById(waiting).orElseThrow().getStatus()).isEqualTo(BonStatus.REJECTED);
        assertThat(bonRepository.findById(paid).orElseThrow().getStatus()).isEqualTo(BonStatus.APPROVED);
        assertThat(read(as(leaderToken, get("/api/bons/pending"), null)).findValues("id"))
                .extracting(JsonNode::asInt).doesNotContain(waiting);

        // The planned meeting moved to the new season; the past one stayed
        assertThat(meetingRepository.findById(planned.getId()).orElseThrow().getSeasonId()).isEqualTo(newSeason);
        assertThat(meetingRepository.findById(meeting.getId()).orElseThrow().getSeasonId()).isEqualTo(oldSeason);

        // The closed season's record
        JsonNode results = read(as(leaderToken, get("/api/seasons/" + oldSeason + "/results"), null));
        assertThat(results.get("season").get("active").asBoolean()).isFalse();
        assertThat(results.get("season").get("endDate").asText()).isEqualTo(LocalDate.now().toString());
        JsonNode row = null;
        for (JsonNode r : results.get("children")) if (r.get("id").asInt() == child.getId()) row = r;
        assertThat(row).isNotNull();
        assertThat(row.get("earnedPoints").asInt()).isEqualTo(2000);
        assertThat(row.get("spentPoints").asInt()).isEqualTo(1500);
        assertThat(row.get("pointsLeft").asInt()).isEqualTo(5500);
        assertThat(row.get("attendance").asInt()).isEqualTo(8);
        assertThat(row.get("lessons").asInt()).isEqualTo(4);
        assertThat(row.get("badges").asInt()).isEqualTo(2);
        assertThat(row.get("shirt").asBoolean()).isTrue();
        assertThat(row.get("hat").asBoolean()).isTrue();
        assertThat(row.get("manual").asBoolean()).isTrue();
        assertThat(row.get("warnings").asInt()).isEqualTo(1);
        JsonNode leaderRow = null;
        for (JsonNode r : results.get("leaders")) if (r.get("id").asInt() == leader.getId()) leaderRow = r;
        assertThat(leaderRow).isNotNull();
        assertThat(leaderRow.get("rating").asDouble()).isEqualTo(4.0);
        assertThat(leaderRow.get("evaluations").asLong()).isEqualTo(1);

        // The new season records under its own id
        meetingRepository.findById(planned.getId()).ifPresent(m -> { m.setDate(LocalDate.now()); meetingRepository.save(m); });
        score(child);
        JsonNode history = read(as(leaderToken, get("/api/scores/child/" + child.getId()), null));
        assertThat(history).hasSize(1);
        assertThat(scoreRepository.findByChildIdAndSeasonIdOrderByMeeting_DateDesc(child.getId(), newSeason)).hasSize(1);
        as(leaderToken, post("/api/meetings/close/" + planned.getId()), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("confirming twice, or after another director, starts only one season")
    void secondConfirmationChangesNothing() throws Exception {
        int confirmed = currentId();
        startSeason(confirmed, uniqueName("Once")).andExpect(status().isOk());
        int after = currentId();

        startSeason(confirmed, uniqueName("Twice")).andExpect(status().isConflict());

        assertThat(currentId()).isEqualTo(after);
        assertThat(seasonRepository.findAll()).filteredOn(s -> s.getStatus() == SeasonStatus.ACTIVE).hasSize(1);
    }

    @Test
    @DisplayName("a season cannot end while a meeting with scores is still open")
    void openMeetingBlocks() throws Exception {
        Child child = childMidSeason();
        Meeting meeting = openMeeting(LocalDate.now());
        score(child);
        int season = currentId();

        JsonNode preview = read(as(directorToken, get("/api/admin/seasons/preview"), null));
        assertThat(preview.get("openMeetingDate").asText()).isEqualTo(LocalDate.now().toString());
        startSeason(season, uniqueName("Blocked")).andExpect(status().isConflict());
        assertThat(currentId()).isEqualTo(season);
        assertThat(childRepository.findById(child.getId()).orElseThrow().getSeasonPoints()).isEqualTo(7000);

        as(leaderToken, post("/api/meetings/close/" + meeting.getId()), null).andExpect(status().isOk());
        startSeason(season, uniqueName("Unblocked")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("only a director with the PIN starts a season; names are required and unique")
    void startingIsGuarded() throws Exception {
        int season = currentId();
        String body = "{\"currentSeasonId\":%d,\"name\":\"%s\"}".formatted(season, uniqueName("Guarded"));

        as(leaderToken, post("/api/admin/seasons"), body).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/seasons").header("Authorization", "Bearer " + directorToken)
                        .header("X-Admin-Pin", "9999").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        startSeason(season, " ").andExpect(status().isBadRequest());
        startSeason(season, "x".repeat(61)).andExpect(status().isBadRequest());
        as(directorToken, post("/api/admin/seasons"), "{\"name\":\"No id\"}").andExpect(status().isBadRequest());
        String taken = seasonRepository.findById(season).orElseThrow().getName();
        startSeason(season, taken.toUpperCase()).andExpect(status().isConflict());

        assertThat(currentId()).isEqualTo(season);
    }

    @Test
    @DisplayName("children cannot see seasons; a running season has no results yet; seasons can be renamed")
    void readingAndRenaming() throws Exception {
        Child kid = childMidSeason();
        kid.setUsername("season.kid." + kid.getId());
        kid.setPassword(passwordEncoder.encode("kid-pass"));
        childRepository.save(kid);
        String kidToken = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"kid-pass\",\"role\":\"CHILD\"}".formatted(kid.getUsername())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
        as(kidToken, get("/api/seasons"), null).andExpect(status().isForbidden());

        // The dashboard, which children do see, names the season
        String current = seasonRepository.findById(currentId()).orElseThrow().getName();
        assertThat(read(as(kidToken, get("/api/dashboard/stats"), null)).get("season").asText()).isEqualTo(current);

        JsonNode seasons = read(as(leaderToken, get("/api/seasons"), null));
        assertThat(seasons.get(0).get("active").asBoolean()).isTrue();
        as(leaderToken, get("/api/seasons/" + currentId() + "/results"), null).andExpect(status().isConflict());
        as(leaderToken, get("/api/seasons/999999/results"), null).andExpect(status().isNotFound());

        String renamed = uniqueName("Renamed");
        JsonNode r = read(as(directorToken, put("/api/admin/seasons/" + currentId()), "{\"name\":\"%s\"}".formatted(renamed)));
        assertThat(r.get("name").asText()).isEqualTo(renamed);
        as(leaderToken, put("/api/admin/seasons/" + currentId()), "{\"name\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("records written without a season (by the previous version, during a deploy) join the active season at startup")
    void recordsWithoutSeasonAreAdopted() throws Exception {
        Child child = childMidSeason();
        Score legacy = new Score();
        legacy.setChild(child);
        legacy.setDate(LocalDate.now());
        legacy.setTotal(1000);
        legacy = scoreRepository.save(legacy);
        assertThat(legacy.getSeasonId()).isNull();

        seasonService.prepare();

        assertThat(scoreRepository.findById(legacy.getId()).orElseThrow().getSeasonId()).isEqualTo(currentId());
    }
}
