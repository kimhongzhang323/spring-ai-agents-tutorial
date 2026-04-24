package com.masterclass.capstone;

import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.util.FindingParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindingParserTest {

    @Test
    void parsesWellFormedJsonArray() {
        String json = """
                [
                  {"id":"CR-001","source":"CREDIT","statement":"FICO score is 782, classified as prime.","severity":"POSITIVE"},
                  {"id":"CR-002","source":"CREDIT","statement":"Revolving utilization is 18%, below 30% threshold.","severity":"INFO"}
                ]
                """;

        List<Finding> findings = FindingParser.parse(json);

        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).id()).isEqualTo("CR-001");
        assertThat(findings.get(0).severity()).isEqualTo(Finding.Severity.POSITIVE);
        assertThat(findings.get(1).id()).isEqualTo("CR-002");
    }

    @Test
    void handlesMarkdownCodeFence() {
        String fenced = "```json\n[{\"id\":\"FR-001\",\"source\":\"FRAUD\",\"statement\":\"No fraud signals.\",\"severity\":\"INFO\"}]\n```";
        // Strip happens in CreditAnalysisAgent before parse; test the parser directly with clean JSON
        String clean = fenced.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
        List<Finding> findings = FindingParser.parse(clean);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).id()).isEqualTo("FR-001");
    }

    @Test
    void returnsEmptyListForEmptyArray() {
        assertThat(FindingParser.parse("[]")).isEmpty();
    }
}
