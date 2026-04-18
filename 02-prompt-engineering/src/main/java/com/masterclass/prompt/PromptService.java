package com.masterclass.prompt;

import com.masterclass.prompt.dto.ChainOfThoughtRequest;
import com.masterclass.prompt.dto.CodeReviewRequest;
import com.masterclass.prompt.dto.ExpertChatRequest;
import com.masterclass.prompt.dto.MetaPromptRequest;
import com.masterclass.prompt.dto.PromptChainRequest;
import com.masterclass.prompt.dto.SummarizeRequest;
import com.masterclass.prompt.dto.TranslateRequest;
import com.masterclass.shared.guardrails.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptService.class);

    private final ChatClient chatClient;
    private final InputValidator inputValidator;

    // Spring injects classpath resources directly into @Value fields
    @Value("classpath:prompts/summarize.st")
    private Resource summarizeTemplate;

    @Value("classpath:prompts/translate.st")
    private Resource translateTemplate;

    @Value("classpath:prompts/classify-sentiment.st")
    private Resource classifySentimentTemplate;

    @Value("classpath:prompts/system-expert.st")
    private Resource systemExpertTemplate;

    @Value("classpath:prompts/chain-of-thought.st")
    private Resource chainOfThoughtTemplate;

    @Value("classpath:prompts/code-review-chain.st")
    private Resource codeReviewChainTemplate;

    @Value("classpath:prompts/meta-prompt.st")
    private Resource metaPromptTemplate;

    @Value("classpath:prompts/few-shot-entity-extract.st")
    private Resource entityExtractTemplate;

    public PromptService(ChatClient.Builder chatClientBuilder, InputValidator inputValidator) {
        // No default system prompt — each method controls its own system/user messages
        this.chatClient = chatClientBuilder.build();
        this.inputValidator = inputValidator;
    }

    // ── Existing methods ──────────────────────────────────────────────────────

    public String summarize(SummarizeRequest request) {
        validate(request.text());
        var template = new PromptTemplate(summarizeTemplate);
        var prompt = template.create(Map.of("text", request.text(), "maxWords", request.maxWords()));
        return chatClient.prompt(prompt).call().content();
    }

    public String translate(TranslateRequest request) {
        validate(request.text());
        var template = new PromptTemplate(translateTemplate);
        var prompt = template.create(Map.of(
                "text", request.text(),
                "sourceLanguage", request.sourceLanguage(),
                "targetLanguage", request.targetLanguage()
        ));
        return chatClient.prompt(prompt).call().content();
    }

    public String classifySentiment(String text) {
        validate(text);
        var template = new PromptTemplate(classifySentimentTemplate);
        var prompt = template.create(Map.of("text", text));
        String result = chatClient.prompt(prompt).call().content();
        return result == null ? "NEUTRAL" : result.trim().toUpperCase();
    }

    public String askExpert(ExpertChatRequest request) {
        validate(request.question());
        var systemTemplate = new SystemPromptTemplate(systemExpertTemplate);
        var systemMessage = systemTemplate.createMessage(Map.of(
                "domain", request.domain(),
                "yearsExperience", request.yearsExperience(),
                "audienceLevel", request.audienceLevel()
        ));
        return chatClient.prompt()
                .system(systemMessage.getText())
                .user(request.question())
                .call()
                .content();
    }

    // ── New: Chain-of-Thought ─────────────────────────────────────────────────

    /**
     * Applies Chain-of-Thought (CoT) prompting.
     *
     * CoT forces the LLM to externalise its reasoning before answering.
     * This dramatically improves accuracy on multi-step reasoning tasks
     * (math, logic, debugging) because the model "shows its work."
     *
     * Key insight: "Think step by step" added to a prompt is the simplest CoT.
     * A structured CoT template (as used here) is more reliable for complex problems.
     */
    public String chainOfThought(ChainOfThoughtRequest request) {
        validate(request.problem());
        var template = new PromptTemplate(chainOfThoughtTemplate);
        var prompt = template.create(Map.of("problem", request.problem()));
        log.debug("CoT prompt rendered for problem: {}", request.problem().substring(0, Math.min(60, request.problem().length())));
        return chatClient.prompt(prompt).call().content();
    }

    // ── New: Structured Code Review (Chain-of-Thought specialised) ────────────

    /**
     * Code review using a step-by-step chain-of-thought template.
     *
     * The template enforces a structured output: understand → issues → security
     * → refactoring → priority recommendation.
     * Without this structure, LLMs tend to give vague, unordered feedback.
     */
    public String reviewCode(CodeReviewRequest request) {
        validate(request.code());
        var template = new PromptTemplate(codeReviewChainTemplate);
        var prompt = template.create(Map.of(
                "code", request.code(),
                "language", request.language()
        ));
        return chatClient.prompt(prompt).call().content();
    }

    // ── New: Meta-Prompting ───────────────────────────────────────────────────

    /**
     * Meta-prompting: use the LLM to write a better prompt for another task.
     *
     * This is powerful for bootstrapping prompt templates. You describe the use
     * case in plain English and get back a production-quality prompt template.
     *
     * Pattern: LLM-as-prompt-engineer — the model knows what good prompts look like.
     */
    public String generatePrompt(MetaPromptRequest request) {
        validate(request.useCase());
        var template = new PromptTemplate(metaPromptTemplate);
        var prompt = template.create(Map.of(
                "useCase", request.useCase(),
                "targetAudience", request.targetAudience()
        ));
        return chatClient.prompt(prompt).call().content();
    }

    // ── New: Few-Shot Entity Extraction ───────────────────────────────────────

    /**
     * Named Entity Recognition using few-shot examples in the prompt.
     *
     * Few-shot prompting gives the LLM 2-3 input/output examples so it
     * learns the exact output format without fine-tuning.
     * This approach outperforms zero-shot for structured extraction tasks.
     */
    public String extractEntities(String text) {
        validate(text);
        var template = new PromptTemplate(entityExtractTemplate);
        var prompt = template.create(Map.of("text", text));
        return chatClient.prompt(prompt).call().content();
    }

    // ── New: Prompt Chaining ──────────────────────────────────────────────────

    /**
     * Prompt chaining: breaks a complex task into sequential LLM calls
     * where each step's output feeds into the next step's input.
     *
     * Why chain rather than use one big prompt?
     * - Easier to debug (you can inspect intermediate outputs)
     * - Each step can use a different model (cheap model for extraction,
     *   expensive model for synthesis)
     * - Longer output quality degrades in a single call; chaining keeps
     *   each step focused
     *
     * This example chain: extract facts → analyse → generate recommendation
     */
    public PromptChainResult executeChain(PromptChainRequest request) {
        validate(request.input());
        List<String> steps = new ArrayList<>();

        // Step 1: Extract key facts
        String factsPrompt = "Extract 5-7 key facts from the following text as a numbered list. Be factual, no opinions.\n\nText: " + request.input();
        String facts = chatClient.prompt().user(factsPrompt).call().content();
        steps.add(facts);
        log.debug("Chain step 1 (extract): {} chars", facts.length());

        // Step 2: Analyse the facts for implications
        String analysisPrompt = "Given these facts:\n" + facts + "\n\nAnalyse the implications for " + request.analysisContext() + ". Be specific.";
        String analysis = chatClient.prompt().user(analysisPrompt).call().content();
        steps.add(analysis);
        log.debug("Chain step 2 (analyse): {} chars", analysis.length());

        // Step 3: Synthesise a final recommendation
        String synthPrompt = "Based on this analysis:\n" + analysis + "\n\nProvide 3 concrete, actionable recommendations for " + request.audience() + ".";
        String recommendation = chatClient.prompt().user(synthPrompt).call().content();
        steps.add(recommendation);
        log.debug("Chain step 3 (synthesise): {} chars", recommendation.length());

        return new PromptChainResult(facts, analysis, recommendation, steps.size());
    }

    private void validate(String input) {
        var result = inputValidator.validate(input);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.reason());
        }
    }

    public record PromptChainResult(
            String extractedFacts,
            String analysis,
            String recommendation,
            int stepsExecuted
    ) {}
}
