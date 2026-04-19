package com.masterclass.providers.tuning;

import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.stereotype.Service;

/**
 * Demonstrates how to control LLM response behavior at runtime using
 * per-request ChatOptions.
 *
 * Spring AI lets you override model options on every call without changing the
 * bean configuration. This is the right pattern for exposing tuning controls
 * to API consumers.
 *
 * Key parameters:
 *   temperature  — randomness/creativity (0.0 = deterministic, 1.0+ = creative)
 *   maxTokens    — hard cap on response length (controls cost and latency)
 *   topP         — nucleus sampling threshold (alternative to temperature)
 *   topK         — limits vocabulary to K most likely tokens at each step
 *   stopSequences — force the model to stop generating at a specific string
 */
@Service
public class ResponseTuningService {

    private final ProviderRouter router;

    public ResponseTuningService(ProviderRouter router) {
        this.router = router;
    }

    /**
     * Chat with full control over every response parameter.
     *
     * @param prompt        user message
     * @param provider      explicit provider name (or null for BALANCED)
     * @param config        all tunable response parameters
     */
    public TuningResult chat(String prompt, String provider, ResponseConfig config) {
        ChatClient client = router.select(
                provider != null ? RoutingStrategy.EXPLICIT : RoutingStrategy.BALANCED,
                provider);

        String providerUsed = router.selectProviderName(
                provider != null ? RoutingStrategy.EXPLICIT : RoutingStrategy.BALANCED,
                provider);

        // Build runtime options — these override the bean-level defaults for this call only.
        var optionsBuilder = ChatOptionsBuilder.builder();
        if (config.temperature() != null)  optionsBuilder.temperature(config.temperature());
        if (config.maxTokens() != null)    optionsBuilder.maxTokens(config.maxTokens());
        if (config.topP() != null)         optionsBuilder.topP(config.topP());
        if (config.topK() != null)         optionsBuilder.topK(config.topK());
        if (config.stopSequences() != null && !config.stopSequences().isEmpty()) {
            optionsBuilder.stopSequences(config.stopSequences());
        }

        long start = System.currentTimeMillis();
        String response = client.prompt()
                .user(prompt)
                .options(optionsBuilder.build())
                .call()
                .content();
        long latencyMs = System.currentTimeMillis() - start;

        return new TuningResult(response, providerUsed, config, latencyMs);
    }

    /**
     * Run the same prompt at three different temperature values so the caller
     * can compare how creativity affects output.
     */
    public TemperatureComparisonResult compareTemperatures(String prompt, String provider) {
        String[] temperatures = {"0.0", "0.5", "1.0"};
        var results = new java.util.LinkedHashMap<String, String>();

        for (String temp : temperatures) {
            double t = Double.parseDouble(temp);
            ChatClient client = router.select(
                    provider != null ? RoutingStrategy.EXPLICIT : RoutingStrategy.BALANCED,
                    provider);
            String response = client.prompt()
                    .user(prompt)
                    .options(ChatOptionsBuilder.builder().temperature(t).build())
                    .call()
                    .content();
            results.put("temperature_" + temp.replace(".", "_"), response);
        }

        String providerUsed = router.selectProviderName(
                provider != null ? RoutingStrategy.EXPLICIT : RoutingStrategy.BALANCED,
                provider);
        return new TemperatureComparisonResult(prompt, providerUsed, results);
    }

    /**
     * Demonstrate stop sequences: generate text but halt before a specific token.
     *
     * Practical uses: stop before "###" section markers, stop after first sentence,
     * or stop at a JSON closing brace for partial structured extraction.
     */
    public TuningResult chatWithStop(String prompt, String stopAt, String provider) {
        var config = new ResponseConfig(0.7, 500, null, null,
                stopAt != null ? java.util.List.of(stopAt) : null, null);
        return chat(prompt, provider, config);
    }
}
