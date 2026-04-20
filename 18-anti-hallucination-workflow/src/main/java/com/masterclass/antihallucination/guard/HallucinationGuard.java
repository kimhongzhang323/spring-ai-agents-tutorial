package com.masterclass.antihallucination.guard;

import com.masterclass.antihallucination.config.AntiHallucinationConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inline hallucination guard that calls a judge LLM to score a candidate response
 * against a set of grounding documents.
 *
 * The judge model is intentionally separate from the generator model — using the same
 * model as both generator and judge creates a blind spot.
 */
@Component
public class HallucinationGuard {

    private static final Logger log = LoggerFactory.getLogger(HallucinationGuard.class);

    private static final String JUDGE_SYSTEM = """
            You are an impartial factual grounding evaluator.
            You will receive:
            - QUESTION: the original user question
            - GROUNDING_DOCS: authoritative source documents
            - CANDIDATE_ANSWER: an LLM-generated response to evaluate

            Evaluate CANDIDATE_ANSWER strictly against GROUNDING_DOCS.
            Return a JSON object with exactly these fields:
            {
              "faithfulness": <float 0.0–1.0>,
              "confidence": <float 0.0–1.0>,
              "explanation": "<one sentence>"
            }

            faithfulness = 1.0 means every claim in the answer is directly supported by the docs.
            faithfulness = 0.0 means the answer contains claims not found in the docs (hallucination).
            confidence reflects how certain you are about the faithfulness score.
            """;

    private final ChatClient judgeClient;
    private final AntiHallucinationConfig config;
    private final MeterRegistry meterRegistry;
    private final BeanOutputConverter<GroundingScore> converter;

    public HallucinationGuard(ChatModel chatModel,
                               AntiHallucinationConfig config,
                               MeterRegistry meterRegistry) {
        this.judgeClient = ChatClient.builder(chatModel).build();
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.converter = new BeanOutputConverter<>(GroundingScore.class);
    }

    public GroundingScore verify(String question, String candidateAnswer, List<String> groundingDocs) {
        if (groundingDocs.isEmpty()) {
            log.debug("No grounding docs provided; skipping hallucination check for question: {}", question);
            return new GroundingScore(1.0, 1.0, "No grounding docs — check skipped");
        }

        String docsText = String.join("\n---\n", groundingDocs);
        String userPrompt = """
                QUESTION: %s

                GROUNDING_DOCS:
                %s

                CANDIDATE_ANSWER: %s

                %s
                """.formatted(question, docsText, candidateAnswer, converter.getFormat());

        String raw = judgeClient.prompt()
                .system(JUDGE_SYSTEM)
                .user(userPrompt)
                .call()
                .content();

        GroundingScore score = converter.convert(raw);

        meterRegistry.counter("hallucination.guard.checks").increment();
        if (!score.passes(config.guard().faithfulnessThreshold(), config.guard().confidenceThreshold())) {
            meterRegistry.counter("hallucination.guard.failures").increment();
            log.warn("Hallucination guard FAILED — faithfulness={}, confidence={}, reason={}",
                    score.faithfulness(), score.confidence(), score.explanation());
        } else {
            log.debug("Hallucination guard PASSED — faithfulness={}, confidence={}",
                    score.faithfulness(), score.confidence());
        }

        return score;
    }
}
