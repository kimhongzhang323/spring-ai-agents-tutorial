# Architecture Rules

## Layer Order
Controller → Service → Agent → Tool

Never skip layers. Controllers handle HTTP concerns only; Agents orchestrate LLM calls and tool use; Services contain business logic; Tools are atomic, side-effecting operations.

## Module Structure
Every module (`0X-name/`) must contain:
- `README.md` — objectives, Mermaid diagram, run instructions, common pitfalls
- Its own `pom.xml` inheriting from the parent
- `src/main/java/` and `src/test/java/`
- `docker-compose.yml` if it needs local infra

## Multi-Agent Pattern
Use the **supervisor pattern**: one orchestrator agent delegates to specialized sub-agents. Sub-agents must not call each other directly — all routing goes through the supervisor.

## Shared Code
Cross-cutting utilities (JWT, rate limit config, tracing helpers, common DTOs) live in `shared/` and are declared as a Maven dependency in each module.

## Diagrams
Every agent flow and non-trivial architecture must have a Mermaid diagram in the module README. Sequence diagrams for agent→tool flows; flowcharts for routing logic.