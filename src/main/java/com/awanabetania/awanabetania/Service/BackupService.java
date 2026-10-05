package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Runs {@code scripts/backup-db.sh} (the same script as the nightly and pre-deploy backups) before
 * an operation that changes a lot of data at once, such as starting a new season. Where the copy
 * goes and how many stay on the server is the script's business ({@code backup.rclone-remote},
 * {@code backup.keep-local}).
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    /** The script's exit code when the backup is on the server but the copy elsewhere failed. */
    static final int COPY_ELSEWHERE_FAILED = 3;

    private final String script;
    private final Duration timeout;

    @Autowired
    public BackupService(@Value("${backup.script:}") String script) {
        this(script, Duration.ofMinutes(5));
    }

    BackupService(String script, Duration timeout) {
        this.script = script == null ? "" : script.trim();
        this.timeout = timeout;
    }

    /** False when {@code backup.script} is empty: no automatic backup (tests, local development). */
    public boolean enabled() {
        return !script.isEmpty();
    }

    /**
     * Makes a backup labelled {@code label}. Returns when it is on the server; a failed copy to
     * Google Drive is only logged, so a network problem does not block the operation.
     *
     * @throws ApiException 503 when no backup could be made; the caller must not go on
     */
    public void backup(String label) {
        if (!enabled()) {
            log.warn("No automatic backup before '{}': backup.script is empty", label);
            return;
        }
        Path output = null;
        try {
            output = Files.createTempFile("awana-backup", ".log");
            Process process = new ProcessBuilder(new File(script).getAbsolutePath(), label)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw failed(label, "timed out after " + timeout.toSeconds() + "s", output);
            }
            int exit = process.exitValue();
            String text = read(output);
            if (exit == 0) {
                log.info("Backup before '{}': {}", label, text);
            } else if (exit == COPY_ELSEWHERE_FAILED) {
                log.warn("Backup before '{}' is on the server, but the copy elsewhere failed: {}", label, text);
            } else {
                throw failed(label, "exit code " + exit, output);
            }
        } catch (IOException e) {
            throw failed(label, e.getMessage(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failed(label, "interrupted", output);
        } finally {
            if (output != null) {
                try {
                    Files.deleteIfExists(output);
                } catch (IOException ignored) {
                    // a temporary log file left behind is harmless
                }
            }
        }
    }

    private ApiException failed(String label, String reason, Path output) {
        log.error("Backup before '{}' failed ({}): {}", label, reason, output == null ? "" : read(output));
        return ApiException.unavailable("The database backup failed, so nothing was changed. "
                + "See the server log (journalctl -u awanabetania).");
    }

    private static String read(Path output) {
        try {
            return Files.readString(output, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            return "";
        }
    }
}
