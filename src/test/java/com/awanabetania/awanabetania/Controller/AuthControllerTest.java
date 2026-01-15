/** package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.LoginRequest;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // AM INLOCUIT @MockBean CU @MockitoBean AICI:
    @MockitoBean
    private LeaderRepository leaderRepository;

    @MockitoBean
    private ChildRepository childRepository;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @Test
    void testLoginSuccess() throws Exception {
        Leader leader = new Leader();
        leader.setName("Ion");
        leader.setPassword("1234");
        leader.setRole("LEADER");

        Mockito.when(leaderRepository.findAll()).thenReturn(List.of(leader));

        LoginRequest request = new LoginRequest();
        request.setUsername("Ion");
        request.setPassword("1234");
        request.setRole("LEADER");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginFail() throws Exception {
        Mockito.when(leaderRepository.findAll()).thenReturn(List.of());

        LoginRequest request = new LoginRequest();
        request.setUsername("Ion");
        request.setPassword("GRESIT");
        request.setRole("LEADER");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
} */