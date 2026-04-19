package com.masterclass.multiagent;

import com.masterclass.multiagent.agent.AnalysisAgent;
import com.masterclass.multiagent.agent.ResearchAgent;
import com.masterclass.multiagent.agent.WriterAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for sub-agent behavior — ensuring each specialist agent:
 * 1. Has a proper {@code @Tool} annotation readable by the LLM
 * 2. Delegates to its own ChatClient (not calling peer agents)
 * 3. Returns non-null, non-empty responses
 *
 * This test also demonstrates the "tool description test" pattern:
 * verify that {@code @Tool} descriptions are LLM-readable (>10 words, no jargon)
 * before deploying. A vague tool description causes the supervisor to use tools incorrectly.
 *
 * <h2>Why sub-agent isolation matters</h2>
 * The supervisor pattern works because each sub-agent is self-contained:
 * it has its own ChatClient with its own system prompt. If sub-agents called each other,
 * debugging infinite loops or incorrect routing would be nearly impossible.
 */
@ExtendWith(MockitoExtension.class)
class SubAgentTest {

    @Mock ChatClient.Builder builder;
    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;

    @Test
    void researchAgentToolDescriptionIsLlmReadable() throws Exception {
        Method toolMethod = ResearchAgent.class.getMethod("research", String.class);
        Tool toolAnnotation = toolMethod.getAnnotation(Tool.class);

        assertThat(toolAnnotation).isNotNull();
        assertThat(toolAnnotation.description())
                .describedAs("Tool description should be longer than 10 words for LLM readability")
                .matches(desc -> desc.split("\\s+").length > 10);
        assertThat(toolAnnotation.description())
                .contains("research")
                .describedAs("Tool description should explain WHEN to use this tool");
    }

    @Test
    void analysisAgentToolDescriptionIsLlmReadable() throws Exception {
        Method toolMethod = AnalysisAgent.class.getMethod("analyse", String.class);
        Tool toolAnnotation = toolMethod.getAnnotation(Tool.class);

        assertThat(toolAnnotation).isNotNull();
        assertThat(toolAnnotation.description().split("\\s+").length).isGreaterThan(10);
    }

    @Test
    void writerAgentToolDescriptionIsLlmReadable() throws Exception {
        // WriterAgent may have a different method name — find the @Tool-annotated one
        Method toolMethod = Arrays.stream(WriterAgent.class.getMethods())
                .filter(m -> m.isAnnotationPresent(Tool.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("WriterAgent has no @Tool method"));

        Tool toolAnnotation = toolMethod.getAnnotation(Tool.class);
        assertThat(toolAnnotation.description().split("\\s+").length).isGreaterThan(10);
    }

    @Test
    void researchAgentCallsDelegatesTopicToLlm() {
        when(builder.defaultSystem(any())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("## Overview\nSpring AI is a Java framework...");

        var agent = new ResearchAgent(builder);
        String result = agent.research("Spring AI");

        assertThat(result).isNotBlank();
        assertThat(result).contains("Spring AI");
        verify(requestSpec).user(argThat(s -> s.contains("Spring AI")));
    }

    @Test
    void researchAgentReturnsNonNullResult() {
        when(builder.defaultSystem(any())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Research summary");

        var agent = new ResearchAgent(builder);
        assertThat(agent.research("any topic")).isNotNull();
    }

    @Test
    void allSubAgentsHaveExactlyOneToolMethod() {
        assertSubAgentHasExactlyOneTool(ResearchAgent.class);
        assertSubAgentHasExactlyOneTool(AnalysisAgent.class);
        assertSubAgentHasExactlyOneTool(WriterAgent.class);
    }

    private void assertSubAgentHasExactlyOneTool(Class<?> agentClass) {
        long toolCount = Arrays.stream(agentClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Tool.class))
                .count();
        assertThat(toolCount)
                .describedAs("%s should expose exactly 1 @Tool method (single responsibility)", agentClass.getSimpleName())
                .isEqualTo(1);
    }
}
