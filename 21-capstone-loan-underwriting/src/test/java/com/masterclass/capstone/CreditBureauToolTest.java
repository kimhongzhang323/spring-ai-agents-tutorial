package com.masterclass.capstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.capstone.repository.ApplicantRepository;
import com.masterclass.capstone.tool.CreditBureauTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditBureauToolTest {

    private CreditBureauTool tool;

    @BeforeEach
    void setUp() throws Exception {
        ApplicantRepository repo = new ApplicantRepository(new ObjectMapper());
        repo.seed();
        tool = new CreditBureauTool(repo);
    }

    @Test
    void returnsExpectedFicoForPrimeApplicant() {
        var pull = tool.pullCreditReport("APP-001"); // Maria Chen, FICO 782
        assertThat(pull.ficoScore()).isEqualTo(782);
        assertThat(pull.delinquencies30d()).isZero();
        assertThat(pull.revolvingUtilization()).isLessThan(0.30);
    }

    @Test
    void returnsHighUtilizationForSubprimeApplicant() {
        var pull = tool.pullCreditReport("APP-004"); // Samuel Okafor, FICO 580
        assertThat(pull.ficoScore()).isEqualTo(580);
        assertThat(pull.delinquencies30d()).isGreaterThan(0);
        assertThat(pull.revolvingUtilization()).isGreaterThan(0.50);
    }

    @Test
    void throwsForUnknownApplicant() {
        assertThatThrownBy(() -> tool.pullCreditReport("UNKNOWN-999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown applicant");
    }
}
