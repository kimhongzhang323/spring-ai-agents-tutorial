package com.masterclass.capstone;

import com.masterclass.capstone.tool.PolicyRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRetrieverTest {

    private PolicyRetriever retriever;

    @BeforeEach
    void setUp() throws Exception {
        retriever = new PolicyRetriever();
        retriever.load();
    }

    @Test
    void returnsRelevantClausesForCreditScoreQuery() {
        List<String> results = retriever.retrievePolicy("credit score FICO prime sub-prime", 3);
        assertThat(results).isNotEmpty();
        assertThat(results.stream().anyMatch(r -> r.contains("§1"))).isTrue();
    }

    @Test
    void returnsRelevantClausesForIncomeQuery() {
        List<String> results = retriever.retrievePolicy("self-employed income verification tax return", 3);
        assertThat(results).isNotEmpty();
        assertThat(results.stream().anyMatch(r -> r.contains("§3"))).isTrue();
    }

    @Test
    void respectsKLimit() {
        List<String> results = retriever.retrievePolicy("loan income credit fraud compliance", 2);
        assertThat(results).hasSizeLessThanOrEqualTo(2);
    }
}
