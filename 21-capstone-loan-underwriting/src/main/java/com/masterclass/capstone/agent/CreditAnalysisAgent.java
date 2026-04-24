package com.masterclass.capstone.agent;

import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.util.FindingParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(CreditAnalysisAgent.class);

    private final ChatClient chatClient;

    public CreditAnalysisAgent(@Qualifier("creditChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<Finding> analyze(String jobId, String applicantId) {
        log.info("[job={}] CreditAnalysisAgent starting for {}", jobId, applicantId);

        String raw = chatClient.prompt()
                .system("""
                        You are a credit risk analyst. Call pullCreditReport for the applicant,
                        then return a JSON array of findings. Each finding must have:
                          id (e.g. CR-001, CR-002 — increment per finding),
                          source (always "CREDIT"),
                          statement (one factual sentence, no opinions),
                          severity (INFO | POSITIVE | WARNING | BLOCKER).
                        Return ONLY the JSON array, no prose.
                        """)
                .user("Analyze credit for applicantId: " + applicantId)
                .call()
                .content();

        return parseFindings(jobId, raw, "CREDIT");
    }

    static List<Finding> parseFindings(String jobId, String raw, String source) {
        try {
            String json = raw == null ? "[]" : raw.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            List<Finding> findings = FindingParser.parse(json);
            if (findings.isEmpty()) {
                log.warn("[job={}] {} agent returned no parseable findings", jobId, source);
            }
            return findings;
        } catch (Exception e) {
            log.warn("[job={}] Failed to parse {} findings: {}", jobId, source, e.getMessage());
            return List.of(new Finding(source.substring(0, 2) + "-000", source,
                    "Parse fallback: " + (raw == null ? "null" : raw.strip().replace('\n', ' ')),
                    Finding.Severity.INFO));
        }
    }
}
