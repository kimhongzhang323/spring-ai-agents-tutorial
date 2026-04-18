package com.masterclass.eval;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eval")
@Tag(name = "Evaluation", description = "Run LLM-as-judge evaluations against golden datasets")
@SecurityRequirement(name = "bearerAuth")
public class EvalController {

    private final EvalRunner runner;
    private final ChatClient chatClient;

    @Value("${eval.score-threshold:0.7}")
    private double threshold;

    public EvalController(EvalRunner runner, ChatClient.Builder builder) {
        this.runner     = runner;
        this.chatClient = builder.build();
    }

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run eval harness against the fixture golden dataset (admin only)")
    public ResponseEntity<EvalReport> runEval() throws Exception {
        // The agent under test: a simple ChatClient call simulating the module under evaluation
        EvalRunner.AgentUnderTest agent = question -> chatClient.prompt()
                .user(question)
                .call()
                .content();

        EvalReport report = runner.run(
                new ClassPathResource("eval/golden-dataset.yml"),
                agent,
                threshold);

        return ResponseEntity.ok(report);
    }
}
