package com.masterclass.capstone.agent;

import com.masterclass.capstone.domain.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudDetectionAgent {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionAgent.class);

    private final ChatClient chatClient;

    public FraudDetectionAgent(@Qualifier("fraudChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<Finding> screen(String jobId, String applicantId) {
        log.info("[job={}] FraudDetectionAgent starting for {}", jobId, applicantId);

        String raw = chatClient.prompt()
                .system("""
                        You are a fraud detection specialist. Call screenForFraud for the applicant,
                        then return a JSON array of findings. Each finding:
                          id (FR-001, FR-002 ...),
                          source "FRAUD",
                          statement (one factual sentence citing the fraud signal),
                          severity (INFO | WARNING | BLOCKER — use BLOCKER if score >= 75).
                        Return ONLY the JSON array.
                        """)
                .user("Screen fraud for applicantId: " + applicantId)
                .call()
                .content();

        return CreditAnalysisAgent.parseFindings(jobId, raw, "FRAUD");
    }
}
