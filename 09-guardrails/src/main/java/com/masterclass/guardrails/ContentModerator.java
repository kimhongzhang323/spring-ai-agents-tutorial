package com.masterclass.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Heuristic content moderation applied to both input and output.
 * Production upgrade: replace with OpenAI Moderation API or AWS Comprehend.
 */
@Component
public class ContentModerator {

    private static final List<Pattern> TOXIC_PATTERNS = List.of(
            Pattern.compile("\\b(hack|exploit|malware|ransomware|ddos)\\s+(tutorial|guide|how.to)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(synthesize|manufacture|produce)\\s+(drugs|explosives|weapons)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(child|minor)\\s+(explicit|pornograph)", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> SENSITIVE_TOPIC_PATTERNS = List.of(
            Pattern.compile("\\b(suicide|self.harm)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(bomb|explosive) (making|instructions|recipe)\\b", Pattern.CASE_INSENSITIVE)
    );

    public ModerationResult moderate(String text) {
        if (text == null || text.isBlank()) return ModerationResult.safe();

        for (Pattern pattern : TOXIC_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return ModerationResult.blocked("Content violates usage policy");
            }
        }

        for (Pattern pattern : SENSITIVE_TOPIC_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return ModerationResult.flagged("Content touches a sensitive topic — please rephrase");
            }
        }

        return ModerationResult.safe();
    }

    public record ModerationResult(Status status, String reason) {
        public enum Status { SAFE, FLAGGED, BLOCKED }
        public boolean isBlocked()  { return status == Status.BLOCKED; }
        public boolean isFlagged()  { return status == Status.FLAGGED; }
        public static ModerationResult safe()                  { return new ModerationResult(Status.SAFE,    null); }
        public static ModerationResult flagged(String reason)  { return new ModerationResult(Status.FLAGGED, reason); }
        public static ModerationResult blocked(String reason)  { return new ModerationResult(Status.BLOCKED, reason); }
    }
}
