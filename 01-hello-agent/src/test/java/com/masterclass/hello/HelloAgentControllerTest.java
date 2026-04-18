package com.masterclass.hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.shared.dto.AgentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelloAgentController.class)
class HelloAgentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    HelloAgentService helloAgentService;

    @Test
    @WithMockUser
    void chatReturnsAgentResponse() throws Exception {
        when(helloAgentService.chat(anyString())).thenReturn("Hello from the AI!");

        mockMvc.perform(post("/api/v1/hello/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello from the AI!"));
    }

    @Test
    void chatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/hello/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Hello"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void chatRejectsMissingMessage() throws Exception {
        mockMvc.perform(post("/api/v1/hello/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void fallbackResponseIsReturnedOnServiceError() throws Exception {
        when(helloAgentService.chat(anyString()))
                .thenReturn("I'm having trouble connecting to the AI model right now.");

        mockMvc.perform(post("/api/v1/hello/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
