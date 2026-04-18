package com.masterclass.providers.streaming;

import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Streams LLM responses as a Flux<String> using Spring AI's streaming API.
 *
 * Streaming is critical for long responses: it lets the browser start rendering
 * immediately rather than waiting for the entire response to complete.
 *
 * The downstream controller exposes this as an SSE endpoint so the browser
 * receives a continuous stream of text tokens.
 */
@Service
public class StreamingService {

    private final ProviderRouter router;

    public StreamingService(ProviderRouter router) {
        this.router = router;
    }

    /**
     * Returns a cold Flux that emits response text chunks as they arrive.
     *
     * Spring AI's .stream().content() returns Flux<String> where each element
     * is a partial response token. Subscribe to consume the stream.
     */
    public Flux<String> stream(String prompt, RoutingStrategy strategy, String explicitProvider) {
        return router.select(strategy, explicitProvider)
                .prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
