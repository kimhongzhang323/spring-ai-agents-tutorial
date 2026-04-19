package com.masterclass.microservices.servicemesh.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ExternalServiceTool {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceTool.class);

    private final PricingServiceClient pricingClient;
    private final UserProfileClient userProfileClient;

    public ExternalServiceTool(PricingServiceClient pricingClient,
                                UserProfileClient userProfileClient) {
        this.pricingClient = pricingClient;
        this.userProfileClient = userProfileClient;
    }

    @Tool(description = """
            Fetches the current price of a product from the internal Pricing microservice via OpenFeign HTTP client.
            OpenFeign generates type-safe HTTP clients from interfaces, making REST calls to
            internal microservices look like local method calls. Use this when the agent needs
            to quote a price to a customer or compare pricing across products.
            Input: productId (e.g. 'SKU-12345').
            Returns: current price and currency as JSON.
            """)
    public String getProductPrice(String productId) {
        try {
            String price = pricingClient.getPrice(productId);
            log.debug("Feign pricing call: product={}", productId);
            return price;
        } catch (Exception e) {
            log.error("Feign pricing call failed: product={}", productId, e);
            return "Pricing service unavailable: " + e.getMessage();
        }
    }

    @Tool(description = """
            Retrieves a user's profile from the User microservice via OpenFeign HTTP client.
            Use this when the agent needs to personalize its response based on user preferences,
            loyalty tier, purchase history, or account details.
            Input: userId (the user's unique identifier).
            Returns: user profile JSON with name, tier, and preferences.
            """)
    public String getUserProfile(String userId) {
        try {
            String profile = userProfileClient.getProfile(userId);
            log.debug("Feign user profile call: userId={}", userId);
            return profile;
        } catch (Exception e) {
            log.error("Feign user profile call failed: userId={}", userId, e);
            return "User service unavailable: " + e.getMessage();
        }
    }
}
