package com.masterclass.observability;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ObservabilityController.class)
class ObservabilityControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AgentObservationService agentService;

    @Test
    @WithMockUser(username = "testuser")
    void chatReturnsSuccessResponse() throws Exception {
        when(agentService.chat(anyString(), anyString()))
                .thenReturn("Observability ensures you can debug your AI agents in production.");

        mockMvc.perform(post("/api/v1/observe/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("What is observability?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Observability ensures you can debug your AI agents in production."));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/observe/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("hello"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void emptyMessageIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/observe/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    void userIdIsPassedToService() throws Exception {
        when(agentService.chat("alice", "Hello")).thenReturn("Hi, Alice!");

        mockMvc.perform(post("/api/v1/observe/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hi, Alice!"));
    }

    @Test
    @WithMockUser
    void serviceExceptionReturns500() throws Exception {
        when(agentService.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM provider unavailable"));

        mockMvc.perform(post("/api/v1/observe/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Any question?"))))
                .andExpect(status().isInternalServerError());
    }
}
