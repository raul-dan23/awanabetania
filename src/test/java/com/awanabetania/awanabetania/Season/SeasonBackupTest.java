package com.awanabetania.awanabetania.Season;

import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Service.BackupService;
import com.awanabetania.awanabetania.Service.SeasonService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A new season backs up the database first, and does not start without a backup.
 * The backup itself is replaced by a mock here; BackupServiceTest runs real scripts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeasonBackupTest {

    private static final String PIN = "0000";

    @Autowired private MockMvc mvc;
    @Autowired private SeasonService seasonService;
    @Autowired private ChildRepository childRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private BackupService backupService;
    private final ObjectMapper json = new ObjectMapper();

    private String directorToken;

    @BeforeEach
    void seed() throws Exception {
        // Other test classes share this database: no meeting may be left open
        for (Meeting m : meetingRepository.findByIsCompletedFalseOrderByDateAsc()) {
            m.setIsCompleted(true);
            meetingRepository.save(m);
        }
        leaderRepository.findByUsername("backup.director").ifPresent(l -> {
            l.getDepartments().clear();
            leaderRepository.delete(l);
        });
        Leader director = new Leader();
        director.setUsername("backup.director");
        director.setName("Dir");
        director.setSurname("Ector");
        director.setRole("Director");
        director.setPassword(passwordEncoder.encode("director-pass"));
        leaderRepository.save(director);
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"backup.director\",\"password\":\"director-pass\",\"role\":\"LEADER\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        directorToken = json.readTree(body).get("token").asText();
    }

    private ResultActions startSeason(int confirmedId) throws Exception {
        return mvc.perform(post("/api/admin/seasons")
                .header("Authorization", "Bearer " + directorToken).header("X-Admin-Pin", PIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentSeasonId\":%d,\"name\":\"Backup %s\"}".formatted(confirmedId, UUID.randomUUID().toString().substring(0, 8))));
    }

    private Child childWithPoints() {
        Child c = new Child();
        c.setName("Backup");
        c.setSurname("Kid");
        c.setSeasonPoints(4200);
        return childRepository.save(c);
    }

    @Test
    @DisplayName("when the backup fails, the season does not start and nothing is reset (503)")
    void failedBackupChangesNothing() throws Exception {
        doThrow(ApiException.unavailable("The database backup failed, so nothing was changed."))
                .when(backupService).backup("pre-season");
        Child child = childWithPoints();
        int season = seasonService.currentId();

        startSeason(season)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("The database backup failed, so nothing was changed."));

        assertThat(seasonService.currentId()).isEqualTo(season);
        assertThat(childRepository.findById(child.getId()).orElseThrow().getSeasonPoints()).isEqualTo(4200);
    }

    @Test
    @DisplayName("a successful backup is made before the season changes, and the preview says it will be")
    void backupComesFirst() throws Exception {
        when(backupService.enabled()).thenReturn(true);
        int season = seasonService.currentId();
        // What the database looks like at the moment of the backup: still the old season
        AtomicInteger seasonDuringBackup = new AtomicInteger();
        doAnswer(call -> {
            seasonDuringBackup.set(seasonService.currentId());
            return null;
        }).when(backupService).backup("pre-season");

        mvc.perform(get("/api/admin/seasons/preview")
                        .header("Authorization", "Bearer " + directorToken).header("X-Admin-Pin", PIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoBackup").value(true));

        startSeason(season).andExpect(status().isOk());

        verify(backupService).backup("pre-season");
        assertThat(seasonDuringBackup.get()).isEqualTo(season);
        assertThat(seasonService.currentId()).isNotEqualTo(season);
    }
}
