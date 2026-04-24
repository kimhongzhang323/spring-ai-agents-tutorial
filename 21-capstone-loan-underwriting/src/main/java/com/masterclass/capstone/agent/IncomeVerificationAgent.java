package com.masterclass.capstone.agent;

import com.masterclass.capstone.domain.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncomeVerificationAgent {

    private static final Logger log = LoggerFactory.getLogger(IncomeVerificationAgent.class);

    private final ChatClient chatClient;

    public IncomeVerificationAgent(@Qualifier("incomeChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<Finding> verify(String jobId, String applicantId) {
        log.info("[job={}] IncomeVerificationAgent starting for {}", jobId, applicantId);

        String raw = chatClient.prompt()
                .system("""
                        You are an income verification officer. Call verifyIncome for the applicant.
                        Return a JSON array of findings:
                          id (IN-001, IN-002 ...),
                          source "INCOME",
                          statement (one factual sentence about verified income, method, or discrepancy),
                          severity (BLOCKER if discrepancy > 10%, WARNING if employment < 2 years, else INFO or POSITIVE).
                        Return ONLY the JSON array.
                        """)
                .user("Verify income for applicantId: " + applicantId)
                .call()
                .content();

        return CreditAnalysisAgent.parseFindings(jobId, raw, "INCOME");
    }
}
