package com.masterclass.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests the document ingestion pipeline without hitting PGVector or the embedding model.
 *
 * Key insight: the expensive operations (PDF parsing, embedding, vector storage) are all
 * behind interfaces. We mock VectorStore to verify the chunks look correct, without
 * needing real infrastructure. This makes the test fast and deterministic.
 *
 * What we're testing:
 * - Text is split into chunks (at least one chunk for any non-empty text)
 * - Metadata tagging (source field is present on every chunk)
 * - IngestionResult counts match what was stored
 */
@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    VectorStore vectorStore;

    @Captor
    ArgumentCaptor<List<Document>> chunksCaptor;

    // Use default props (chunkSize=800, chunkOverlap=100)
    RagProperties props = new RagProperties();

    @Test
    void ingestTextProducesAtLeastOneChunk() {
        var service = new IngestionService(vectorStore, props);
        String text = "Spring AI is a framework for building AI applications in Java. ".repeat(20);

        IngestionService.IngestionResult result = service.ingestText(text, "spring-ai-intro");

        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();

        assertThat(chunks).isNotEmpty();
        assertThat(result.chunkCount()).isEqualTo(chunks.size());
        assertThat(result.source()).isEqualTo("spring-ai-intro");
    }

    @Test
    void everyChunkHasSourceMetadata() {
        var service = new IngestionService(vectorStore, props);
        String text = "LangChain4j provides a Java-native API for building AI agents. ".repeat(30);

        service.ingestText(text, "langchain4j-docs");

        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();

        assertThat(chunks).allSatisfy(doc ->
                assertThat(doc.getMetadata()).containsKey("source")
        );
        assertThat(chunks).allSatisfy(doc ->
                assertThat(doc.getMetadata().get("source")).isEqualTo("langchain4j-docs")
        );
    }

    @Test
    void shortTextProducesSingleChunk() {
        var service = new IngestionService(vectorStore, props);
        String shortText = "This is a short document.";

        IngestionService.IngestionResult result = service.ingestText(shortText, "short-doc");

        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();

        assertThat(chunks).hasSize(1);
        assertThat(result.chunkCount()).isEqualTo(1);
    }

    @Test
    void longTextProducesMultipleChunks() {
        var service = new IngestionService(vectorStore, props);
        // Generate text that will definitely exceed chunkSize=800 tokens (~3200 chars)
        String longText = "The quick brown fox jumps over the lazy dog. ".repeat(200);

        IngestionService.IngestionResult result = service.ingestText(longText, "long-doc");

        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(result.chunkCount()).isEqualTo(chunks.size());
    }

    @Test
    void chunksDoNotExceedConfiguredSize() {
        var service = new IngestionService(vectorStore, props);
        String text = "Chunk size validation test content. ".repeat(300);

        service.ingestText(text, "size-test");

        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();

        // Each chunk should be within a reasonable bound of the configured chunk size
        // TokenTextSplitter uses tokens (~4 chars), so 800 tokens ≈ 3200 chars + some overhead
        chunks.forEach(chunk ->
                assertThat(chunk.getText().length()).isLessThan(5000)
        );
    }

    @Test
    void differentSourcesAreTaggedCorrectly() {
        var service = new IngestionService(vectorStore, props);

        service.ingestText("First document content about Java.", "doc-a");
        service.ingestText("Second document content about Python.", "doc-b");

        // Verify the second call also tags source correctly
        verify(vectorStore, org.mockito.Mockito.times(2)).add(chunksCaptor.capture());
        List<List<Document>> allCalls = chunksCaptor.getAllValues();

        assertThat(allCalls.get(0)).allSatisfy(d ->
                assertThat(d.getMetadata().get("source")).isEqualTo("doc-a")
        );
        assertThat(allCalls.get(1)).allSatisfy(d ->
                assertThat(d.getMetadata().get("source")).isEqualTo("doc-b")
        );
    }
}
