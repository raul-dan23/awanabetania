/**package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
public class MeetingControllerTest {

    @Autowired private MockMvc mockMvc;


    @MockitoBean private MeetingRepository meetingRepository;
    @MockitoBean private WarningRepository warningRepository;
    @MockitoBean private ScoreRepository scoreRepository;
    @MockitoBean private ChildRepository childRepository;

    // AM ADAUGAT ASTA CA SA NU DEA EROARE LA PORNIRE:
    @MockitoBean private EntityManager entityManager;

    @Test
    void testGetUpcomingMeetings() throws Exception {
        // Simulam o lista de intalniri
        Mockito.when(meetingRepository.findByIsCompletedFalseOrderByDateAsc())
                .thenReturn(List.of(new Meeting()));

        // Verificam daca merge
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk());
    }
}*/