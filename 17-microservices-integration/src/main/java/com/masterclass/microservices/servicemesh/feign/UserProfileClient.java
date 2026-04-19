package com.masterclass.microservices.servicemesh.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.user-service.url:http://localhost:8082}")
public interface UserProfileClient {

    @GetMapping("/api/v1/users/{userId}")
    String getProfile(@PathVariable String userId);
}
