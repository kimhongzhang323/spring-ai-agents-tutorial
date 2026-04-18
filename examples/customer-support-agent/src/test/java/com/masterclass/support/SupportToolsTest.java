package com.masterclass.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportToolsTest {

    private final SupportTools tools = new SupportTools();

    @Test
    void lookupKnownOrder() {
        String result = tools.lookupOrder("ORD-001");
        assertThat(result).contains("ORD-001").contains("SHIPPED");
    }

    @Test
    void lookupUnknownOrderReturnsNotFound() {
        assertThat(tools.lookupOrder("ORD-999")).contains("not found");
    }

    @Test
    void createTicketReturnsTicketId() {
        String result = tools.createTicket("user123", "BILLING", "Double charged");
        assertThat(result).startsWith("Ticket created: TKT-");
    }

    @Test
    void refundOnNonDeliveredOrderIsRejected() {
        String result = tools.processRefund("ORD-002"); // PROCESSING status
        assertThat(result).contains("not applicable");
    }

    @Test
    void refundOnDeliveredOrderSucceeds() {
        String result = tools.processRefund("ORD-003"); // DELIVERED
        assertThat(result).contains("Refund initiated");
    }
}
