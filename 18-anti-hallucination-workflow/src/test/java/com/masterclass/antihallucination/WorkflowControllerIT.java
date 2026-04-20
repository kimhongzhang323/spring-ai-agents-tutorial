package com.masterclass.antihallucination;

import com.masterclass.antihallucination.domain.*;
import com.masterclass.antihallucination.workflow.WorkflowEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test skeleton — mocks the WorkflowEngine so no real LLM or Redis is needed.
 * Real infra tests run under -Pci profile with Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowEngine workflowEngine;

    @Test
    @WithMockUser(username = "test-user", roles = "USER")
    void shouldReturnSseEventsForValidWorkflow() throws Exception {
        StepResult stepResult = new StepResult("step1", StepStatus.PASSED,
                "Paris", 0.95, 0.90, "Grounded.", 150L);
        WorkflowResult workflowResult = WorkflowResult.from(
                "test", List.of(stepResult), 200L);

        when(workflowEngine.execute(any()))
                .thenReturn(Flux.just(stepResult, workflowResult));

        mockMvc.perform(post("/api/v1/workflow/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "name": "test",
                                  "steps": [{
                                    "name": "step1",
                                    "type": "AGENT",
                                    "promptTemplate": "What is the capital of France?",
                                    "groundingDocs": ["France capital is Paris."],
                                    "consistencySamples": 1,
                                    "guardEnabled": true,
                                    "targetContextKey": null
                                  }],
                                  "abortOnFirstFailure": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/workflow/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
