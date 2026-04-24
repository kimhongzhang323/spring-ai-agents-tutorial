package com.masterclass.capstone.tool;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tiny keyword-based retrieval over the underwriting policy markdown.
 * Intentionally NOT using embeddings — keeps the capstone runnable with zero extra models
 * and makes the retrieval deterministic for evaluation tests. Swap with pgvector for prod.
 */
@Component
public class PolicyRetriever {

    private static final Pattern CLAUSE = Pattern.compile("^- §([\\d.]+) (.+)$", Pattern.MULTILINE);
    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "for", "of", "to", "and", "or", "is", "are", "be",
            "with", "in", "on", "at", "by", "as", "must", "not");

    private final List<Clause> clauses = new ArrayList<>();

    public record Clause(String section, String text, Set<String> tokens) {}

    @PostConstruct
    public void load() throws IOException {
        String text;
        try (var in = new ClassPathResource("data/policies/underwriting-policy.md").getInputStream()) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        var m = CLAUSE.matcher(text);
        while (m.find()) {
            String section = "§" + m.group(1);
            String body = m.group(2);
            clauses.add(new Clause(section, body, tokenize(section + " " + body)));
        }
    }

    @Tool(description = """
            Search the underwriting policy for the clauses most relevant to the given query.
            Returns up to `k` clauses formatted as "§X.Y: clause text". Cite these sections
            by their §-identifier in any compliance rationale.
            """)
    public List<String> retrievePolicy(
            @ToolParam(description = "Free-text query, e.g. 'self-employed income verification'") String query,
            @ToolParam(description = "Max number of clauses to return (typical: 3)") int k) {

        Set<String> qTokens = tokenize(query);
        return clauses.stream()
                .map(c -> new Scored(c, overlap(qTokens, c.tokens())))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.score).reversed())
                .limit(Math.max(1, k))
                .map(s -> s.clause.section() + ": " + s.clause.text())
                .toList();
    }

    private static Set<String> tokenize(String s) {
        return java.util.Arrays.stream(s.toLowerCase().split("[^a-z0-9§.]+"))
                .filter(t -> !t.isBlank() && !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private static int overlap(Set<String> a, Set<String> b) {
        int n = 0;
        for (String t : a) if (b.contains(t)) n++;
        return n;
    }

    private record Scored(Clause clause, int score) {}
}
