package com.masterclass.structured.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("Key data extracted from a resume or CV")
public record ResumeData(

        @JsonProperty(required = true)
        @JsonPropertyDescription("Candidate's full name")
        String fullName,

        @JsonPropertyDescription("Email address if present, null otherwise")
        String email,

        @JsonPropertyDescription("Phone number if present, null otherwise")
        String phone,

        @JsonProperty(required = true)
        @JsonPropertyDescription("List of technical and soft skills mentioned")
        List<String> skills,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Work experience entries in reverse chronological order")
        List<WorkExperience> experience,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Education entries")
        List<Education> education,

        @JsonPropertyDescription("Years of total professional experience inferred from dates, null if cannot be determined")
        Integer inferredYearsExperience
) {
    @JsonClassDescription("A single work experience entry")
    public record WorkExperience(
            String company,
            String title,
            String startDate,
            String endDate,
            String description
    ) {}

    @JsonClassDescription("An education entry")
    public record Education(
            String institution,
            String degree,
            String fieldOfStudy,
            String graduationYear
    ) {}
}
