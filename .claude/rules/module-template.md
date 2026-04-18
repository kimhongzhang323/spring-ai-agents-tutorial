# Module Template Rules

## Folder Naming
`0X-kebab-name/` — two-digit prefix, lowercase kebab. No exceptions.

## Mandatory File Creation Order (per SKILL.md checklist)
1. `README.md`
2. `pom.xml` (parent = `com.masterclass:java-ai-agents-masterclass`)
3. `src/main/resources/application.yml` + `application-local.yml` + `application-cloud.yml`
4. `docker-compose.yml` (only if module needs infra beyond what root compose provides)
5. Domain records / entities
6. Tool classes (`@Tool` with LLM-readable `description`)
7. Agent service (`ChatClient`-based)
8. REST controller (always inherits API management stack from `shared/`)
9. Unit tests (mocked LLM / WireMock)
10. Integration test skeleton (Testcontainers, `@SpringBootTest`)

## README.md Template (exactly these 9 sections, in order)
1. **Learning Objectives** — 3–5 bullets
2. **Prerequisites** — link to previous module + env requirements
3. **Architecture** — Mermaid diagram (sequence or flowchart)
4. **Key Concepts** — 2–4 short explanatory paragraphs
5. **How to Run** — profile-aware commands (`-Plocal` / `-Pcloud`)
6. **Code Walkthrough** — file-by-file explanation
7. **Common Pitfalls** — bulleted list
8. **Further Reading** — links to Spring AI / LangChain4j docs, papers
9. **What's Next** — link to next module

## API Management Minimum (every controller, no exceptions)
All controllers must wire in from `shared/`:
- `JwtAuthFilter` via `SecurityConfig`
- `Bucket4jConfig` rate limit filter
- `springdoc-openapi` OpenAPI docs (endpoint appears in `/swagger-ui.html`)
- Return `401` for missing/expired JWT, `429` with `Retry-After` for over-quota

Phase 1–2 modules use the simple local Bucket4j configuration.
Phase 3+ modules wire Redis-backed Bucket4j for multi-instance correctness.

## LangChain4j Variant
If the module has a meaningful LangChain4j implementation, place it in:
`src/main/java/com/masterclass/<module>/langchain4j/`
Not in a separate subfolder — keep it in the same module, same Spring context.

## Package Convention
`com.masterclass.<moduleshortname>` — e.g., `com.masterclass.hello`, `com.masterclass.rag`, `com.masterclass.multiagent`.

## Test Naming
- Unit: `<ClassName>Test.java`
- Integration: `<ClassName>IT.java` (run only under `-Pci` profile via Failsafe)
