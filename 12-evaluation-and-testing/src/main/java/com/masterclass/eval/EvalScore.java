package com.masterclass.eval;

public record EvalScore(
        String caseId,
        double faithfulness,   // answer grounded in contexts (0–1)
        double relevance,      // answer addresses the question (0–1)
        String judgeRationale
) {
    public double overall() {
        return (faithfulness + relevance) / 2.0;
    }

    public boolean passed(double threshold) {
        return overall() >= threshold;
    }
}
