package com.masterclass.multiagent;

import com.masterclass.multiagent.agent.AnalysisAgent;
import com.masterclass.multiagent.agent.ResearchAgent;
import com.masterclass.multiagent.agent.WriterAgent;
import com.masterclass.shared.guardrails.InputValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

@Service
public class SupervisorAgent {

    private static final String SYSTEM_PROMPT = """
            You are a supervisor agent that orchestrates a team of specialist agents:
            - research: gathers factual background on any topic
            - analyse:  performs critical evaluation of information
            - write:    synthesises findings into a polished final report

            Your job is to:
            1. Understand the user's request.
            2. Call research() to gather information.
            3. Call analyse() with the research findings.
            4. Call write() with both the research and analysis.
            5. Return the writer's final output to the user.

            IMPORTANT: Never skip steps. Always call all three agents in order.
            Never try to answer the question directly from your own knowledge.
            """;

    private final ChatClient chatClient;
    private final InputValidator inputValidator;
    private final Counter delegationCounter;

    public SupervisorAgent(ChatClient.Builder builder,
                           ResearchAgent researchAgent,
                           AnalysisAgent analysisAgent,
                           WriterAgent writerAgent,
                           InputValidator inputValidator,
                           MeterRegistry meterRegistry) {
        this.inputValidator = inputValidator;
        this.delegationCounter = Counter.builder("supervisor.delegations.total")
                .description("Total tool delegations made by the supervisor")
                .register(meterRegistry);

        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // Each sub-agent's @Tool method is registered as a tool
                // The supervisor LLM calls them in order: research → analyse → write
                .defaultTools(researchAgent, analysisAgent, writerAgent)
                .build();
    }

    public String process(String userRequest) {
        var validation = inputValidator.validate(userRequest);
        if (!validation.valid()) throw new IllegalArgumentException(validation.reason());

        delegationCounter.increment();
        return chatClient.prompt().user(userRequest).call().content();
    }
}
