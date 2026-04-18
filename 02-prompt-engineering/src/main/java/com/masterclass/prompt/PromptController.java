package com.masterclass.prompt;

import com.masterclass.prompt.dto.ExpertChatRequest;
import com.masterclass.prompt.dto.SummarizeRequest;
import com.masterclass.prompt.dto.TranslateRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prompts")
@Tag(name = "Prompt Engineering", description = "Module 02 – PromptTemplate, few-shot, role prompting")
@SecurityRequirement(name = "bearerAuth")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping("/summarize")
    @Operation(
            summary = "Summarize text",
            description = "Uses a resource-backed PromptTemplate. maxWords controls the output length hint."
    )
    public ResponseEntity<AgentResponse> summarize(@Valid @RequestBody SummarizeRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.summarize(request)));
    }

    @PostMapping("/translate")
    @Operation(summary = "Translate text between languages")
    public ResponseEntity<AgentResponse> translate(@Valid @RequestBody TranslateRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.translate(request)));
    }

    @PostMapping("/classify-sentiment")
    @Operation(
            summary = "Classify sentiment using few-shot prompting",
            description = "Returns POSITIVE, NEGATIVE, or NEUTRAL. Demonstrates few-shot examples baked into the prompt template."
    )
    public ResponseEntity<AgentResponse> classifySentiment(@Valid @RequestBody com.masterclass.shared.dto.AgentRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.classifySentiment(request.message())));
    }

    @PostMapping("/ask-expert")
    @Operation(
            summary = "Ask a domain expert (role prompting)",
            description = "Demonstrates SystemPromptTemplate — the system message sets the LLM's persona before the user question."
    )
    public ResponseEntity<AgentResponse> askExpert(@Valid @RequestBody ExpertChatRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.askExpert(request)));
    }
}
