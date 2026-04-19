package com.masterclass.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Map-Reduce summarization pattern — a foundational technique for
 * processing documents that exceed the LLM's context window.
 *
 * <h2>The problem</h2>
 * A 200-page PDF has ~150,000 tokens. Even large-context models (128K tokens) struggle
 * with quality at full context. Chunked summarization (Map-Reduce) produces better summaries
 * for long documents by summarizing small pieces and then combining the summaries.
 *
 * <h2>Map-Reduce pattern</h2>
 * <pre>
 *  Document chunks: [C1, C2, C3, ... C50]
 *
 *  MAP phase (parallel in production):
 *    S1 = summarize(C1)
 *    S2 = summarize(C2)
 *    ...
 *    S50 = summarize(C50)
 *
 *  REDUCE phase (recursive if needed):
 *    Final = synthesize([S1, S2, ... S50])
 * </pre>
 *
 * <h2>When to use each approach</h2>
 * <ul>
 *   <li>Short document (< 8K tokens): single-call summarization</li>
 *   <li>Medium document (8K–128K tokens): stuff all chunks into context (Stuff pattern)</li>
 *   <li>Long document (> 128K tokens): Map-Reduce with this service</li>
 * </ul>
 *
 * Production note: the map phase should be parallelised with CompletableFuture or
 * Project Reactor. This implementation is sequential for readability.
 */
@Service
public class DocumentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSummaryService.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private static final String MAP_PROMPT = """
            Summarize the following text excerpt in 2-3 sentences.
            Preserve key facts, numbers, dates, and conclusions.
            Text:
            """;

    private static final String REDUCE_PROMPT = """
            You are given multiple summaries from different sections of a document.
            Synthesize them into a single coherent summary of 3-5 sentences.
            Identify the main themes, key findings, and important conclusions.

            Section summaries:
            """;

    public DocumentSummaryService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultSystem("You are an expert document summarizer. Be concise and accurate.")
                .build();
    }

    /**
     * Summarize all chunks from a given source using Map-Reduce.
     *
     * The chunks are retrieved from the vector store by performing a very broad
     * similarity search against the source name (not ideal for production — in
     * production, use metadata-only filtering or a separate document store).
     */
    public SummaryResult summarizeSource(String source, String topicHint) {
        // Retrieve all chunks for this source via broad search + metadata filter
        var searchRequest = SearchRequest.builder()
                .query(topicHint.isEmpty() ? source : topicHint)
                .topK(50) // Retrieve many chunks for full-document coverage
                .similarityThreshold(0.0) // Accept all chunks from this source
                .build();

        List<Document> chunks = vectorStore.similaritySearch(searchRequest).stream()
                .filter(doc -> source.equals(doc.getMetadata().get("source")))
                .toList();

        if (chunks.isEmpty()) {
            return new SummaryResult("No documents found for source: " + source, 0, 0);
        }

        log.info("Map-Reduce summarization: {} chunks from source '{}'", chunks.size(), source);

        // MAP phase: summarize each chunk independently
        List<String> chunkSummaries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            log.debug("MAP phase: summarizing chunk {}/{}", i + 1, chunks.size());
            String summary = chatClient.prompt()
                    .user(MAP_PROMPT + chunks.get(i).getText())
                    .call()
                    .content();
            chunkSummaries.add("Section " + (i + 1) + ": " + summary);
        }

        // REDUCE phase: if too many summaries, apply recursive reduction
        String finalSummary;
        if (chunkSummaries.size() <= 10) {
            finalSummary = reduce(chunkSummaries);
        } else {
            // Recursive reduction: group summaries into batches of 10, reduce each batch
            log.debug("REDUCE phase: recursive reduction of {} summaries", chunkSummaries.size());
            List<String> intermediateSummaries = new ArrayList<>();
            for (int i = 0; i < chunkSummaries.size(); i += 10) {
                List<String> batch = chunkSummaries.subList(i, Math.min(i + 10, chunkSummaries.size()));
                intermediateSummaries.add(reduce(batch));
            }
            finalSummary = reduce(intermediateSummaries);
        }

        return new SummaryResult(finalSummary, chunks.size(), chunkSummaries.size());
    }

    /**
     * Quick single-call summarization for short texts that fit in context.
     * Suitable for individual chunks or short documents under ~3000 tokens.
     */
    public String summarizeText(String text) {
        return chatClient.prompt()
                .user("Summarize the following text in 2-3 concise sentences:\n\n" + text)
                .call()
                .content();
    }

    private String reduce(List<String> summaries) {
        String combined = String.join("\n\n", summaries);
        return chatClient.prompt()
                .user(REDUCE_PROMPT + combined)
                .call()
                .content();
    }

    public record SummaryResult(String summary, int chunksProcessed, int mapPhaseOutputs) {}
}
