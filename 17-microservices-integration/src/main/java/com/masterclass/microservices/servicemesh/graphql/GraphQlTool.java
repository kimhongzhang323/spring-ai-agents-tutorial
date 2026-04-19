package com.masterclass.microservices.servicemesh.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class GraphQlTool {

    private static final Logger log = LoggerFactory.getLogger(GraphQlTool.class);

    private final WebClient webClient;

    public GraphQlTool(WebClient.Builder webClientBuilder,
                       @org.springframework.beans.factory.annotation.Value(
                               "${graphql.service.url:http://localhost:8083/graphql}") String graphqlUrl) {
        this.webClient = webClientBuilder.baseUrl(graphqlUrl).build();
    }

    @Tool(description = """
            Executes a GraphQL query against an internal GraphQL API and returns the result.
            GraphQL allows the agent to request exactly the fields it needs — no over-fetching,
            no under-fetching. Use this when calling an internal service that exposes a
            GraphQL endpoint (e.g., product catalog, content API, user data service).
            Input: graphqlQuery (a valid GraphQL query string, e.g. '{ products { id name price } }').
            Returns: the GraphQL response data as JSON.
            """)
    public String executeGraphQlQuery(String graphqlQuery) {
        try {
            Map<String, Object> requestBody = Map.of("query", graphqlQuery);
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.debug("GraphQL query executed successfully");
            return response != null ? response : "{}";
        } catch (Exception e) {
            log.error("GraphQL query failed", e);
            return "GraphQL error: " + e.getMessage();
        }
    }
}
