package com.masterclass.capstone.agent;

import com.masterclass.capstone.domain.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplianceAgent {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAgent.class);

    private final ChatClient chatClient;

    public ComplianceAgent(@Qualifier("complianceChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<Finding> review(String jobId, String applicantId, List<Finding> priorFindings,
                                String loanPurpose, double loanAmount) {
        log.info("[job={}] ComplianceAgent starting policy review", jobId);

        String summary = priorFindings.stream()
                .map(f -> f.id() + ": [" + f.severity() + "] " + f.statement())
                .collect(Collectors.joining("\n"));

        String raw = chatClient.prompt()
                .system("""
                        You are a compliance officer for a consumer lending firm.
                        You have specialist findings from credit, fraud, and income review.
                        Use retrievePolicy to look up relevant §-clauses for any finding that touches
                        credit bands, DTI, income verification, fraud thresholds, or loan limits.
                        Return a JSON array of compliance findings:
                          id (CO-001, CO-002 ...),
                          source "COMPLIANCE",
                          statement (one sentence referencing the §-clause by identifier),
                          severity (BLOCKER if hard stop, WARNING if review needed, else INFO).
                        Return ONLY the JSON array.
                        """)
                .user("""
                        Applicant: %s | Loan purpose: %s | Loan amount: %.0f
                        Specialist findings:
                        %s
                        """.formatted(applicantId, loanPurpose, loanAmount, summary))
                .call()
                .content();

        return CreditAnalysisAgent.parseFindings(jobId, raw, "COMPLIANCE");
    }
}
