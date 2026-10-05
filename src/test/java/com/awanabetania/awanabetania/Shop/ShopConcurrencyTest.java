package com.awanabetania.awanabetania.Shop;

import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Bon;
import com.awanabetania.awanabetania.Model.BonStatus;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Service.BonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cashiers approving at the same moment, on MySQL 8.0 as in production (row locking is
 * what makes the approval safe, so H2 would prove little). Skipped without Docker.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ShopConcurrencyTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired private BonService bonService;
    @Autowired private BonRepository bonRepository;
    @Autowired private ChildRepository childRepository;

    private Child child(int points) {
        Child c = new Child();
        c.setName("Concurrent");
        c.setSurname("Child");
        c.setSeasonPoints(points);
        return childRepository.save(c);
    }

    private Bon bon(Child child, int total) {
        Bon b = new Bon();
        b.setChild(child);
        b.setItems("[]");
        b.setTotalPoints(total);
        return bonRepository.save(b);
    }

    /** Runs the calls on separate threads, released together, and returns how many succeeded. */
    private int runTogether(int calls, IntFunction<Runnable> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < calls; i++) {
            Runnable work = call.apply(i);
            results.add(pool.submit(() -> {
                start.await();
                try {
                    work.run();
                    return true;
                } catch (ApiException e) {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    return false;
                }
            }));
        }
        start.countDown();
        int succeeded = 0;
        for (Future<Boolean> r : results) {
            if (r.get(60, TimeUnit.SECONDS)) succeeded++;
        }
        pool.shutdown();
        return succeeded;
    }

    private int points(Child c) {
        return childRepository.findById(c.getId()).orElseThrow().getSeasonPoints();
    }

    @Test
    @DisplayName("eight cashiers approving the same receipt charge the child once")
    void sameReceiptIsApprovedOnce() throws Exception {
        Child child = child(1000);
        Bon bon = bon(child, 300);

        int approved = runTogether(8, i -> () -> bonService.approve(bon.getId()));

        assertThat(approved).isEqualTo(1);
        assertThat(points(child)).isEqualTo(700);
        assertThat(bonRepository.findById(bon.getId()).orElseThrow().getStatus()).isEqualTo(BonStatus.APPROVED);
    }

    @Test
    @DisplayName("receipts approved together never take the balance below zero")
    void balanceIsNeverOverdrawn() throws Exception {
        Child child = child(1000);
        List<Bon> bons = new ArrayList<>();
        for (int i = 0; i < 5; i++) bons.add(bon(child, 300));   // 1500 asked, 1000 available

        int approved = runTogether(5, i -> () -> bonService.approve(bons.get(i).getId()));

        assertThat(approved).isEqualTo(3);
        assertThat(points(child)).isEqualTo(100);
        long stillPending = bons.stream()
                .map(b -> bonRepository.findById(b.getId()).orElseThrow().getStatus())
                .filter(s -> s == BonStatus.PENDING)
                .count();
        assertThat(stillPending).isEqualTo(2);   // refused receipts stay pending, nothing half-done
    }
}
