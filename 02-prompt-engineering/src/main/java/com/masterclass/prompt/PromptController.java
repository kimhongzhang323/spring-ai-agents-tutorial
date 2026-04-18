package com.masterclass.prompt;

import com.masterclass.prompt.dto.ChainOfThoughtRequest;
import com.masterclass.prompt.dto.CodeReviewRequest;
import com.masterclass.prompt.dto.ExpertChatRequest;
import com.masterclass.prompt.dto.MetaPromptRequest;
import com.masterclass.prompt.dto.PromptChainRequest;
import com.masterclass.prompt.dto.SummarizeRequest;
import com.masterclass.prompt.dto.TranslateRequest;
import com.masterclass.shared.dto.AgentRequest;
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
@Tag(name = "Prompt Engineering", description = "Module 02 — PromptTemplate, Chain-of-Thought, few-shot, meta-prompting, prompt chaining")
@SecurityRequirement(name = "bearerAuth")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    // ── Basic Patterns ────────────────────────────────────────────────────────

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
            description = "Returns POSITIVE, NEGATIVE, or NEUTRAL. Few-shot examples in the template teach the output format."
    )
    public ResponseEntity<AgentResponse> classifySentiment(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.classifySentiment(request.message())));
    }

    @PostMapping("/ask-expert")
    @Operation(
            summary = "Role prompting — ask a domain expert",
            description = "Demonstrates SystemPromptTemplate: the system message sets the LLM persona (domain, experience, audience level)."
    )
    public ResponseEntity<AgentResponse> askExpert(@Valid @RequestBody ExpertChatRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.askExpert(request)));
    }

    // ── Advanced Patterns ─────────────────────────────────────────────────────

    @PostMapping("/chain-of-thought")
    @Operation(
            summary = "Chain-of-Thought (CoT) reasoning",
            description = """
                    Forces the LLM to reason step-by-step before answering.
                    Dramatically improves accuracy on math, logic, and debugging tasks.
                    The structured template ensures consistent step labelling.
                    """
    )
    public ResponseEntity<AgentResponse> chainOfThought(@Valid @RequestBody ChainOfThoughtRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.chainOfThought(request)));
    }

    @PostMapping("/code-review")
    @Operation(
            summary = "Structured code review (CoT specialised)",
            description = """
                    Applies chain-of-thought to code review: understand → find issues → security →
                    suggest refactoring → prioritise. The structured template prevents vague feedback.
                    """
    )
    public ResponseEntity<AgentResponse> reviewCode(@Valid @RequestBody CodeReviewRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.reviewCode(request)));
    }

    @PostMapping("/meta-prompt")
    @Operation(
            summary = "Meta-prompting — LLM writes a prompt for you",
            description = """
                    Describe a use case in plain English; the LLM generates a production-quality
                    prompt template. Useful for bootstrapping new prompts quickly.
                    """
    )
    public ResponseEntity<AgentResponse> generatePrompt(@Valid @RequestBody MetaPromptRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.generatePrompt(request)));
    }

    @PostMapping("/extract-entities")
    @Operation(
            summary = "Named entity extraction (few-shot)",
            description = """
                    Extracts persons, organisations, locations, dates, and products using
                    3 few-shot examples. Returns JSON. No fine-tuning required.
                    """
    )
    public ResponseEntity<AgentResponse> extractEntities(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(AgentResponse.of(promptService.extractEntities(request.message())));
    }

    @PostMapping("/prompt-chain")
    @Operation(
            summary = "Prompt chaining — multi-step pipeline",
            description = """
                    Executes a 3-step chain: extract facts → analyse implications → generate recommendations.
                    Each step's output feeds the next. Returns all intermediate results for transparency.
                    """
    )
    public ResponseEntity<PromptService.PromptChainResult> promptChain(@Valid @RequestBody PromptChainRequest request) {
        return ResponseEntity.ok(promptService.executeChain(request));
    }
}
