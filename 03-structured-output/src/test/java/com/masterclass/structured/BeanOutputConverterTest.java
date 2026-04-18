package com.masterclass.structured;

import com.masterclass.structured.domain.InvoiceData;
import com.masterclass.structured.domain.ProductReview;
import com.masterclass.structured.domain.ResumeData;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that BeanOutputConverter generates a non-empty schema from the Jackson annotations
 * on our domain records — without calling the LLM.
 *
 * If annotations are missing or the record structure is invalid, the schema will be empty
 * and the LLM will have no guidance — causing hallucinated or random output.
 */
class BeanOutputConverterTest {

    @Test
    void invoiceSchemaContainsRequiredFields() {
        var converter = new BeanOutputConverter<>(InvoiceData.class);
        String format = converter.getFormat();

        assertThat(format).isNotBlank();
        assertThat(format).contains("invoiceNumber");
        assertThat(format).contains("vendorName");
        assertThat(format).contains("totalAmount");
        assertThat(format).contains("lineItems");
    }

    @Test
    void reviewSchemaContainsSentimentEnum() {
        var converter = new BeanOutputConverter<>(ProductReview.class);
        String format = converter.getFormat();

        assertThat(format).contains("sentiment");
        assertThat(format).contains("POSITIVE");
        assertThat(format).contains("NEGATIVE");
        assertThat(format).contains("NEUTRAL");
        assertThat(format).contains("inferredRating");
    }

    @Test
    void resumeSchemaContainsNestedTypes() {
        var converter = new BeanOutputConverter<>(ResumeData.class);
        String format = converter.getFormat();

        assertThat(format).contains("fullName");
        assertThat(format).contains("skills");
        assertThat(format).contains("experience");
        assertThat(format).contains("education");
    }

    @Test
    void converterParsesValidJson() {
        var converter = new BeanOutputConverter<>(ProductReview.class);
        String validJson = """
                {
                  "sentiment": "POSITIVE",
                  "inferredRating": 5,
                  "pros": ["fast delivery", "great quality"],
                  "cons": [],
                  "summary": "Excellent product, highly recommend."
                }
                """;

        ProductReview result = converter.convert(validJson);

        assertThat(result.sentiment()).isEqualTo(ProductReview.Sentiment.POSITIVE);
        assertThat(result.inferredRating()).isEqualTo(5);
        assertThat(result.pros()).containsExactly("fast delivery", "great quality");
        assertThat(result.cons()).isEmpty();
    }

    @Test
    void converterParsesJsonWithMarkdownFencesStripped() {
        var converter = new BeanOutputConverter<>(ProductReview.class);
        // LLMs sometimes wrap JSON in markdown fences despite instructions
        String withFences = """
                ```json
                {
                  "sentiment": "NEGATIVE",
                  "inferredRating": 2,
                  "pros": [],
                  "cons": ["arrived broken"],
                  "summary": "Disappointed."
                }
                ```
                """;

        // BeanOutputConverter strips ```json fences before parsing
        ProductReview result = converter.convert(withFences);
        assertThat(result.sentiment()).isEqualTo(ProductReview.Sentiment.NEGATIVE);
    }
}
