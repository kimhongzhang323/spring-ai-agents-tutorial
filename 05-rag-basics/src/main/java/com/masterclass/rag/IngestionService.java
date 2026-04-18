package com.masterclass.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final RagProperties props;

    public IngestionService(VectorStore vectorStore, RagProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    /**
     * Ingest a PDF file:
     * 1. Read pages via PagePdfDocumentReader
     * 2. Split into overlapping token chunks via TokenTextSplitter
     * 3. Embed + store in PGVector (embedding happens inside VectorStore.add())
     */
    public IngestionResult ingestPdf(MultipartFile file, String source) throws IOException {
        log.info("Ingesting PDF: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        Resource resource = file.getResource();
        var reader = new PagePdfDocumentReader(resource);
        List<Document> pages = reader.get();

        var splitter = new TokenTextSplitter(props.chunkSize(), props.chunkOverlap(), 5, 10000, true);
        List<Document> chunks = splitter.apply(pages);

        // Tag each chunk with source metadata so we can cite it in answers
        chunks.forEach(doc -> doc.getMetadata().put("source", source));

        vectorStore.add(chunks);
        log.info("Ingested {} chunks from {}", chunks.size(), file.getOriginalFilename());
        return new IngestionResult(file.getOriginalFilename(), pages.size(), chunks.size(), source);
    }

    /** Ingest plain text directly (useful for testing and small corpora). */
    public IngestionResult ingestText(String text, String source) {
        var splitter = new TokenTextSplitter(props.chunkSize(), props.chunkOverlap(), 5, 10000, true);
        List<Document> chunks = splitter.apply(
                List.of(new Document(text, Map.of("source", source))));
        vectorStore.add(chunks);
        log.info("Ingested {} chunks from text source '{}'", chunks.size(), source);
        return new IngestionResult(source, 1, chunks.size(), source);
    }

    public record IngestionResult(String fileName, int pageCount, int chunkCount, String source) {}
}
