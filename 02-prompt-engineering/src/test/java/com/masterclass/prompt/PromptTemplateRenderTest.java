package com.masterclass.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that template files render correctly without calling the LLM.
 * These tests catch broken variable names (typos in .st files) at build time.
 */
class PromptTemplateRenderTest {

    @Test
    void summarizeTemplateContainsInjectedVariables() {
        var template = new PromptTemplate(new ClassPathResource("prompts/summarize.st"));
        var rendered = template.render(Map.of("text", "sample text", "maxWords", 100));

        assertThat(rendered).contains("sample text");
        assertThat(rendered).contains("100");
        // Template variables should be fully substituted
        assertThat(rendered).doesNotContain("{text}");
        assertThat(rendered).doesNotContain("{maxWords}");
    }

    @Test
    void translateTemplateContainsAllVariables() {
        var template = new PromptTemplate(new ClassPathResource("prompts/translate.st"));
        var rendered = template.render(Map.of(
                "text", "Hello world",
                "sourceLanguage", "English",
                "targetLanguage", "Japanese"
        ));

        assertThat(rendered).contains("Hello world");
        assertThat(rendered).contains("English");
        assertThat(rendered).contains("Japanese");
        assertThat(rendered).doesNotContain("{sourceLanguage}");
    }

    @Test
    void sentimentTemplateContainsFewShotExamples() {
        var template = new PromptTemplate(new ClassPathResource("prompts/classify-sentiment.st"));
        var rendered = template.render(Map.of("text", "Great product!"));

        assertThat(rendered).contains("POSITIVE");
        assertThat(rendered).contains("NEGATIVE");
        assertThat(rendered).contains("NEUTRAL");
        assertThat(rendered).contains("Great product!");
    }

    @Test
    void chainOfThoughtTemplateInjectsProblem() {
        var template = new PromptTemplate(new ClassPathResource("prompts/chain-of-thought.st"));
        var rendered = template.render(Map.of("problem", "What is 17 * 8?"));

        assertThat(rendered).contains("What is 17 * 8?");
        assertThat(rendered).contains("Step 1");
        assertThat(rendered).contains("Final Answer");
        assertThat(rendered).doesNotContain("{problem}");
    }

    @Test
    void codeReviewChainTemplateInjectsCodeAndLanguage() {
        var template = new PromptTemplate(new ClassPathResource("prompts/code-review-chain.st"));
        var rendered = template.render(Map.of("code", "System.out.println(x);", "language", "java"));

        assertThat(rendered).contains("System.out.println(x);");
        assertThat(rendered).contains("java");
        assertThat(rendered).contains("CRITICAL");
        assertThat(rendered).doesNotContain("{code}");
        assertThat(rendered).doesNotContain("{language}");
    }

    @Test
    void metaPromptTemplateInjectsUseCaseAndAudience() {
        var template = new PromptTemplate(new ClassPathResource("prompts/meta-prompt.st"));
        var rendered = template.render(Map.of(
                "useCase", "customer support chatbot",
                "targetAudience", "non-technical users"
        ));

        assertThat(rendered).contains("customer support chatbot");
        assertThat(rendered).contains("non-technical users");
        assertThat(rendered).contains("SYSTEM PROMPT");
        assertThat(rendered).doesNotContain("{useCase}");
    }

    @Test
    void entityExtractTemplateContainsFewShotExamplesAndInjectsText() {
        var template = new PromptTemplate(new ClassPathResource("prompts/few-shot-entity-extract.st"));
        var rendered = template.render(Map.of("text", "Elon Musk visited Berlin."));

        assertThat(rendered).contains("Elon Musk visited Berlin.");
        assertThat(rendered).contains("persons");
        assertThat(rendered).contains("organizations");
        assertThat(rendered).doesNotContain("{text}");
    }
}
