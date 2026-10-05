package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Dto.NewSeasonPreviewResponse;
import com.awanabetania.awanabetania.Dto.SeasonResponse;
import com.awanabetania.awanabetania.Dto.SeasonResultsResponse;
import com.awanabetania.awanabetania.Dto.SeasonResultsResponse.ChildResult;
import com.awanabetania.awanabetania.Dto.SeasonResultsResponse.LeaderResult;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.BonStatus;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.ChildManual;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Season;
import com.awanabetania.awanabetania.Model.SeasonChildResult;
import com.awanabetania.awanabetania.Model.SeasonStatus;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildManualRepository;
import com.awanabetania.awanabetania.Repository.ChildProgressRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderEvaluationRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.awanabetania.awanabetania.Repository.SeasonChildResultRepository;
import com.awanabetania.awanabetania.Repository.SeasonRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Club seasons. Everything recorded during a season (scores, meetings, leader evaluations,
 * warnings, fair receipts) is tagged with it, and the app works with the active season.
 *
 * <p>Starting a new season backs up the database first ({@link BackupService}), then, in one
 * transaction: closes the current season, archives each child's counters
 * ({@link SeasonChildResult}), sets points, streaks, attendance, lessons, badges, handbooks,
 * rewards and suspensions back to zero, resets leader ratings, hides feedback and reward
 * notifications, rejects receipts still waiting, and moves planned meetings to the new season. Children, leaders, their contact details and the sticker
 * progress stay. Nothing recorded is deleted except the handbook list, which is archived.</p>
 */
@Service
@RequiredArgsConstructor
public class SeasonService {

    private static final Logger log = LoggerFactory.getLogger(SeasonService.class);

    /** Notifications about the closed season: leader feedback and shirt/hat reminders. */
    private static final List<String> SEASON_NOTIFICATION_TYPES = List.of("FEEDBACK", "SHIRT_ELIGIBLE", "HAT_ELIGIBLE");

    /** Entities that carry a season; rows written by a version without seasons have none. */
    private static final List<String> SEASONAL_ENTITIES = List.of("Score", "Meeting", "LeaderEvaluation", "Warning", "Bon");

    private final SeasonRepository seasonRepository;
    private final SeasonChildResultRepository resultRepository;
    private final ChildRepository childRepository;
    private final ChildProgressRepository progressRepository;
    private final ChildManualRepository manualRepository;
    private final LeaderRepository leaderRepository;
    private final MeetingRepository meetingRepository;
    private final ScoreRepository scoreRepository;
    private final LeaderEvaluationRepository evaluationRepository;
    private final WarningRepository warningRepository;
    private final BonRepository bonRepository;
    private final NotificationRepository notificationRepository;
    private final BackupService backupService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    /**
     * Run at every start: creates the first season if there is none, and gives the active
     * season every row that has none: all existing data the first time, and afterwards what
     * the previous version wrote during a deploy's pre-flight or after a rollback.
     */
    @Transactional
    public void prepare() {
        Season active = seasonRepository.findByStatus(SeasonStatus.ACTIVE)
                .orElseGet(() -> seasonRepository.save(firstSeason()));
        int adopted = 0;
        for (String entity : SEASONAL_ENTITIES) {
            adopted += entityManager
                    .createQuery("UPDATE " + entity + " e SET e.seasonId = :season WHERE e.seasonId IS NULL")
                    .setParameter("season", active.getId())
                    .executeUpdate();
        }
        if (adopted > 0) {
            log.info("Season '{}': {} records without a season were added to it", active.getName(), adopted);
        }
    }

