package com.masterclass.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EvalReportTest {

    @Test
    void passRateCalculatedCorrectly() {
        var scores = List.of(
                new EvalScore("q1", 0.9, 0.8, "good"),  // overall 0.85 → passes 0.7
                new EvalScore("q2", 0.5, 0.6, "meh"),   // overall 0.55 → fails 0.7
                new EvalScore("q3", 1.0, 1.0, "perfect") // overall 1.0 → passes 0.7
        );
        var report = new EvalReport(scores, 0.7);

        assertThat(report.passCount()).isEqualTo(2);
        assertThat(report.passRate()).isCloseTo(2.0 / 3.0, within(0.01));
        assertThat(report.averageFaithfulness()).isCloseTo(0.8, within(0.01));
    }

    @Test
    void overallPassedRequiresPassRateAboveThreshold() {
        var allFail = List.of(new EvalScore("q1", 0.1, 0.1, "bad"));
        assertThat(new EvalReport(allFail, 0.7).overallPassed()).isFalse();

        var allPass = List.of(new EvalScore("q1", 0.9, 0.9, "good"));
        assertThat(new EvalReport(allPass, 0.7).overallPassed()).isTrue();
    }

    @Test
    void emptyDatasetDoesNotThrow() {
        var report = new EvalReport(List.of(), 0.7);
        assertThat(report.passRate()).isZero();
        assertThat(report.averageFaithfulness()).isZero();
    }
}
