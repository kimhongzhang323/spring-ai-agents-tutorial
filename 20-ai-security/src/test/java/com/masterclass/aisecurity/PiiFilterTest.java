package com.masterclass.aisecurity;

import com.masterclass.aisecurity.filter.PiiFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PiiFilterTest {

    private final PiiFilter piiFilter = new PiiFilter(new SimpleMeterRegistry());

    @ParameterizedTest
    @CsvSource({
            "My email is john@example.com please help, [EMAIL]",
            "Call me at 555-867-5309 thanks,          [PHONE]",
            "SSN is 123-45-6789 urgent,               [SSN]",
            "Card number 4111 1111 1111 1111,          [CREDIT_CARD]"
    })
    void piiIsRedacted(String input, String expectedPlaceholder) {
        String result = piiFilter.redact(input);
        assertThat(result).contains(expectedPlaceholder);
        assertThat(result).doesNotContain(extractPii(input));
    }

    @Test
    void safeTextIsUnchanged() {
        String safe = "Explain quantum computing in simple terms";
        assertThat(piiFilter.redact(safe)).isEqualTo(safe);
    }

    @Test
    void multipleTypesRedactedInOnePas() {
        String text = "Email me at a@b.com or call 555-123-4567";
        String result = piiFilter.redact(text);
        assertThat(result).contains("[EMAIL]").contains("[PHONE]");
        assertThat(result).doesNotContain("a@b.com").doesNotContain("555-123-4567");
    }

    private String extractPii(String input) {
        // crude extraction for test clarity — grab the "interesting" token
        return input.split(" ")[3];
    }
}
