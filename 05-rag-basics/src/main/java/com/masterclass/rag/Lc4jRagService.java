package com.masterclass.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LangChain4j RAG variant — demonstrating the same retrieval-augmented generation
 * pattern using LangChain4j's composable RAG pipeline.
 *
 * <h2>Spring AI vs LangChain4j RAG</h2>
 * <table>
 *   <tr><th>Feature</th><th>Spring AI</th><th>LangChain4j</th></tr>
 *   <tr><td>Retrieval wiring</td><td>QuestionAnswerAdvisor (Advisor pattern)</td><td>RetrievalAugmentor (explicit pipeline)</td></tr>
 *   <tr><td>Embedding store</td><td>VectorStore abstraction</td><td>EmbeddingStore abstraction</td></tr>
 *   <tr><td>Content retriever</td><td>Implicit via advisor</td><td>Explicit EmbeddingStoreContentRetriever</td></tr>
 *   <tr><td>Query routing</td><td>Manual (multiple VectorStore calls)</td><td>Built-in QueryRouter</td></tr>
 *   <tr><td>Re-ranking</td><td>Manual (as shown in HybridSearchService)</td><td>Built-in ContentAggregator + reranker</td></tr>
 * </table>
 *
 * <h2>LangChain4j RAG pipeline stages</h2>
 * <pre>
 *   User Query
 *     → QueryTransformer   (optional: expand/decompose query)
 *     → QueryRouter        (optional: route to different retrievers)
 *     → ContentRetriever   (embed query + similarity search)
 *     → ContentAggregator  (optional: merge results from multiple retrievers)
 *     → ContentInjector    (inject retrieved text into the prompt)
 *     → LLM               (generate answer)
 * </pre>
 *
 * This implementation uses the default pipeline (no router or reranker) to show
 * the baseline. The LangChain4j approach gives more explicit control over each stage.
 *
 * Note: only active when {@code app.rag.lc4j.enabled=true} to avoid bean conflicts.
 */
@Service
@ConditionalOnProperty(name = "app.rag.lc4j.enabled", havingValue = "true")
public class Lc4jRagService {

    private static final Logger log = LoggerFactory.getLogger(Lc4jRagService.class);

    private final RagAssistant assistant;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    /**
     * Typed AiService interface — LangChain4j generates the implementation at runtime.
     * The {@code @UserMessage} annotation defines the prompt template.
     * The RetrievalAugmentor wired in the config injects context automatically.
     */
    interface RagAssistant {
        String answer(String question);
    }

    public Lc4jRagService(ChatLanguageModel chatModel,
                          EmbeddingStore<TextSegment> embeddingStore,
                          EmbeddingModel embeddingModel,
                          RagProperties props) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;

        // Build the retrieval augmentor with explicit configuration
        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(props.topK())
                .minScore(props.similarityThreshold())
                .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        // AiServices wires the augmentor into the chat pipeline — no manual context injection needed
        this.assistant = AiServices.builder(RagAssistant.class)
                .chatLanguageModel(chatModel)
                .retrievalAugmentor(augmentor)
                .build();
    }

    public String ask(String question) {
        log.debug("LangChain4j RAG query: {}", question);
        return assistant.answer(question);
    }

    /**
     * Manual embedding + storage for new documents.
     * LangChain4j requires explicit embedding before storage (unlike Spring AI's VectorStore
     * which embeds automatically inside add()).
     */
    public void ingestText(String text, String source) {
        TextSegment segment = TextSegment.from(text, dev.langchain4j.data.document.Metadata.from("source", source));
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
        log.info("LangChain4j: ingested 1 segment from source '{}'", source);
    }

    /**
     * Demonstrates custom prompt template composition — useful when you want
     * to control exactly how context and question are formatted for the LLM.
     */
    public String askWithCustomPrompt(String question, String systemContext) {
        PromptTemplate template = PromptTemplate.from(
                "System context: {{systemContext}}\n\nQuestion: {{question}}\n\nAnswer based on retrieved context only:"
        );
        Prompt prompt = template.apply(Map.of("systemContext", systemContext, "question", question));

        // Ask via the typed assistant (retrieval augmentor still injects context)
        return assistant.answer(prompt.text());
    }
}
