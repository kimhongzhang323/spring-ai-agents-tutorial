package com.masterclass.structured;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.structured.domain.InvoiceData;
import com.masterclass.structured.domain.ProductReview;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtractionController.class)
class ExtractionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ExtractionService extractionService;

    @Test
    @WithMockUser
    void extractInvoiceReturnsTypedResponse() throws Exception {
        var invoice = new InvoiceData(
                "INV-001", "Acme Corp", "2024-01-15", 1500.00, "USD",
                List.of(new InvoiceData.LineItem("Widget A", 10, 150.00, 1500.00))
        );
        when(extractionService.extractInvoice(anyString())).thenReturn(invoice);

        mockMvc.perform(post("/api/v1/extract/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AgentRequest("Invoice INV-001 from Acme Corp..."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.vendorName").value("Acme Corp"))
                .andExpect(jsonPath("$.totalAmount").value(1500.00))
                .andExpect(jsonPath("$.lineItems[0].description").value("Widget A"));
    }

    @Test
    @WithMockUser
    void extractReviewReturnsSentiment() throws Exception {
        var review = new ProductReview(
                ProductReview.Sentiment.POSITIVE, 5,
                List.of("fast delivery", "great quality"), List.of(),
                "Excellent product."
        );
        when(extractionService.analyzeReview(anyString())).thenReturn(review);

        mockMvc.perform(post("/api/v1/extract/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Amazing product!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$.inferredRating").value(5));
    }

    @Test
    @WithMockUser
    void extractionFailureReturns422() throws Exception {
        when(extractionService.extractInvoice(anyString()))
                .thenThrow(new ExtractionService.ExtractionFailedException("Parse failed after 3 retries"));

        mockMvc.perform(post("/api/v1/extract/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentRequest("Some text"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Extraction Failed"))
                .andExpect(jsonPath("$.errorCode").value("EXTRACTION_PARSE_FAILURE"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/extract/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void blankMessageReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/extract/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
