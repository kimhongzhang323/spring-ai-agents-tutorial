package com.masterclass.rag;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG", description = "Module 05 – document ingestion and retrieval-augmented generation")
@SecurityRequirement(name = "bearerAuth")
public class RagController {

    private final RagService ragService;
    private final IngestionService ingestionService;

    public RagController(RagService ragService, IngestionService ingestionService) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question answered from ingested documents")
    public ResponseEntity<AgentResponse> ask(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(AgentResponse.of(ragService.ask(request.message())));
    }

    @PostMapping(value = "/ingest/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingest a PDF document (ADMIN only)",
               description = "Chunks, embeds, and stores the PDF in PGVector. Use source param to label the document.")
    public ResponseEntity<IngestionService.IngestionResult> ingestPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "uploaded-document") String source) throws Exception {
        return ResponseEntity.ok(ingestionService.ingestPdf(file, source));
    }

    @PostMapping("/ingest/text")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingest plain text (ADMIN only)")
    public ResponseEntity<IngestionService.IngestionResult> ingestText(
            @Valid @RequestBody AgentRequest request,
            @RequestParam(defaultValue = "text-upload") String source) {
        return ResponseEntity.ok(ingestionService.ingestText(request.message(), source));
    }
}
