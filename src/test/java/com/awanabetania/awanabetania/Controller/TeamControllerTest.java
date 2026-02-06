package com.awanabetania.awanabetania.Controller;

/**import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Team;
import com.awanabetania.awanabetania.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// IMPORTUL NOU:
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
public class TeamControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Folosim MockitoBean pentru toate repository-urile
    @MockitoBean private ChildRepository childRepository;
    @MockitoBean private TeamRepository teamRepository;
    @MockitoBean private TeamGamePointRepository gamePointRepository;
    @MockitoBean private ScoreRepository scoreRepository;

    @Test
    void testPickChild() throws Exception {
        Child child = new Child();
        child.setId(1);
        child.setIsSuspended(false); // Important sa nu fie suspendat

        Mockito.when(childRepository.findById(1)).thenReturn(Optional.of(child));
        // Simulam ca gasim echipele
        Mockito.when(teamRepository.findAll()).thenReturn(List.of(new Team("red", "red")));

        Map<String, String> payload = new HashMap<>();
        payload.put("childId", "1");
        payload.put("teamColor", "red");

        mockMvc.perform(post("/api/teams/pick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }
}*/