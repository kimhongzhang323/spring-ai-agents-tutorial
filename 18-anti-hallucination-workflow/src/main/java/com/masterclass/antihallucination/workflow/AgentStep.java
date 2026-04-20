package com.masterclass.antihallucination.workflow;

import com.masterclass.antihallucination.config.AntiHallucinationConfig;
import com.masterclass.antihallucination.domain.StepDef;
import com.masterclass.antihallucination.domain.StepResult;
import com.masterclass.antihallucination.domain.StepStatus;
import com.masterclass.antihallucination.guard.GroundingScore;
import com.masterclass.antihallucination.guard.HallucinationGuard;
import com.masterclass.antihallucination.guard.SelfConsistencyChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Executes a single AGENT-type step: interpolates the prompt, optionally applies
 * self-consistency sampling, then runs the hallucination guard.
 */
@Component
public class AgentStep {

    private static final Logger log = LoggerFactory.getLogger(AgentStep.class);

    private final ChatClient chatClient;
    private final HallucinationGuard guard;
    private final SelfConsistencyChecker consistencyChecker;
    private final AntiHallucinationConfig config;

    public AgentStep(ChatModel chatModel,
                     HallucinationGuard guard,
                     SelfConsistencyChecker consistencyChecker,
                     AntiHallucinationConfig config) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.guard = guard;
        this.consistencyChecker = consistencyChecker;
        this.config = config;
    }

    public StepResult execute(StepDef step, WorkflowContext ctx) {
        long start = System.currentTimeMillis();
        String interpolatedPrompt = ctx.interpolate(step.promptTemplate());

        log.debug("Executing step '{}' — prompt interpolated", step.name());

        String response;
        int samples = step.consistencySamples() > 1
                ? step.consistencySamples()
                : config.selfConsistency().defaultSamples() > 1 ? 1 : 1;

        if (samples > 1) {
            response = consistencyChecker.majorityVote(
                    "You are a helpful AI assistant.",
                    interpolatedPrompt,
                    samples,
                    config.selfConsistency().temperature()
            );
        } else {
            response = chatClient.prompt()
                    .user(interpolatedPrompt)
                    .call()
                    .content();
        }

        ctx.put(step.name(), response);

        if (!step.guardEnabled() || step.groundingDocs().isEmpty()) {
            return new StepResult(step.name(), StepStatus.PASSED, response,
                    null, null, null, System.currentTimeMillis() - start);
        }

        GroundingScore score = guard.verify(interpolatedPrompt, response, step.groundingDocs());
        boolean passed = score.passes(
                config.guard().faithfulnessThreshold(),
                config.guard().confidenceThreshold()
        );

        StepStatus status = passed ? StepStatus.PASSED : StepStatus.FAILED;
        return new StepResult(
                step.name(), status, response,
                score.faithfulness(), score.confidence(), score.explanation(),
                System.currentTimeMillis() - start
        );
    }
}
