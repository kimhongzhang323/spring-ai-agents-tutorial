package com.masterclass.structured.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("Structured analysis of a product review")
public record ProductReview(

        @JsonProperty(required = true)
        @JsonPropertyDescription("Sentiment: POSITIVE, NEGATIVE, or NEUTRAL")
        Sentiment sentiment,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Rating inferred from review tone, 1 (worst) to 5 (best)")
        int inferredRating,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Key positive aspects mentioned, empty list if none")
        List<String> pros,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Key negative aspects mentioned, empty list if none")
        List<String> cons,

        @JsonPropertyDescription("Summary of the review in one sentence")
        String summary
) {
    public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }
}
