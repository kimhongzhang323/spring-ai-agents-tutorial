package com.masterclass.eval;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * LLM-as-judge: sends the question, contexts, expected answer, and actual answer
 * to a strong model (e.g. GPT-4o-mini) and asks it to score faithfulness + relevance.
 *
 * This is a simplified ragas-style evaluator. For production use consider
 * the official ragas library or a fine-tuned judge model.
 */
@Component
public class LlmJudge {

    private static final String JUDGE_PROMPT = """
            You are an impartial evaluator assessing an AI agent's answer quality.

            Question: {question}

            Reference contexts (ideal source material):
            {contexts}

            Expected answer (ground truth):
            {expected}

            Actual answer produced by the agent:
            {actual}

            Score on two dimensions from 0.0 to 1.0:
            - faithfulness: is the actual answer grounded in the reference contexts? (1.0 = fully grounded, 0.0 = hallucinated)
            - relevance: does the actual answer address the question? (1.0 = fully relevant, 0.0 = completely off-topic)

            Respond with a JSON object containing: faithfulness (float), relevance (float), rationale (string, ≤ 80 words).
            {format}
            """;

    private final ChatClient chatClient;
    private final BeanOutputConverter<JudgeOutput> converter;

    public LlmJudge(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.converter  = new BeanOutputConverter<>(JudgeOutput.class);
    }

    public EvalScore evaluate(EvalCase evalCase, String actualAnswer) {
        String contexts = String.join("\n---\n", evalCase.contexts());

        String raw = chatClient.prompt()
                .user(u -> u.text(JUDGE_PROMPT)
                        .param("question",  evalCase.question())
                        .param("contexts",  contexts)
                        .param("expected",  evalCase.expectedAnswer())
                        .param("actual",    actualAnswer)
                        .param("format",    converter.getFormat()))
                .call()
                .content();

        JudgeOutput output = converter.convert(raw);
        return new EvalScore(
                evalCase.id(),
                output.faithfulness(),
                output.relevance(),
                output.rationale()
        );
    }

    public record JudgeOutput(
            double faithfulness,
            double relevance,
            @JsonProperty("rationale") String rationale
    ) {}
}
