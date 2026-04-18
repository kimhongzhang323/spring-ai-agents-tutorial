package com.masterclass.eval;

import java.util.List;

public record EvalReport(
        List<EvalScore> scores,
        double threshold
) {
    public double averageFaithfulness() {
        return scores.stream().mapToDouble(EvalScore::faithfulness).average().orElse(0);
    }

    public double averageRelevance() {
        return scores.stream().mapToDouble(EvalScore::relevance).average().orElse(0);
    }

    public long passCount() {
        return scores.stream().filter(s -> s.passed(threshold)).count();
    }

    public double passRate() {
        return scores.isEmpty() ? 0 : (double) passCount() / scores.size();
    }

    public boolean overallPassed() {
        return passRate() >= threshold;
    }
}
