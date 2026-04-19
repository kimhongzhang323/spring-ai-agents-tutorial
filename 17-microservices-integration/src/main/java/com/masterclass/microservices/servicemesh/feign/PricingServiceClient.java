package com.masterclass.microservices.servicemesh.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pricing-service", url = "${feign.pricing-service.url:http://localhost:8081}")
public interface PricingServiceClient {

    @GetMapping("/api/v1/prices/{productId}")
    String getPrice(@PathVariable String productId);
}
