package com.awanabetania.awanabetania;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs every Flyway migration on a real, empty MySQL 8.0 (the production engine) and starts
 * the application with {@code ddl-auto=validate}, exactly as production does.
 * <p>
 * The context only starts if the migrated schema matches the JPA entities, so a field added
 * to an entity without a matching {@code V<n>__*.sql} migration fails this test in CI
 * instead of failing the production deploy.
 * <p>
 * Needs Docker; without it the test is skipped (the other tests run on H2).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired private Flyway flyway;

    @Test
    @DisplayName("Migrations build, on an empty MySQL, the schema the entities expect")
    void migrationsMatchEntities() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isFailed()).isFalse());
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(applied[0].getScript()).isEqualTo("V1__baseline.sql");
    }
}
