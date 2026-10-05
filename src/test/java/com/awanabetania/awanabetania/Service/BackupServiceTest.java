package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The backup before a new season, with small stand-in scripts in place of
 * scripts/backup-db.sh: what each exit code means for the operation waiting on it.
 */
class BackupServiceTest {

    @TempDir
    Path dir;

    private Path script(String body) throws Exception {
        Path file = dir.resolve("backup-" + System.nanoTime() + ".sh");
        Files.writeString(file, "#!/bin/sh\n" + body + "\n");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
        return file;
    }

    private static BackupService service(Path script, Duration timeout) {
        return new BackupService(script == null ? "" : script.toString(), timeout);
    }

    @Test
    @DisplayName("runs the script with the label and returns when it succeeds")
    void success() throws Exception {
        Path seen = dir.resolve("label");
        BackupService backup = service(script("echo \"$1\" > " + seen + "; echo 'Backup: x.sql.gz (44K)'"), Duration.ofSeconds(10));

        backup.backup("pre-season");

        assertThat(Files.readString(seen).strip()).isEqualTo("pre-season");
    }

    @Test
    @DisplayName("exit code 3 (backup on the server, Google Drive copy failed) does not stop the operation")
    void copyElsewhereFailedIsAWarning() throws Exception {
        BackupService backup = service(script("echo 'copia a esuat' >&2; exit 3"), Duration.ofSeconds(10));

        backup.backup("pre-season");   // no exception
    }

    @Test
    @DisplayName("any other failure is a 503: the operation must not go on without a backup")
    void failureStopsTheOperation() throws Exception {
        BackupService backup = service(script("echo 'mysqldump: access denied' >&2; exit 1"), Duration.ofSeconds(10));

        assertThatThrownBy(() -> backup.backup("pre-season"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("a script that hangs is stopped and counts as a failure")
    void hangingScriptTimesOut() throws Exception {
        BackupService backup = service(script("sleep 30"), Duration.ofMillis(500));

        long start = System.nanoTime();
        assertThatThrownBy(() -> backup.backup("pre-season")).isInstanceOf(ApiException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("a missing script is a failure, not a silent skip")
    void missingScriptFails() throws Exception {
        BackupService backup = service(dir.resolve("nope.sh"), Duration.ofSeconds(10));

        assertThat(backup.enabled()).isTrue();
        assertThatThrownBy(() -> backup.backup("pre-season")).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("with no script configured, there is no backup and nothing fails")
    void disabled() throws Exception {
        BackupService backup = service(null, Duration.ofSeconds(10));

        assertThat(backup.enabled()).isFalse();
        backup.backup("pre-season");
    }
}
