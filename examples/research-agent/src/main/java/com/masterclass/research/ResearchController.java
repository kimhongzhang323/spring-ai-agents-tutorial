package com.masterclass.research;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/research")
@Tag(name = "Research Agent", description = "LangChain4j agentic research with web search + critic loop")
@SecurityRequirement(name = "bearerAuth")
public class ResearchController {

    private final ResearchAgentDef agent;

    public ResearchController(ResearchAgentDef agent) {
        this.agent = agent;
    }

    @PostMapping("/report")
    @Operation(summary = "Generate a cited research report on any topic",
               description = "Uses web search tool (stubbed) + LangChain4j AiServices. Expect 2–4 LLM calls.")
    public ResponseEntity<String> report(@RequestBody AgentRequest request) {
        String report = agent.research(request.message());
        return ResponseEntity.ok(report);
    }
}
