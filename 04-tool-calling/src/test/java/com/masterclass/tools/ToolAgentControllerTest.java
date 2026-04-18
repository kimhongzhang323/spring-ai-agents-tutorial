package com.masterclass.tools;

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

@WebMvcTest(ToolAgentController.class)
class ToolAgentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ToolAgentService toolAgentService;

    @Test
    @WithMockUser
    void chatReturnsAgentReply() throws Exception {
        when(toolAgentService.chat(anyString()))
                .thenReturn("The weather in Tokyo is 24°C and partly cloudy. 500 USD = 74,750 JPY.");

        mockMvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgentRequest("Weather in Tokyo and 500 USD to JPY?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "The weather in Tokyo is 24°C and partly cloudy. 500 USD = 74,750 JPY."));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void fallbackResponseIsReturnedOnServiceError() throws Exception {
        when(toolAgentService.chat(anyString()))
                .thenReturn("I'm having trouble reaching the AI right now.");

        mockMvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("What is 2+2?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
