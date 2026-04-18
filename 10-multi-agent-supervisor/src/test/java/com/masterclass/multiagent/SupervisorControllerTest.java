package com.masterclass.multiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.shared.dto.AgentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupervisorController.class)
class SupervisorControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SupervisorAgent supervisorAgent;

    @Test
    @WithMockUser
    void processReturnsReport() throws Exception {
        when(supervisorAgent.process(anyString())).thenReturn("## Final Report\n\nAI is transforming software engineering...");

        mockMvc.perform(post("/api/v1/supervisor/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgentRequest("Impact of AI on software engineering"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(supervisorAgent).process("Impact of AI on software engineering");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/supervisor/process")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }
}
