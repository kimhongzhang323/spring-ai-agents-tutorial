# Coding Standards

## Java Style
- Java 21+, Spring Boot conventions throughout
- Constructor injection always — no `@Autowired` on fields
- Use Java records for DTOs and value objects
- Minimize Lombok; prefer records and explicit constructors
- Meaningful names — no single-letter variables except loop counters

## Spring AI Specifics
- Prefer `ChatClient` (fluent builder API) over raw `ChatModel`
- Every `@Tool` method must have a detailed `description` written for the LLM, not for humans
- Advisors for cross-cutting concerns (memory, RAG, logging) — do not inline these in service classes
- Configure models via `application.yml` under `spring.ai.*`; never hardcode model names in Java

## Documentation
- Javadoc only on public API surface and complex agent orchestration logic
- No obvious comments — name the code well instead
- Module READMEs are the primary teaching artifact; keep them accurate

## Logging & Observability
- Structured JSON logging in production profiles
- Never log raw user prompts or LLM responses at INFO level (privacy + cost)
- Every agent invocation must emit an OpenTelemetry span with token usage attributes

## Testing
- Unit tests: mock the LLM with WireMock or a stubbed `ChatModel`
- Integration tests: `@SpringBootTest` + Testcontainers for real infra (PGVector, Redis)
- At minimum: one happy-path and one error-path test per agent endpoint