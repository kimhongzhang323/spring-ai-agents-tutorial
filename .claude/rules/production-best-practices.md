# Production Best Practices

## Local Infrastructure
- Every module requiring external services ships a `docker-compose.yml`
- Standard services: Ollama (local LLM), PGVector (vector store), Redis (rate limit state + cache), Prometheus + Grafana (monitoring)
- Include `.env.example` — never commit real keys

## Resilience
- Retries with exponential backoff on LLM calls (Spring Retry or Resilience4j)
- Fallback model strategy: if primary model fails, degrade to a cheaper/local model
- Circuit breaker on all external tool calls

## Security
- No secrets in code or `application.yml` — use environment variables or Spring Cloud Config
- Dependency vulnerability scan in CI (`mvn dependency-check:check`)
- OWASP Top 10 awareness: validate all user input before it reaches prompt templates

## Cost & Model Awareness
- Document token usage estimates and model choice trade-offs in each module README
- Prefer streaming responses (`Flux<String>`) for long-running agent responses
- Cache embeddings where the corpus is stable (avoid re-embedding on every restart)

## CI/CD
- GitHub Actions pipeline: build → test → security scan → Docker image push
- Use Maven profiles: `local` (Ollama), `cloud` (OpenAI/Anthropic/Gemini)
- Integration tests run only in `ci` profile with Testcontainers
