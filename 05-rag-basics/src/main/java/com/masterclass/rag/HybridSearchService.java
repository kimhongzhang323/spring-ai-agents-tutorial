package com.masterclass.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Demonstrates advanced RAG retrieval patterns beyond the basic similarity search.
 *
 * <h2>Why hybrid search matters</h2>
 * Pure vector search finds semantically similar chunks but can miss exact keyword matches
 * (e.g., product codes, version numbers, names). Metadata filtering further narrows results
 * to a specific document source, date range, or category before the similarity computation.
 *
 * <h2>Pattern: Metadata-filtered retrieval</h2>
 * <pre>
 *   SearchRequest.builder()
 *       .query(question)
 *       .topK(5)
 *       .filterExpression("source == 'annual-report-2024'")
 *       .build()
 * </pre>
 * This translates to a WHERE clause in PGVector: {@code WHERE metadata->>'source' = 'annual-report-2024'}.
 *
 * <h2>Pattern: Two-phase retrieval (retrieve-then-rerank)</h2>
 * 1. Retrieve a larger candidate set (topK = 20) with looser threshold.
 * 2. Rerank using a cross-encoder or LLM judge to pick the top-5 most relevant.
 * This module shows step 1; module 12 covers LLM-as-judge reranking.
 *
 * <h2>Source citation</h2>
 * Every ingested chunk includes {@code source} metadata. We extract it from retrieved
 * documents and append citations to the LLM answer, so users can verify the information.
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RagProperties props;

    public HybridSearchService(VectorStore vectorStore, ChatClient.Builder builder, RagProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
        this.chatClient = builder
                .defaultSystem("""
                        You are a precise research assistant. Answer using only the provided context.
                        Always cite sources. Format: "Answer: ... [Source: filename]"
                        If the context doesn't contain the answer, say so explicitly.
                        """)
                .build();
    }

    /**
     * Query restricted to a specific source document.
     * Useful when the user wants to interrogate a single uploaded file.
     */
    public SearchResult askWithSourceFilter(String question, String sourceFilter) {
        var filter = new FilterExpressionBuilder()
                .eq("source", sourceFilter)
                .build();

        var searchRequest = SearchRequest.builder()
                .query(question)
                .topK(props.topK())
                .similarityThreshold(props.similarityThreshold())
                .filterExpression(filter)
                .build();

        List<Document> chunks = vectorStore.similaritySearch(searchRequest);
        log.debug("Source-filtered search for '{}' in '{}': {} chunks found", question, sourceFilter, chunks.size());

        if (chunks.isEmpty()) {
            return new SearchResult("No relevant content found in source: " + sourceFilter, List.of(), 0);
        }

        // Build context string with inline source markers
        String context = buildContextWithSources(chunks);

        String answer = chatClient.prompt()
                .user("Context:\n" + context + "\n\nQuestion: " + question)
                .call()
                .content();

        List<String> sources = chunks.stream()
                .map(d -> (String) d.getMetadata().getOrDefault("source", "unknown"))
                .distinct()
                .toList();

        return new SearchResult(answer, sources, chunks.size());
    }

    /**
     * Two-phase retrieval: over-fetch candidates, then let the LLM pick the most relevant.
     *
     * This is a lightweight alternative to a dedicated reranker model. The LLM is asked
     * to select the top-3 most relevant chunks before generating its final answer.
     * Trade-off: costs extra tokens but produces more focused answers.
     */
    public SearchResult askWithLlmReranking(String question) {
        // Phase 1: retrieve a wider candidate set (3x the normal topK)
        int candidateCount = props.topK() * 3;
        var searchRequest = SearchRequest.builder()
                .query(question)
                .topK(candidateCount)
                .similarityThreshold(props.similarityThreshold() - 0.1) // slightly looser
                .build();

        List<Document> candidates = vectorStore.similaritySearch(searchRequest);
        if (candidates.isEmpty()) {
            return new SearchResult("No relevant content found in the knowledge base.", List.of(), 0);
        }

        log.debug("Two-phase retrieval: {} candidates for question '{}'", candidates.size(), question);

        // Phase 2: LLM selects the most relevant chunks (reranking without a dedicated model)
        String numberedCandidates = buildNumberedContext(candidates);
        String selectionPrompt = """
                You will receive %d candidate text chunks. Select the %d most relevant to the question.
                Reply with ONLY the numbers (e.g. "1, 3, 7"), nothing else.

                Question: %s

                Candidates:
                %s
                """.formatted(candidates.size(), props.topK(), question, numberedCandidates);

        String selection = chatClient.prompt().user(selectionPrompt).call().content();

        // Parse selected chunk indices
        List<Document> reranked = parseSelection(selection, candidates);

        // Phase 3: answer with the reranked context
        String context = buildContextWithSources(reranked);
        String answer = chatClient.prompt()
                .user("Context:\n" + context + "\n\nQuestion: " + question)
                .call()
                .content();

        List<String> sources = reranked.stream()
                .map(d -> (String) d.getMetadata().getOrDefault("source", "unknown"))
                .distinct()
                .toList();

        return new SearchResult(answer, sources, reranked.size());
    }

    /**
     * Direct similarity search — returns raw chunks without generating an answer.
     * Useful for building search UIs, debugging retrieval quality, or pipelines
     * that process the chunks independently.
     */
    public List<ScoredChunk> similaritySearch(String query) {
        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(props.topK())
                .similarityThreshold(props.similarityThreshold())
                .build();

        return vectorStore.similaritySearch(searchRequest).stream()
                .map(doc -> new ScoredChunk(
                        doc.getText(),
                        (String) doc.getMetadata().getOrDefault("source", "unknown"),
                        doc.getMetadata()))
                .toList();
    }

    private String buildContextWithSources(List<Document> docs) {
        var sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "unknown");
            sb.append("[").append(i + 1).append("] (Source: ").append(source).append(")\n");
            sb.append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    private String buildNumberedContext(List<Document> docs) {
        var sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            sb.append(i + 1).append(". ").append(docs.get(i).getText(), 0,
                    Math.min(200, docs.get(i).getText().length())).append("...\n\n");
        }
        return sb.toString();
    }

    private List<Document> parseSelection(String selection, List<Document> candidates) {
        try {
            return java.util.Arrays.stream(selection.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> Integer.parseInt(s) - 1) // convert to 0-indexed
                    .filter(i -> i >= 0 && i < candidates.size())
                    .map(candidates::get)
                    .toList();
        } catch (NumberFormatException e) {
            log.warn("Could not parse reranking selection '{}', using first {} candidates", selection, props.topK());
            return candidates.subList(0, Math.min(props.topK(), candidates.size()));
        }
    }

    public record SearchResult(String answer, List<String> sources, int chunksUsed) {}

    public record ScoredChunk(String text, String source, java.util.Map<String, Object> metadata) {}
}