    /** The season the club is in. */
    @Transactional(readOnly = true)
    public Season current() {
        return seasonRepository.findByStatus(SeasonStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active season; it is created at startup"));
    }

    /** The id new records are tagged with. */
    public Integer currentId() {
        return current().getId();
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> list() {
        return seasonRepository.findAllByOrderByIdDesc().stream().map(SeasonResponse::from).toList();
    }

    /** What a new season would reset, and whether something prevents it now. */
    @Transactional(readOnly = true)
    public NewSeasonPreviewResponse preview() {
        Season season = current();
        Integer id = season.getId();
        return new NewSeasonPreviewResponse(
                SeasonResponse.from(season),
                childRepository.count(),
                childRepository.countBySeasonPointsGreaterThan(0),
                childRepository.sumSeasonPoints(),
                scoreRepository.countBySeasonId(id),
                evaluationRepository.countBySeasonIdAndIsVisibleTrue(id),
                childRepository.countByIsSuspendedTrue(),
                bonRepository.countByStatus(BonStatus.PENDING),
                meetingRepository.countBySeasonIdAndIsCompletedFalse(id),
                scoreRepository.firstOpenMeetingWithScores(),
                backupService.enabled());
    }

    /**
     * Backs up the database, then closes the current season and starts a new one, as
     * described on the class.
     *
     * @throws ApiException 409 when the current season is not the one the director confirmed,
     *                      the name is taken, or a meeting with scores is still open;
     *                      503 when the backup failed (nothing is changed)
     */
    @Transactional
    public SeasonResponse startNew(Integer confirmedSeasonId, String requestedName, AuthUser director) {
        Season closing = current();
        if (!closing.getId().equals(confirmedSeasonId)) {
            throw ApiException.conflict("The current season has changed in the meantime. Reload the page.");
        }
        String name = requestedName.trim();
        if (seasonRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A season named \"" + name + "\" already exists.");
        }
        LocalDate openMeeting = scoreRepository.firstOpenMeetingWithScores();
        if (openMeeting != null) {
            throw ApiException.conflict("The meeting of " + openMeeting
                    + " is still open and has scores. Close it before starting a new season.");
        }

        // Before anything changes; without a backup the season does not start (503)
        backupService.backup("pre-season");

        LocalDate today = LocalDate.now();
        // The conditional UPDATE is what stops two simultaneous requests: only one closes it
        if (seasonRepository.close(closing.getId(), today) == 0) {
            throw ApiException.conflict("The current season has changed in the meantime. Reload the page.");
        }

        int archived = archiveChildren(closing.getId());
        childRepository.resetSeasonCounters();
        progressRepository.resetManualsCount();
        manualRepository.deleteAllInBatch();
        leaderRepository.resetRatings();
        int hidden = notificationRepository.hideByTypes(SEASON_NOTIFICATION_TYPES);
        int rejected = bonRepository.rejectAllPending();

        Season next = seasonRepository.save(new Season(name, today));
        int moved = meetingRepository.moveOpenMeetings(closing.getId(), next.getId());

        log.info("Season '{}' closed and '{}' started by leader #{}: {} children archived, {} notifications hidden, "
                        + "{} pending receipts rejected, {} planned meetings moved",
                closing.getName(), next.getName(), director.id(), archived, hidden, rejected, moved);
        return SeasonResponse.from(next);
    }

    /** @throws ApiException 404 unknown season, 409 name taken */
    @Transactional
    public SeasonResponse rename(Integer id, String requestedName) {
        Season season = seasonRepository.findById(id).orElseThrow(() -> ApiException.notFound("Season not found."));
        String name = requestedName.trim();
        if (seasonRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("A season named \"" + name + "\" already exists.");
        }
        season.setName(name);
        return SeasonResponse.from(season);
    }

    /**
     * The record of a closed season: children ranked by the points they earned at meetings,
     * and the leaders' average ratings.
     *
     * @throws ApiException 404 unknown season, 409 the season is still running
     */
    @Transactional(readOnly = true)
    public SeasonResultsResponse results(Integer id) {
        Season season = seasonRepository.findById(id).orElseThrow(() -> ApiException.notFound("Season not found."));
        if (season.getStatus() == SeasonStatus.ACTIVE) {
            throw ApiException.conflict("The season is still running; its results are kept when it closes.");
        }

        Map<Integer, Integer> earned = sums(scoreRepository.earnedPerChild(id));
        Map<Integer, Integer> spent = sums(bonRepository.spentPerChild(id));
        Map<Integer, Integer> warnings = sums(warningRepository.countPerChild(id));

        List<SeasonChildResult> rows = new ArrayList<>(resultRepository.findWithChildBySeasonId(id));
        Comparator<SeasonChildResult> byEarned = Comparator.comparingInt(r -> earned.getOrDefault(r.getChild().getId(), 0));
        rows.sort(byEarned.reversed()
                .thenComparing(Comparator.comparingInt(SeasonChildResult::getSeasonPoints).reversed())
                .thenComparing(r -> r.getChild().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<ChildResult> children = new ArrayList<>();
        for (SeasonChildResult r : rows) {
            Child c = r.getChild();
            children.add(new ChildResult(children.size() + 1, c.getId(), c.getName(), c.getSurname(),
                    earned.getOrDefault(c.getId(), 0), spent.getOrDefault(c.getId(), 0), r.getSeasonPoints(),
                    r.getTotalAttendance(), r.getAttendanceStreak(), r.getLessonsCompleted(), r.getBadgesCount(),
                    r.isHadManual(), r.isHadShirt(), r.isHadHat(), warnings.getOrDefault(c.getId(), 0)));
        }

        List<Object[]> ratings = evaluationRepository.ratingsPerLeader(id);
        Map<Integer, Leader> leadersById = leaderRepository
                .findAllById(ratings.stream().map(row -> (Integer) row[0]).toList())
                .stream().collect(Collectors.toMap(Leader::getId, Function.identity()));
        List<LeaderResult> leaders = ratings.stream()
                .filter(row -> leadersById.containsKey((Integer) row[0]))
                .map(row -> {
                    Leader l = leadersById.get((Integer) row[0]);
                    double average = Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0;
                    return new LeaderResult(l.getId(), l.getName(), l.getSurname(), average, ((Number) row[2]).longValue());
                })
                .sorted(Comparator.comparingDouble(LeaderResult::rating).reversed()
                        .thenComparing(LeaderResult::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new SeasonResultsResponse(SeasonResponse.from(season),
                meetingRepository.countBySeasonIdAndIsCompletedTrue(id), children, leaders);
    }

    // ---------------------------------------------------------------------------------

    /** Copies every child's season counters into the archive; returns how many. */
    private int archiveChildren(Integer seasonId) {
        List<SeasonChildResult> results = new ArrayList<>();
        for (Child c : childRepository.findAll()) {
            SeasonChildResult r = new SeasonChildResult();
            r.setSeasonId(seasonId);
            r.setChild(c);
            r.setSeasonPoints(orZero(c.getSeasonPoints()));
            r.setTotalAttendance(orZero(c.getTotalAttendance()));
            r.setAttendanceStreak(orZero(c.getAttendanceStreak()));
            r.setLessonsCompleted(orZero(c.getLessonsCompleted()));
            r.setBadgesCount(orZero(c.getBadgesCount()));
            r.setHadManual(Boolean.TRUE.equals(c.getHasManual()));
            r.setHadShirt(Boolean.TRUE.equals(c.getHasShirt()));
            r.setHadHat(Boolean.TRUE.equals(c.getHasHat()));
            r.setManuals(manualsJson(c.getManuals()));
            results.add(r);
        }
        resultRepository.saveAll(results);
        return results.size();
    }

    /** The handbooks a child had, kept as JSON because the list itself starts over. */
    private String manualsJson(List<ChildManual> manuals) {
        if (manuals == null || manuals.isEmpty()) return null;
        List<Map<String, Object>> list = manuals.stream().map(m -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", m.getName());
            entry.put("status", m.getStatus());
            entry.put("startDate", m.getStartDate());
            entry.put("endDate", m.getEndDate());
            return entry;
        }).toList();
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not archive handbooks", e);
        }
    }

    /**
     * The first season, for data recorded before seasons existed. Named after the club year
     * (September to summer) of the earliest meeting; the director can rename it.
     */
    private Season firstSeason() {
        LocalDate start = entityManager.createQuery("SELECT MIN(m.date) FROM Meeting m", LocalDate.class)
                .getSingleResult();
        if (start == null) start = LocalDate.now();
        int year = start.getMonthValue() >= 8 ? start.getYear() : start.getYear() - 1;
        return new Season(year + "–" + (year + 1), start);
    }

    /** Rows of [childId, number] as a map. */
    private static Map<Integer, Integer> sums(List<Object[]> rows) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) map.put((Integer) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
