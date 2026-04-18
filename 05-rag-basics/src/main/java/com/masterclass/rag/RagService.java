package com.masterclass.rag;

import com.masterclass.shared.guardrails.InputValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based strictly on the provided context.
            If the answer cannot be found in the context, say "I don't have enough information to answer that."
            Do not make up information. Always cite the source document when possible.
            """;

    private final ChatClient chatClient;
    private final InputValidator inputValidator;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore,
                      RagProperties props, InputValidator inputValidator) {
        this.inputValidator = inputValidator;
        /*
         * QuestionAnswerAdvisor intercepts the user message before it reaches the LLM:
         * 1. Embeds the user question using the same embedding model.
         * 2. Searches PGVector for the top-K most similar chunks.
         * 3. Appends retrieved chunks to the prompt as context.
         * 4. Forwards the augmented prompt to the LLM.
         */
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder()
                                        .topK(props.topK())
                                        .similarityThreshold(props.similarityThreshold())
                                        .build()))
                .build();
    }

    public String ask(String question) {
        var validation = inputValidator.validate(question);
        if (!validation.valid()) throw new IllegalArgumentException(validation.reason());

        return chatClient.prompt().user(question).call().content();
    }
}
