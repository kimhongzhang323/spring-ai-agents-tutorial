package com.masterclass.tools;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "Tool-Calling Agent", description = "Module 04 – ReAct pattern with weather, stocks, currency, and calculator tools")
@SecurityRequirement(name = "bearerAuth")
public class ToolAgentController {

    private final ToolAgentService toolAgentService;

    public ToolAgentController(ToolAgentService toolAgentService) {
        this.toolAgentService = toolAgentService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Chat with the tool-calling agent",
            description = """
                    The agent has access to four tools and will invoke them as needed:
                    - **weather**: current conditions for a city
                    - **calculator**: arithmetic operations
                    - **stockPrice**: current price for major US tickers
                    - **currency**: exchange rate conversion

                    Try: "What's the weather in Tokyo and how much is 500 USD in JPY?"
                    The agent will call multiple tools in sequence to answer compound questions.
                    """
    )
    public ResponseEntity<AgentResponse> chat(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(AgentResponse.of(toolAgentService.chat(request.message())));
    }
}
