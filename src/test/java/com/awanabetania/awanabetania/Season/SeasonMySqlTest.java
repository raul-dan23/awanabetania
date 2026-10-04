package com.awanabetania.awanabetania.Season;

import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Score;
import com.awanabetania.awanabetania.Model.SeasonChildResult;
import com.awanabetania.awanabetania.Model.SeasonStatus;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.awanabetania.awanabetania.Repository.SeasonChildResultRepository;
import com.awanabetania.awanabetania.Repository.SeasonRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Service.SeasonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seasons on MySQL 8.0, with the real migrations: the bulk updates that start a season over,
 * and two directors confirming at the same moment (row locking decides, so H2 would prove
 * little). Skipped without Docker.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SeasonMySqlTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static final AuthUser DIRECTOR = new AuthUser(AuthUser.LEADER, 1, "DIRECTOR");

    @Autowired private SeasonService seasonService;
    @Autowired private SeasonRepository seasonRepository;
    @Autowired private SeasonChildResultRepository resultRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private JdbcTemplate jdbc;

    private Child child(int points) {
        Child c = new Child();
        c.setName("Season");
        c.setSurname("MySql");
        c.setSeasonPoints(points);
        c.setAttendanceStreak(3);
        c.setHasShirt(true);
        return childRepository.save(c);
    }

    @Test
    @DisplayName("four directors confirming at once start exactly one season, and the children are archived once")
    void simultaneousConfirmationsStartOneSeason() throws Exception {
        Child child = child(900);
        int confirmed = seasonService.currentId();

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String name = "Concurrent " + i;
            results.add(pool.submit(() -> {
                start.await();
                try {
                    seasonService.startNew(confirmed, name, DIRECTOR);
                    return true;
                } catch (ApiException e) {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    return false;
                }
            }));
        }
        start.countDown();
        int started = 0;
        for (Future<Boolean> r : results) {
            if (r.get(60, TimeUnit.SECONDS)) started++;
        }
        pool.shutdown();

        assertThat(started).isEqualTo(1);
        assertThat(seasonRepository.findAll()).filteredOn(s -> s.getStatus() == SeasonStatus.ACTIVE).hasSize(1);
        List<SeasonChildResult> archived = resultRepository.findWithChildBySeasonId(confirmed).stream()
                .filter(r -> r.getChild().getId().equals(child.getId())).toList();
        assertThat(archived).singleElement().satisfies(r -> {
            assertThat(r.getSeasonPoints()).isEqualTo(900);
            assertThat(r.getAttendanceStreak()).isEqualTo(3);
            assertThat(r.isHadShirt()).isTrue();
        });
        Child after = childRepository.findById(child.getId()).orElseThrow();
        assertThat(after.getSeasonPoints()).isZero();
        assertThat(after.getAttendanceStreak()).isZero();
        assertThat(after.getHasShirt()).isFalse();
    }

    @Test
    @DisplayName("rows the previous version writes during a deploy get the active season at the next start")
    void rowsWithoutSeasonAreAdopted() {
        Child child = child(0);
        jdbc.update("INSERT INTO scores (child_id, date, total) VALUES (?, ?, 500)", child.getId(), LocalDate.now());
        Integer id = jdbc.queryForObject("SELECT MAX(id) FROM scores", Integer.class);

        seasonService.prepare();

        Score adopted = scoreRepository.findById(id).orElseThrow();
        assertThat(adopted.getSeasonId()).isEqualTo(seasonService.currentId());
    }
}
