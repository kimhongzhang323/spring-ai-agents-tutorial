package com.masterclass.antihallucination.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses [CITE: ...] markers from LLM responses and verifies each citation
 * appears verbatim in the grounding documents.
 *
 * Usage: inject the citation format into the system prompt using {@link #citationInstruction()},
 * then call {@link #verifyCitations(String, List)} on the response.
 */
@Component
public class CitationExtractor {

    private static final Logger log = LoggerFactory.getLogger(CitationExtractor.class);

    // Matches [CITE: <quoted fragment>]
    private static final Pattern CITE_PATTERN = Pattern.compile("\\[CITE:\\s*\"([^\"]+)\"\\]");

    public String citationInstruction() {
        return """
                When making a factual claim, you MUST cite the exact source fragment using this format:
                [CITE: "<exact quote from the provided documents>"]
                Only cite text that appears verbatim in the documents. Do not paraphrase.
                """;
    }

    /**
     * @return list of citations that could NOT be found in any grounding document
     */
    public List<String> verifyCitations(String response, List<String> groundingDocs) {
        Matcher matcher = CITE_PATTERN.matcher(response);
        List<String> unverified = new ArrayList<>();

        while (matcher.find()) {
            String citation = matcher.group(1).trim();
            boolean found = groundingDocs.stream()
                    .anyMatch(doc -> doc.contains(citation));
            if (!found) {
                log.warn("Citation not found in grounding docs: \"{}\"", citation);
                unverified.add(citation);
            }
        }

        return unverified;
    }

    /** True when the response contains at least one CITE marker. */
    public boolean hasCitations(String response) {
        return CITE_PATTERN.matcher(response).find();
    }
}
