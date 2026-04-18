package com.masterclass.guardrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContentModeratorTest {

    private final ContentModerator moderator = new ContentModerator();

    @ParameterizedTest
    @ValueSource(strings = {
            "hack tutorial for beginners",
            "malware guide step by step",
            "how to synthesize drugs at home"
    })
    void blocksHarmfulContent(String input) {
        var result = moderator.moderate(input);
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).isNotBlank();
    }

    @Test
    void safeContentPasses() {
        var result = moderator.moderate("What is the capital of France?");
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void nullInputIsSafe() {
        assertThat(moderator.moderate(null).isBlocked()).isFalse();
    }
}
