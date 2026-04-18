package com.masterclass.rag;

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

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean RagService ragService;
    @MockBean IngestionService ingestionService;

    @Test
    @WithMockUser
    void askReturnsAnswer() throws Exception {
        when(ragService.ask(anyString())).thenReturn("Spring AI is a framework for building AI applications.");

        mockMvc.perform(post("/api/v1/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("What is Spring AI?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/rag/ask").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void ingestTextRequiresAdminRole() throws Exception {
        // Regular user (ROLE_USER) should be forbidden from ingesting
        mockMvc.perform(post("/api/v1/rag/ingest/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("some document text"))))
                .andExpect(status().isForbidden());
    }
}
