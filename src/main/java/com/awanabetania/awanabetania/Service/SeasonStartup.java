package com.awanabetania.awanabetania.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Makes sure there is an active season and that every record belongs to one, at every
 * start (including the deploy pre-flight). See {@link SeasonService#prepare()}.
 */
@Component
@RequiredArgsConstructor
public class SeasonStartup implements CommandLineRunner {

    private final SeasonService seasonService;

    @Override
    public void run(String... args) {
        seasonService.prepare();
    }
}
