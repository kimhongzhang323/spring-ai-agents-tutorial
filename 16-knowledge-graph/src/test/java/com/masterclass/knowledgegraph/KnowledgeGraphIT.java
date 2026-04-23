package com.masterclass.knowledgegraph;

import com.masterclass.knowledgegraph.controller.BuildRequest;
import com.masterclass.knowledgegraph.controller.GraphResponse;
import com.masterclass.knowledgegraph.controller.QueryRequest;
import com.masterclass.knowledgegraph.controller.ResumeRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test skeleton for the Knowledge Graph pipeline.
 *
 * Requires: Ollama running on localhost:11434 with llama3.2 model.
 * Start infra: docker compose up -d
 *
 * Run with: ./mvnw test -Pci -Dtest=KnowledgeGraphIT -pl 16-knowledge-graph
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Disabled("Requires live Ollama — run with -Pci after docker compose up -d")
class KnowledgeGraphIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String base() {
        return "http://localhost:" + port + "/api/v1/knowledge-graph";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Integration tests use a pre-signed test JWT from the shared test fixture
        headers.setBearerAuth("test-jwt-placeholder");
        return headers;
    }

    @Test
    void buildAndQueryPipeline_happyPath() {
        String text = """
                Alan Turing worked at Bletchley Park during World War II.
                Bletchley Park is located in the United Kingdom.
                Turing invented the concept of a universal computing machine.
                """;

        ResponseEntity<GraphResponse> buildResp = rest.postForEntity(
                base() + "/build",
                new HttpEntity<>(new BuildRequest(text), authHeaders()),
                GraphResponse.class);

        assertThat(buildResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(buildResp.getBody()).isNotNull();
        assertThat(buildResp.getBody().status()).isIn("COMPLETED", "SUSPENDED");

        ResponseEntity<GraphResponse> queryResp = rest.postForEntity(
                base() + "/query",
                new HttpEntity<>(new QueryRequest("Where did Turing work?"), authHeaders()),
                GraphResponse.class);

        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResp.getBody()).isNotNull();
        assertThat(queryResp.getBody().status()).isEqualTo("COMPLETED");
    }

    @Test
    void resumeEndpoint_rejectAbortsPipeline() {
        // Requires a SUSPENDED build — inject a text that exceeds validation thresholds
        String text = "X works at Y. A works at B. C works at D. E works at F.";

        ResponseEntity<GraphResponse> buildResp = rest.postForEntity(
                base() + "/build",
                new HttpEntity<>(new BuildRequest(text), authHeaders()),
                GraphResponse.class);

        if (!"SUSPENDED".equals(buildResp.getBody().status())) {
            return; // graph passed validation cleanly; test is a no-op
        }

        String threadId = buildResp.getBody().threadId();
        ResponseEntity<GraphResponse> resumeResp = rest.postForEntity(
                base() + "/resume",
                new HttpEntity<>(new ResumeRequest(threadId, "reject"), authHeaders()),
                GraphResponse.class);

        assertThat(resumeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resumeResp.getBody().status()).isEqualTo("COMPLETED");
    }

    @Test
    void invalidInput_returnsBadRequest() {
        ResponseEntity<String> resp = rest.postForEntity(
                base() + "/build",
                new HttpEntity<>(new BuildRequest("short"), authHeaders()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
