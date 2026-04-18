package com.masterclass.prompt;

import com.masterclass.prompt.dto.ExpertChatRequest;
import com.masterclass.prompt.dto.SummarizeRequest;
import com.masterclass.prompt.dto.TranslateRequest;
import com.masterclass.shared.guardrails.InputValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptService {

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

    public PromptService(ChatClient.Builder chatClientBuilder, InputValidator inputValidator) {
        // No default system prompt here — each method sets its own
        this.chatClient = chatClientBuilder.build();
        this.inputValidator = inputValidator;
    }

    public String summarize(SummarizeRequest request) {
        validate(request.text());

        // PromptTemplate renders variables into the .st file content
        var template = new PromptTemplate(summarizeTemplate);
        var prompt = template.create(Map.of(
                "text", request.text(),
                "maxWords", request.maxWords()
        ));

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

        // SystemPromptTemplate sets the system message; user message is the question
        var systemTemplate = new SystemPromptTemplate(systemExpertTemplate);
        var systemMessage = systemTemplate.createMessage(Map.of(
                "domain", request.domain(),
                "yearsExperience", request.yearsExperience(),
                "audienceLevel", request.audienceLevel()
        ));

        return chatClient.prompt()
                .system(systemMessage.getContent())
                .user(request.question())
                .call()
                .content();
    }

    private void validate(String input) {
        var result = inputValidator.validate(input);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.reason());
        }
    }
}
