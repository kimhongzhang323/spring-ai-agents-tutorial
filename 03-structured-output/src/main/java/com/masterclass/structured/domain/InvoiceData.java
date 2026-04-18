package com.masterclass.structured.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Jackson annotations drive the JSON schema that BeanOutputConverter injects into the prompt.
 * The LLM reads the schema and produces conforming JSON — the converter parses it back into this record.
 *
 * Rule: every field that is optional in real invoices must be declared nullable here,
 * or the LLM may hallucinate a value rather than omit the field.
 */
@JsonClassDescription("Structured data extracted from an invoice document")
public record InvoiceData(

        @JsonProperty(required = true)
        @JsonPropertyDescription("Invoice number or ID as printed on the document")
        String invoiceNumber,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Name of the vendor or supplier issuing the invoice")
        String vendorName,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Invoice date in ISO-8601 format (YYYY-MM-DD)")
        String invoiceDate,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Total amount due including tax, as a numeric value")
        Double totalAmount,

        @JsonPropertyDescription("Currency code (e.g. USD, EUR, GBP). Null if not stated.")
        String currency,

        @JsonProperty(required = true)
        @JsonPropertyDescription("List of individual line items on the invoice")
        List<LineItem> lineItems
) {
    @JsonClassDescription("A single line item on the invoice")
    public record LineItem(
            @JsonPropertyDescription("Description of the product or service")
            String description,

            @JsonPropertyDescription("Quantity ordered")
            Integer quantity,

            @JsonPropertyDescription("Unit price")
            Double unitPrice,

            @JsonPropertyDescription("Line total (quantity × unitPrice)")
            Double lineTotal
    ) {}
}
