package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.SeasonResponse;
import com.awanabetania.awanabetania.Dto.SeasonResultsResponse;
import com.awanabetania.awanabetania.Service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Club seasons, for leaders: the list and the record of each closed season. */
@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    /** Every season, newest first; the active one has {@code active: true}. */
    @GetMapping
    public List<SeasonResponse> list() {
        return seasonService.list();
    }

    /** Final standings and leader ratings of a closed season. 404, or 409 while it is running. */
    @GetMapping("/{id}/results")
    public SeasonResultsResponse results(@PathVariable Integer id) {
        return seasonService.results(id);
    }
}
