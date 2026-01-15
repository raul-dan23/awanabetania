package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// IMPORTUL NOU:
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**@WebMvcTest(ChildController.class)
public class ChildControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // MODIFICARE AICI: Folosim MockitoBean
    @MockitoBean private ChildRepository childRepository;

    @Test
    void testAddChild() throws Exception {
        Child child = new Child();
        child.setName("Mihai");

        // Cand salvam, returnam copilul
        Mockito.when(childRepository.save(Mockito.any(Child.class))).thenReturn(child);

        mockMvc.perform(post("/api/children/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetChildById() throws Exception {
        Child child = new Child();
        child.setId(1);

        Mockito.when(childRepository.findById(1)).thenReturn(Optional.of(child));

        mockMvc.perform(get("/api/children/1"))
                .andExpect(status().isOk());
    }
}*/