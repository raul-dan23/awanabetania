/**package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Model.ScoreRequest;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoreController.class)
public class ScoreControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Folosim MockitoBean
    @MockitoBean private ScoreRepository scoreRepository;
    @MockitoBean private ChildRepository childRepository;
    @MockitoBean private MeetingRepository meetingRepository;

    @Test
    void testAddScoreSuccess() throws Exception {
        // 1. Simulam o intalnire activa azi
        Meeting meeting = new Meeting();
        meeting.setDate(LocalDate.now());
        meeting.setIsCompleted(false);
        Mockito.when(meetingRepository.findByIsCompletedFalseOrderByDateAsc())
                .thenReturn(List.of(meeting));

        // 2. Simulam un copil
        Child child = new Child();
        child.setId(10);
        // Important: setam seasonPoints ca sa nu fie null cand adunam puncte la el
        child.setSeasonPoints(0);
        Mockito.when(childRepository.findById(10)).thenReturn(Optional.of(child));

        // 3. Simulam ca nu are puncte azi
        Mockito.when(scoreRepository.findByChildIdAndMeeting_Date(10, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        // 4. Facem cererea
        ScoreRequest request = new ScoreRequest();
        request.setChildId(10);
        request.setAttended(true);
        request.setHasUniform(true);

        mockMvc.perform(post("/api/scores/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}*/