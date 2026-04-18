package com.masterclass.banking;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Supervisor agent: routes customer requests to AccountsAgent or ActionAgent.
 * Demonstrates the no-peer-calling rule: sub-agents are wired as tools on the supervisor,
 * they never call each other directly.
 */
@Service
public class BankingSupervisorService {

    private static final String SYSTEM = """
            You are a secure banking assistant for Acme Bank.
            You have tools to check account balances, view transactions, and initiate transfers.

            IMPORTANT RULES:
            1. Never reveal account numbers in full — mask the last 3 digits as XXX.
            2. For any money movement (transfers), you MUST use requestTransferApproval first.
               Only call executeApprovedTransfer after confirming the approval is APPROVED.
            3. If the customer's request is ambiguous about which account, ask for clarification.
            4. Never speculate about account balances — always use the getBalance tool.
            """;

    private final ChatClient chatClient;

    public BankingSupervisorService(ChatClient.Builder builder,
                                     AccountsAgent accounts, ActionAgent actions) {
        this.chatClient = builder
                .defaultSystem(SYSTEM)
                .defaultTools(accounts, actions)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt().user(message).call().content();
    }
}
