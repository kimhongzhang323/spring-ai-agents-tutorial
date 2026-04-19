package com.masterclass.microservices.servicemesh.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class GrpcInventoryTool {

    private static final Logger log = LoggerFactory.getLogger(GrpcInventoryTool.class);

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    @Tool(description = """
            Checks product stock availability by calling the internal Inventory microservice via gRPC.
            gRPC uses Protocol Buffers (binary serialization) over HTTP/2 — it is 5-10x faster
            than REST for internal service-to-service calls and provides strong type contracts.
            Use this when the agent needs to verify whether a product can be fulfilled before
            making a recommendation or placing an order.
            Input: productId (e.g. 'SKU-12345'), quantity (how many units needed).
            Returns: available stock count and whether the item is in stock.
            """)
    public String checkInventoryStock(String productId, int quantity) {
        try {
            StockRequest request = StockRequest.newBuilder()
                    .setProductId(productId)
                    .setQuantity(quantity)
                    .build();
            StockResponse response = inventoryStub.checkStock(request);
            log.debug("gRPC inventory check: product={} available={}", productId, response.getAvailable());
            return "{\"productId\":\"%s\",\"available\":%d,\"inStock\":%b}".formatted(
                    response.getProductId(), response.getAvailable(), response.getInStock());
        } catch (Exception e) {
            log.error("gRPC call failed: product={}", productId, e);
            return "gRPC inventory check failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Reserves a quantity of a product in the Inventory microservice via gRPC.
            Use this when the agent has confirmed stock availability and needs to atomically
            reserve units before completing an order — prevents overselling in concurrent requests.
            Input: productId, quantity, reservationId (unique ID for this reservation).
            Returns: success status and reservation confirmation.
            """)
    public String reserveInventory(String productId, int quantity, String reservationId) {
        try {
            ReserveRequest request = ReserveRequest.newBuilder()
                    .setProductId(productId)
                    .setQuantity(quantity)
                    .setReservationId(reservationId)
                    .build();
            ReserveResponse response = inventoryStub.reserveItem(request);
            log.debug("gRPC reserve: product={} success={}", productId, response.getSuccess());
            return "{\"reservationId\":\"%s\",\"success\":%b,\"message\":\"%s\"}".formatted(
                    response.getReservationId(), response.getSuccess(), response.getMessage());
        } catch (Exception e) {
            log.error("gRPC reserve failed: product={}", productId, e);
            return "gRPC reservation failed: " + e.getMessage();
        }
    }
}
