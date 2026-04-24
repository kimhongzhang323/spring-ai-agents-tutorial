package com.masterclass.capstone.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.LoanApplication;
import com.masterclass.capstone.domain.UnderwritingDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Final adjudicator. Uses a dedicated ChatClient with no tools — it receives
 * pre-collected finding text and produces a structured decision with citations.
 *
 * Injecting a separate @Qualifier bean here makes it easy to swap this client
 * to a stronger model (gpt-4o, claude-opus) via a cloud @Profile config.
 */
@Service
public class UnderwritingSupervisor {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingSupervisor.class);

    private final ChatClient chatClient;
    private final ObjectMapper mapper;

    public UnderwritingSupervisor(@Qualifier("adjudicationChatClient") ChatClient chatClient,
                                   ObjectMapper mapper) {
        this.chatClient = chatClient;
        this.mapper = mapper;
    }

    public UnderwritingDecision adjudicate(String jobId, LoanApplication app, List<Finding> evidence) {
        log.info("[job={}] UnderwritingSupervisor adjudicating with {} findings", jobId, evidence.size());

        String evidenceText = evidence.stream()
                .map(f -> "  %s [%s/%s]: %s".formatted(f.id(), f.source(), f.severity(), f.statement()))
                .collect(Collectors.joining("\n"));

        String raw = chatClient.prompt()
                .system("""
                        You are a senior loan underwriter making a final credit decision.
                        You receive structured evidence from specialist agents.

                        RULES:
                        1. Output ONLY valid JSON matching this exact schema:
                           {
                             "outcome": "APPROVED|CONDITIONALLY_APPROVED|DECLINED|REFER_TO_HUMAN",
                             "approvedAmount": <number or 0 if declined>,
                             "approvedRatePercent": <number or 0 if declined>,
                             "rationale": [
                               { "statement": "...", "citedFindingIds": ["XX-001", "XX-002"] }
                             ]
                           }
                        2. Every rationale entry MUST cite at least one finding ID from the evidence list.
                        3. NEVER mention protected characteristics (age, gender, marital status, national origin).
                        4. For DECLINED, at least one rationale item must cite a CO-xxx (compliance) finding.
                        5. If any BLOCKER finding exists, outcome must be DECLINED or REFER_TO_HUMAN.
                        """)
                .user("""
                        Application: %s | Amount: %.0f | Term: %d months | Purpose: %s
                        Evidence:
                        %s
                        Adjudicate this loan.
                        """.formatted(app.applicantId(), app.loanAmount(), app.termMonths(), app.purpose(), evidenceText))
                .call()
                .content();

        return parseDecision(jobId, app, evidence, raw);
    }

    private UnderwritingDecision parseDecision(String jobId, LoanApplication app,
                                               List<Finding> evidence, String raw) {
        try {
            String json = raw == null ? "{}" : raw.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            var node = mapper.readTree(json);
            var outcome = UnderwritingDecision.Outcome.valueOf(node.get("outcome").asText());
            double amount = node.get("approvedAmount").asDouble();
            double rate = node.get("approvedRatePercent").asDouble();
            List<UnderwritingDecision.RationaleItem> rationale = new ArrayList<>();
            for (var item : node.get("rationale")) {
                String stmt = item.get("statement").asText();
                List<String> cites = new ArrayList<>();
                item.get("citedFindingIds").forEach(n -> cites.add(n.asText()));
                rationale.add(new UnderwritingDecision.RationaleItem(stmt, cites));
            }
            return new UnderwritingDecision(jobId, app.applicantId(), outcome, amount, rate,
                    rationale, evidence, Instant.now());
        } catch (Exception e) {
            log.error("[job={}] Failed to parse supervisor decision, defaulting to REFER_TO_HUMAN: {}", jobId, e.getMessage());
            return new UnderwritingDecision(jobId, app.applicantId(),
                    UnderwritingDecision.Outcome.REFER_TO_HUMAN, 0, 0,
                    List.of(new UnderwritingDecision.RationaleItem(
                            "Parse error in LLM output — human review required: " + e.getMessage(),
                            List.of())),
                    evidence, Instant.now());
        }
    }
}
