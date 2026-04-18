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

        // Few-shot examples must be present in the rendered template
        assertThat(rendered).contains("POSITIVE");
        assertThat(rendered).contains("NEGATIVE");
        assertThat(rendered).contains("NEUTRAL");
        assertThat(rendered).contains("Great product!");
    }
}
