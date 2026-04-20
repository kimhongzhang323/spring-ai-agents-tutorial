package com.masterclass.microservices;

import com.masterclass.microservices.messaging.batch.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LlmBatchAccumulatorTest {

    private LlmBatchAccumulator accumulator;
    private LlmBatchProcessor processor;

    @BeforeEach
    void setUp() {
        // Stub the full ChatClient builder chain
        var builder = mock(ChatClient.Builder.class);
        var chatClient = mock(ChatClient.class);
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var callSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Mocked LLM response");

        processor = new LlmBatchProcessor(builder);
        accumulator = new LlmBatchAccumulator(processor);
        ReflectionTestUtils.setField(accumulator, "maxBatchSize", 5);
    }

    @Test
    void enqueuedJobStartsAsPending() {
        BatchRequest req = accumulator.enqueue("What is Java?");

        assertThat(req.jobId()).isNotBlank();
        assertThat(accumulator.getResult(req.jobId()).status())
                .isEqualTo(BatchJobStatus.PENDING);
    }

    @Test
    void drainSetsStatusToProcessingThenDone() throws InterruptedException {
        BatchRequest req = accumulator.enqueue("Explain Spring AI");

        accumulator.drainAndProcess();
        Thread.sleep(300); // allow virtual thread to complete

        BatchJobResult result = accumulator.getResult(req.jobId());
        assertThat(result.status()).isEqualTo(BatchJobStatus.DONE);
        assertThat(result.result()).isEqualTo("Mocked LLM response");
    }

    @Test
    void autoFlushesWhenBatchSizeReached() throws InterruptedException {
        // Enqueuing maxBatchSize items triggers immediate drain
        List<BatchRequest> requests = List.of(
                accumulator.enqueue("Q1"), accumulator.enqueue("Q2"),
                accumulator.enqueue("Q3"), accumulator.enqueue("Q4"),
                accumulator.enqueue("Q5")
        );

        Thread.sleep(500);

        for (BatchRequest r : requests) {
            assertThat(accumulator.getResult(r.jobId()).status())
                    .isIn(BatchJobStatus.DONE, BatchJobStatus.PROCESSING);
        }
    }

    @Test
    void unknownJobIdReturnsFailed() {
        BatchJobResult result = accumulator.getResult("non-existent-id");
        assertThat(result.status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(result.errorMessage()).contains("Unknown jobId");
    }
}
