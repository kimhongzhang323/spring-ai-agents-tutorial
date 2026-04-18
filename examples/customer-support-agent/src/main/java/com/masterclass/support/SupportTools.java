package com.masterclass.support;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulated CRM and order management tools.
 * In production each method would call a real microservice or database.
 */
@Component
public class SupportTools {

    // In-memory stores simulating a CRM and order system
    private final Map<String, String> ticketStore = new ConcurrentHashMap<>();
    private final Map<String, OrderInfo> orderStore = new ConcurrentHashMap<>(Map.of(
            "ORD-001", new OrderInfo("ORD-001", "Widget Pro", "SHIPPED", "2024-01-15"),
            "ORD-002", new OrderInfo("ORD-002", "Gadget Max", "PROCESSING", "2024-01-20"),
            "ORD-003", new OrderInfo("ORD-003", "Doohickey", "DELIVERED", "2024-01-10")
    ));

    @Tool(description = """
            Look up the status of a customer order by order ID.
            Returns order status (PROCESSING, SHIPPED, DELIVERED, CANCELLED),
            product name, and estimated delivery date.
            Use this when the customer asks about order status or delivery.
            """)
    public String lookupOrder(String orderId) {
        var order = orderStore.get(orderId.toUpperCase());
        if (order == null) return "Order " + orderId + " not found.";
        return "Order %s: %s — Status: %s — Date: %s"
                .formatted(order.orderId(), order.product(), order.status(), order.date());
    }

    @Tool(description = """
            Create a support ticket for an issue the customer cannot resolve through conversation.
            Use this for: refund requests, billing disputes, technical issues requiring investigation.
            Returns a ticket ID for the customer to reference.
            Do NOT create a ticket for simple questions that you can answer directly.
            """)
    public String createTicket(String customerId, String category, String description) {
        String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String summary = "Category: %s | Customer: %s | %s | Created: %s"
                .formatted(category, customerId, description, LocalDateTime.now());
        ticketStore.put(ticketId, summary);
        return "Ticket created: " + ticketId + ". Our team will contact you within 24 hours.";
    }

    @Tool(description = """
            Process a refund for a delivered order.
            Requires the order ID. Only applicable when order status is DELIVERED.
            Returns confirmation of refund initiation or an error if ineligible.
            Always confirm the customer's intent before calling this tool.
            """)
    public String processRefund(String orderId) {
        var order = orderStore.get(orderId.toUpperCase());
        if (order == null) return "Order " + orderId + " not found.";
        if (!"DELIVERED".equals(order.status()))
            return "Refund not applicable — order is in status: " + order.status();
        return "Refund initiated for order " + orderId + ". Expect 3–5 business days.";
    }

    public record OrderInfo(String orderId, String product, String status, String date) {}
}
