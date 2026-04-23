package com.masterclass.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.shared.dto.AgentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ConversationService conversationService;

    @Test
    @WithMockUser(username = "alice")
    void chatReturnsConversationTurn() throws Exception {
        when(conversationService.chat(any(), any(), any()))
                .thenReturn(new ConversationService.ConversationTurn("conv-1", "Hello", "Hi Alice!"));

        mockMvc.perform(post("/api/v1/conversations/conv-1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("conv-1"))
                .andExpect(jsonPath("$.assistantReply").value("Hi Alice!"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/conversations/conv-1/chat")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void newConversationReturnsId() throws Exception {
        when(conversationService.newConversationId()).thenReturn("new-uuid");
        mockMvc.perform(post("/api/v1/conversations/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("new-uuid"));
    }

    @Test
    @WithMockUser(username = "alice")
    void blankMessageReturns400() throws Exception {
        when(conversationService.chat(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Message must not be blank"));

        mockMvc.perform(post("/api/v1/conversations/conv-1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    void clearConversationReturns204() throws Exception {
        doNothing().when(conversationService).clearConversation(any(), any());

        mockMvc.perform(delete("/api/v1/conversations/conv-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearConversationRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/conversations/conv-1"))
                .andExpect(status().isUnauthorized());
    }
}
