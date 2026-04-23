package com.masterclass.parallelteam;

import com.masterclass.parallelteam.model.TeamJob;
import com.masterclass.parallelteam.model.TeamRequest;
import com.masterclass.parallelteam.model.TeamResponse;
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
 * Integration tests for module 19 – Parallel Agent Teams.
 *
 * Requires: Ollama running on localhost:11434 with llama3.2 model.
 * Start infra: docker compose up -d (from module root)
 *
 * Run with: ./mvnw test -Pci -Dtest=ParallelTeamIT -pl 19-parallel-agent-teams
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Disabled("Requires live Ollama — run after docker compose up -d")
class ParallelTeamIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String base() {
        return "http://localhost:" + port + "/api/v1/parallel-team";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth("test-jwt-placeholder");
        return h;
    }

    @Test
    void runJob_returnsAcceptedWithJobId() {
        ResponseEntity<TeamResponse> resp = rest.postForEntity(
                base() + "/run",
                new HttpEntity<>(new TeamRequest("Impact of AI on software engineering"), authHeaders()),
                TeamResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().jobId()).isNotBlank();
        assertThat(resp.getBody().status()).isEqualTo(TeamJob.JobStatus.RUNNING);
    }

    @Test
    void pollStatus_eventuallyCompletes() throws InterruptedException {
        ResponseEntity<TeamResponse> submitted = rest.postForEntity(
                base() + "/run",
                new HttpEntity<>(new TeamRequest("Quantum computing trends 2025"), authHeaders()),
                TeamResponse.class);

        String jobId = submitted.getBody().jobId();
        long deadline = System.currentTimeMillis() + 120_000; // 2 min max for 4 LLM calls

        TeamResponse last = submitted.getBody();
        while (System.currentTimeMillis() < deadline && last.status() == TeamJob.JobStatus.RUNNING) {
            Thread.sleep(2_000);
            ResponseEntity<TeamResponse> poll = rest.exchange(
                    base() + "/" + jobId,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    TeamResponse.class);
            last = poll.getBody();
        }

        assertThat(last.status()).isEqualTo(TeamJob.JobStatus.COMPLETED);
        assertThat(last.finalReport()).isNotBlank();
        assertThat(last.completedAt()).isNotNull();
    }

    @Test
    void unknownJobId_returns404() {
        ResponseEntity<String> resp = rest.exchange(
                base() + "/00000000-0000-0000-0000-000000000000",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shortTopic_returnsBadRequest() {
        ResponseEntity<String> resp = rest.postForEntity(
                base() + "/run",
                new HttpEntity<>(new TeamRequest("AI"), authHeaders()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
