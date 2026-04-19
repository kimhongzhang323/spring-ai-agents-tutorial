package com.masterclass.microservices.servicemesh.temporal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AgentActivityImpl implements AgentActivity {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityImpl.class);

    private final ChatClient chatClient;

    public AgentActivityImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String processInput(String input) {
        log.debug("Temporal activity: processInput");
        return chatClient.prompt()
                .system("You are a document processor. Summarize the input clearly.")
                .user(input)
                .call()
                .content();
    }

    @Override
    public String validateOutput(String output) {
        log.debug("Temporal activity: validateOutput");
        if (output == null || output.isBlank()) {
            throw new IllegalStateException("Agent produced empty output — retrying");
        }
        return output;
    }

    @Override
    public String storeResult(String result) {
        log.debug("Temporal activity: storeResult length={}", result.length());
        // In production: persist to PostgreSQL, MongoDB, or S3
        return "{\"status\":\"completed\",\"resultLength\":%d}".formatted(result.length());
    }
}
