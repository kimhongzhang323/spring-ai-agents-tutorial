package com.masterclass.microservices.orchestrator;

import java.time.Instant;
import java.util.List;

public record DemoResult(
        String scenario,
        String description,
        List<DemoStep> steps,
        String summary,
        long durationMs,
        Instant timestamp
) {

    public record DemoStep(String tool, String action, String result, boolean success) {
        public static DemoStep ok(String tool, String action, String result) {
            return new DemoStep(tool, action, result, true);
        }

        public static DemoStep fail(String tool, String action, String error) {
            return new DemoStep(tool, action, error, false);
        }
    }
}
