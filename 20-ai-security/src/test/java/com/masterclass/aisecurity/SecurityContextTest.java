package com.masterclass.aisecurity;

import com.masterclass.aisecurity.filter.SecurityContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that SecurityContext mutations are fully immutable —
 * no method call should alter another context instance.
 */
class SecurityContextTest {

    @Test
    void withWarning_doesNotMutateOriginal() {
        SecurityContext original = SecurityContext.of("user1", "hello");
        SecurityContext withWarn = original.withWarning("PII detected");

        assertThat(original.warnings()).isEmpty();
        assertThat(withWarn.warnings()).containsExactly("PII detected");
    }

    @Test
    void withSanitized_doesNotShareWarningsList() {
        SecurityContext original = SecurityContext.of("user1", "hello");
        SecurityContext sanitized = original.withSanitized("clean");

        // Mutating sanitized's warnings must not affect original
        SecurityContext sanitizedWithWarn = sanitized.withWarning("warn");
        assertThat(original.warnings()).isEmpty();
        assertThat(sanitized.warnings()).isEmpty();
        assertThat(sanitizedWithWarn.warnings()).hasSize(1);
    }

    @Test
    void multipleWarnings_accumulateCorrectly() {
        SecurityContext ctx = SecurityContext.of("user1", "input")
                .withWarning("w1")
                .withWarning("w2")
                .withWarning("w3");

        assertThat(ctx.warnings()).containsExactly("w1", "w2", "w3");
    }

    @Test
    void withRisk_doesNotShareWarningsList() {
        SecurityContext base = SecurityContext.of("user1", "input").withWarning("existing");
        SecurityContext highRisk = base.withRisk(0.9);

        SecurityContext highRiskWithWarn = highRisk.withWarning("new-warn");
        assertThat(base.warnings()).containsExactly("existing");
        assertThat(highRisk.warnings()).containsExactly("existing");
        assertThat(highRiskWithWarn.warnings()).containsExactly("existing", "new-warn");
    }

    @Test
    void blocked_preservesAllFields() {
        SecurityContext ctx = SecurityContext.of("user1", "bad input")
                .withSanitized("sanitized")
                .withRisk(0.8)
                .withWarning("flagged")
                .blocked();

        assertThat(ctx.blocked()).isTrue();
        assertThat(ctx.sanitizedInput()).isEqualTo("sanitized");
        assertThat(ctx.riskScore()).isEqualTo(0.8);
        assertThat(ctx.warnings()).containsExactly("flagged");
    }
}
