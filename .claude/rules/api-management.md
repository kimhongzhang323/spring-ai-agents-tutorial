# API Management Rules (Core Differentiator)

Every agent-backed REST endpoint MUST include all of the following — no exceptions without explicit sign-off.

## Authentication
- JWT bearer token validation via Spring Security filter chain
- Per-user API key support (stored hashed in DB, validated on each request)
- Separate rate limit buckets per user identity

## Rate Limiting (Bucket4j)
- Per-user token bucket: configurable RPS + burst
- Global LLM quota bucket: prevents runaway costs
- Return `429 Too Many Requests` with `Retry-After` header
- Expose current quota usage in response headers (`X-RateLimit-Remaining`)

## Observability
- OpenTelemetry trace per request — propagate trace ID into LLM calls
- Micrometer metrics: request count, latency histogram, token usage counters
- Spring Actuator `/actuator/health` and `/actuator/metrics` always enabled

## Input / Output
- Input guardrails: length limits, content moderation check before sending to LLM
- Structured output: use Spring AI's structured output converters; document schema in OpenAPI
- Full OpenAPI 3.1 docs on every endpoint (`springdoc-openapi`)

## Cost Tracking
- Record prompt + completion token counts per request
- Associate cost to user ID for billing/quota dashboards
- Log model used and token counts at DEBUG level

## Versioning
- URL versioning: `/api/v1/agents/...`
- Never break a versioned endpoint — add `/v2/` instead
