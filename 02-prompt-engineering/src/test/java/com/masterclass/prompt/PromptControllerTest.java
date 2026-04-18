package com.masterclass.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.prompt.dto.ExpertChatRequest;
import com.masterclass.prompt.dto.SummarizeRequest;
import com.masterclass.prompt.dto.TranslateRequest;
import com.masterclass.shared.dto.AgentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PromptController.class)
class PromptControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PromptService promptService;

    @Test
    @WithMockUser
    void summarizeReturnsResult() throws Exception {
        when(promptService.summarize(any())).thenReturn("A short summary.");

        mockMvc.perform(post("/api/v1/prompts/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SummarizeRequest("Long text here", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("A short summary."));
    }

    @Test
    @WithMockUser
    void translateReturnsResult() throws Exception {
        when(promptService.translate(any())).thenReturn("Bonjour le monde");

        mockMvc.perform(post("/api/v1/prompts/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TranslateRequest("Hello world", "English", "French"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bonjour le monde"));
    }

    @Test
    @WithMockUser
    void sentimentClassificationReturnsSentiment() throws Exception {
        when(promptService.classifySentiment(any())).thenReturn("POSITIVE");

        mockMvc.perform(post("/api/v1/prompts/classify-sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("I love this!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("POSITIVE"));
    }

    @Test
    void allEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void summarizeRejectsMissingText() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\",\"maxWords\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void expertChatReturnsDomainAnswer() throws Exception {
        when(promptService.askExpert(any())).thenReturn("Generics in Java allow type-safe collections...");

        mockMvc.perform(post("/api/v1/prompts/ask-expert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpertChatRequest("Java", "beginner", 15, "Explain generics"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
