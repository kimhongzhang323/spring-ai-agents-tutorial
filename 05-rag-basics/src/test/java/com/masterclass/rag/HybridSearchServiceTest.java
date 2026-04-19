package com.masterclass.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for the hybrid search and metadata-filtered retrieval patterns.
 *
 * These tests verify the service's logic (source attribution, empty corpus handling,
 * chunk counting) without hitting PGVector or making real LLM calls.
 *
 * The ChatClient is hard to mock because it uses a fluent builder chain.
 * We use a ChatClient.Builder stub that returns a fixed response string.
 * In Spring AI 1.x this is achieved by mocking at the ChatModel level.
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock VectorStore vectorStore;
    @Mock ChatClient.Builder chatClientBuilder;
    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;

    RagProperties props = new RagProperties(); // defaults: topK=5, threshold=0.7

    @BeforeEach
    void setup() {
        when(chatClientBuilder.defaultSystem(any())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void similaritySearchReturnsChunksWithSourceMetadata() {
        List<Document> mockDocs = List.of(
                new Document("Spring AI is a framework.", Map.of("source", "spring-ai-docs")),
                new Document("It supports OpenAI and Ollama.", Map.of("source", "spring-ai-docs"))
        );
        when(vectorStore.similaritySearch(any())).thenReturn(mockDocs);

        var service = new HybridSearchService(vectorStore, chatClientBuilder, props);
        List<HybridSearchService.ScoredChunk> chunks = service.similaritySearch("Spring AI");

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(c -> assertThat(c.source()).isEqualTo("spring-ai-docs"));
    }

    @Test
    void similaritySearchReturnsEmptyListWhenNoResults() {
        when(vectorStore.similaritySearch(any())).thenReturn(List.of());

        var service = new HybridSearchService(vectorStore, chatClientBuilder, props);
        List<HybridSearchService.ScoredChunk> chunks = service.similaritySearch("unknown topic");

        assertThat(chunks).isEmpty();
    }

    @Test
    void askWithSourceFilterReturnsNoContentMessageWhenEmpty() {
        when(vectorStore.similaritySearch(any())).thenReturn(List.of());

        var service = new HybridSearchService(vectorStore, chatClientBuilder, props);
        HybridSearchService.SearchResult result = service.askWithSourceFilter("What is RAG?", "missing-doc");

        assertThat(result.answer()).contains("No relevant content found");
        assertThat(result.sources()).isEmpty();
        assertThat(result.chunksUsed()).isZero();
    }

    @Test
    void askWithSourceFilterReturnsSourceAttributionInResult() {
        when(vectorStore.similaritySearch(any())).thenReturn(List.of(
                new Document("RAG retrieves relevant chunks.", Map.of("source", "rag-guide")),
                new Document("Then injects them into the prompt.", Map.of("source", "rag-guide"))
        ));
        when(callSpec.content()).thenReturn("RAG stands for Retrieval-Augmented Generation. [Source: rag-guide]");

        var service = new HybridSearchService(vectorStore, chatClientBuilder, props);
        HybridSearchService.SearchResult result = service.askWithSourceFilter("What is RAG?", "rag-guide");

        assertThat(result.answer()).isNotEmpty();
        assertThat(result.sources()).containsExactly("rag-guide");
        assertThat(result.chunksUsed()).isEqualTo(2);
    }

    @Test
    void chunksFromDifferentSourcesAreDeduplicatedInSourceList() {
        when(vectorStore.similaritySearch(any())).thenReturn(List.of(
                new Document("Doc A content", Map.of("source", "doc-a")),
                new Document("Doc A more content", Map.of("source", "doc-a")),
                new Document("Doc B content", Map.of("source", "doc-b"))
        ));
        when(callSpec.content()).thenReturn("Combined answer from both sources.");

        var service = new HybridSearchService(vectorStore, chatClientBuilder, props);
        HybridSearchService.SearchResult result = service.askWithSourceFilter("topic", "any");

        assertThat(result.sources()).containsExactlyInAnyOrder("doc-a", "doc-b");
        assertThat(result.sources()).doesNotHaveDuplicates();
    }
}
