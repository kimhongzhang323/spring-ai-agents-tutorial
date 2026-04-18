package com.masterclass.structured;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.structured.domain.InvoiceData;
import com.masterclass.structured.domain.ProductReview;
import com.masterclass.structured.domain.ResumeData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/extract")
@Tag(name = "Structured Output", description = "Module 03 – entity extraction with BeanOutputConverter")
@SecurityRequirement(name = "bearerAuth")
public class ExtractionController {

    private final ExtractionService extractionService;

    public ExtractionController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping("/invoice")
    @Operation(
            summary = "Extract structured invoice data",
            description = "Parses raw invoice text into a typed InvoiceData record. " +
                    "Retries up to 3 times if the LLM returns malformed JSON.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Extracted invoice",
                            content = @Content(schema = @Schema(implementation = InvoiceData.class))),
                    @ApiResponse(responseCode = "422", description = "Extraction failed after retries",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    public ResponseEntity<InvoiceData> extractInvoice(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(extractionService.extractInvoice(request.message()));
    }

    @PostMapping("/review")
    @Operation(
            summary = "Analyze product review",
            description = "Returns structured sentiment, pros, cons, and inferred rating.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ProductReview.class)))
            }
    )
    public ResponseEntity<ProductReview> analyzeReview(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(extractionService.analyzeReview(request.message()));
    }

    @PostMapping("/resume")
    @Operation(
            summary = "Extract resume / CV data",
            description = "Extracts name, skills, experience, education, and infers years of experience.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResumeData.class)))
            }
    )
    public ResponseEntity<ResumeData> extractResume(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(extractionService.extractResume(request.message()));
    }
}
