package com.masterclass.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the golden dataset YAML parses correctly without any LLM call.
 * Fails fast if someone edits the YAML and breaks the schema.
 */
class GoldenDatasetLoadTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void goldenDatasetParsesWithoutError() throws IOException {
        var resource = new ClassPathResource("eval/golden-dataset.yml");
        List<EvalCase> cases = yaml.readValue(resource.getInputStream(), new TypeReference<>() {});

        assertThat(cases).hasSizeGreaterThanOrEqualTo(3);
        cases.forEach(c -> {
            assertThat(c.id()).isNotBlank();
            assertThat(c.question()).isNotBlank();
            assertThat(c.contexts()).isNotEmpty();
            assertThat(c.expectedAnswer()).isNotBlank();
        });
    }
}
