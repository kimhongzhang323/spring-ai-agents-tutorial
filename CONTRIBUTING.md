# Contributing

## Adding a New Module

Every module must pass this checklist **before a PR is opened**. Reviewers will close PRs that skip items.

### Checklist

- [ ] Folder named `0X-kebab-name/` (two-digit prefix).
- [ ] `README.md` has all 9 sections from `.claude/rules/module-template.md`.
- [ ] `pom.xml` inherits from `com.masterclass:java-ai-agents-masterclass`.
- [ ] `shared` is declared as a dependency — JWT auth, rate limiting, and OpenAPI are wired in.
- [ ] Controller returns `401` for missing JWT and `429` with `Retry-After` for over-quota (verified by tests).
- [ ] At least one unit test (mocked LLM) and one test verifying auth rejection.
- [ ] Integration test skeleton in `*IT.java` (can be empty `@Disabled` but must exist).
- [ ] Mermaid diagram in README (sequence or flowchart matching the module's agent flow).
- [ ] `docker-compose.yml` added if the module needs infra beyond the root compose.
- [ ] `.env.example` at repo root updated if any new environment variables are introduced.

### PR Template

PRs that add a module should use the module PR template (`.github/PULL_REQUEST_TEMPLATE.md`).  
For bug fixes or docs-only changes, a short description is sufficient.

### Code Style

- Java 21+, constructor injection, records for DTOs.
- No `@Autowired` on fields.
- No Lombok (except `@Slf4j` if preferred over `private static final Logger log = ...`).
- `@Tool` descriptions must be written for the LLM, not for developers.
- Do not log raw user messages at INFO level.

### Running Tests Locally

```bash
# Unit tests only (fast)
./mvnw test

# Unit + integration tests (requires Docker)
./mvnw verify -Pci

# Single module
./mvnw -pl 01-hello-agent test
```
