# Cross-Cutting Concerns — Introduction Order

This table tells Claude which concerns are already active from module 01 (wire from `shared/` without explanation) versus which modules introduce and explain them in depth.

| Concern | Active from module | Deep-dive module | Implementation location |
|---|---|---|---|
| JWT auth (HS256, stateless) | 01 | 07 | `shared/security/JwtAuthFilter` |
| Per-user API keys | 07 | 07 | `shared/security/ApiKeyAuthFilter` |
| Bucket4j rate limit (local, per-user) | 01 | 07 | `shared/ratelimit/Bucket4jConfig` |
| Bucket4j rate limit (Redis-backed, multi-instance) | 07 | 13 | same class, `bucket4j-redis` backend |
| OpenAPI / springdoc UI | 01 | 07 | `shared/config/OpenApiConfig` |
| Spring Actuator health + info | 01 | 08 | auto-configured |
| OpenTelemetry tracing (auto-instrumented) | 01 | 08 | `shared/observability/OtelConfig` |
| Micrometer metrics + Prometheus scrape | 01 | 08 | `shared/observability/TokenUsageMetrics` |
| Cost tracking (token count recording) | 04 | 07 | `shared/cost/CostTracker` |
| Basic input length + content validation | 02 | 09 | `shared/guardrails/InputValidator` |
| Prompt injection detection | 04 | 09 | `shared/guardrails/PromptInjectionDetector` |
| PII redaction on output | 09 | 09 | `shared/guardrails/PiiRedactor` |
| Retry + exponential backoff on LLM calls | 04 | 13 | Resilience4j `@Retry` on agent service |
| Circuit breaker on external tool calls | 04 | 13 | Resilience4j `@CircuitBreaker` on tool methods |
| Docker Compose local infra | 01 | 13 | root `docker-compose.yml` |
| Session ownership check (user isolation) | 06 | 06 | `ConversationController` |
| Admin-only endpoint scope | 05 | 07 | Spring Security `hasRole("ADMIN")` |
| Kubernetes / Helm deployment | — | 13 | `13-deployment/k8s/`, `13-deployment/helm/` |

## Rule for Claude
- When creating modules 01–03: wire `JwtAuthFilter`, `Bucket4jConfig`, and `OpenApiConfig` from `shared/` silently (no in-depth explanation in those modules).
- When creating module 07: explain every concern in the table; this is the dedicated deep-dive.
- When a concern is listed as "active from module X" do NOT build it from scratch again in later modules — reference and reuse `shared/`.
- When creating modules 08–13: extend `shared/` implementations; never duplicate.
