package com.masterclass.banking;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sub-agent: reads account balances and recent transactions.
 * Read-only — no approval required.
 */
@Component
public class AccountsAgent {

    // Stub account data
    private static final Map<String, Double> BALANCES = Map.of(
            "ACC-001", 5432.10,
            "ACC-002", 12800.00,
            "ACC-003", 320.55
    );

    @Tool(description = """
            Return the current balance of a bank account.
            Use when the customer asks 'what is my balance' or 'how much do I have'.
            Requires the account ID (format: ACC-XXX).
            """)
    public String getBalance(String accountId) {
        Double balance = BALANCES.get(accountId.toUpperCase());
        return balance == null
                ? "Account " + accountId + " not found."
                : "Account %s balance: $%.2f".formatted(accountId.toUpperCase(), balance);
    }

    @Tool(description = """
            Return the 5 most recent transactions for a bank account.
            Use when the customer asks about recent activity, charges, or transaction history.
            """)
    public String getRecentTransactions(String accountId) {
        if (!BALANCES.containsKey(accountId.toUpperCase()))
            return "Account " + accountId + " not found.";
        return """
                Recent transactions for %s:
                1. -$45.00  Coffee Shop       2024-01-20
                2. +$2000.00 Salary Deposit   2024-01-18
                3. -$120.00 Electric Bill      2024-01-17
                4. -$67.50  Grocery Store      2024-01-16
                5. -$15.99  Streaming Service  2024-01-15
                """.formatted(accountId.toUpperCase());
    }
}
