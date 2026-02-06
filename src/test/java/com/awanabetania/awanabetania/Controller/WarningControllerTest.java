/**package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Warning;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarningController.class)
public class WarningControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Folosim MockitoBean
    @MockitoBean private WarningRepository warningRepository;
    @MockitoBean private ChildRepository childRepository;

    @Test
    void testAddWarning() throws Exception {
        Child child = new Child();
        child.setId(5);
        Mockito.when(childRepository.findById(5)).thenReturn(Optional.of(child));

        Warning w = new Warning();
        // Setam ID-ul copilului in campul ajutator (transient)
        w.setChildId(5);
        w.setDescription("Galagie");
        w.setSuspension(true); // Verificam si suspendarea

        mockMvc.perform(post("/api/warnings/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(w)))
                .andExpect(status().isOk());
    }
}*/