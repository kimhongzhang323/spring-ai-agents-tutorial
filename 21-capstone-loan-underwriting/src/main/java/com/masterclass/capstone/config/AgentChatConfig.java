package com.masterclass.capstone.config;

import com.masterclass.capstone.tool.CreditBureauTool;
import com.masterclass.capstone.tool.FraudCheckTool;
import com.masterclass.capstone.tool.IncomeVerificationTool;
import com.masterclass.capstone.tool.PolicyRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Named ChatClient beans, one per specialist concern.
 *
 * All agents share the same underlying ChatModel (configured in application.yml).
 * The split exists so each bean carries only the tools relevant to its domain,
 * which reduces token waste and prompt confusion from unrelated tool descriptions.
 *
 * To route the supervisor to a stronger model on cloud (gpt-4o vs gpt-4o-mini),
 * wire a second ChatModel bean in a cloud @Profile configuration and inject it here.
 */
@Configuration
public class AgentChatConfig {

    @Bean
    @Qualifier("creditChatClient")
    public ChatClient creditChatClient(ChatClient.Builder builder, CreditBureauTool tool) {
        return builder.defaultTools(tool).build();
    }

    @Bean
    @Qualifier("fraudChatClient")
    public ChatClient fraudChatClient(ChatClient.Builder builder, FraudCheckTool tool) {
        return builder.defaultTools(tool).build();
    }

    @Bean
    @Qualifier("incomeChatClient")
    public ChatClient incomeChatClient(ChatClient.Builder builder, IncomeVerificationTool tool) {
        return builder.defaultTools(tool).build();
    }

    @Bean
    @Qualifier("complianceChatClient")
    public ChatClient complianceChatClient(ChatClient.Builder builder, PolicyRetriever tool) {
        return builder.defaultTools(tool).build();
    }

    @Bean
    @Qualifier("adjudicationChatClient")
    public ChatClient adjudicationChatClient(ChatClient.Builder builder) {
        // No tools — supervisor reasons over pre-collected findings text only
        return builder.build();
    }
}
