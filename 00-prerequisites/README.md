# Module 00 — Prerequisites

Before running any module, verify your environment meets these requirements.

## Requirements

| Tool | Minimum version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9 | `./mvnw -version` |
| Docker | 24 | `docker --version` |
| Docker Compose | v2 | `docker compose version` |
| Ollama | latest | `ollama --version` |

## Environment Setup

### 1. Clone and verify Maven wrapper

```bash
git clone https://github.com/kimhongzhang323/java-ai-agents-masterclass.git
cd java-ai-agents-masterclass
./mvnw -version
```

### 2. Copy environment variables

```bash
cp .env.example .env
# Edit .env — set JWT_SECRET to a 32+ character random string
# For cloud profile: set OPENAI_API_KEY
```

### 3. Start infrastructure

```bash
docker compose up -d
docker compose ps   # all services should be healthy within 60 seconds
```

### 4. Pull Ollama models

```bash
# LLM used by modules 01–10
docker exec masterclass-ollama ollama pull llama3.1

# Embedding model used by module 05 (RAG)
docker exec masterclass-ollama ollama pull nomic-embed-text
```

### 5. Verify everything

```bash
# Ollama
curl http://localhost:11434/api/tags

# PGVector (PostgreSQL)
docker exec masterclass-pgvector pg_isready -U masterclass

# Redis
docker exec masterclass-redis redis-cli -a masterclass ping

# Jaeger UI
curl -s http://localhost:16686 | grep -q Jaeger && echo "Jaeger OK"
```

## Common Problems

**Docker Desktop runs out of memory**  
Increase Docker memory allocation to at least 8 GB (Settings → Resources).

**Ollama GPU not detected**  
Modules work on CPU — just slower. If you have an NVIDIA GPU, install the [CUDA runtime](https://developer.nvidia.com/cuda-downloads) and restart Ollama.

**Port conflict**  
If 5432, 6379, or 11434 are already in use locally, edit `docker-compose.yml` and remap the host ports.

## What's Next

[Module 01 — Hello Agent](../01-hello-agent/README.md): your first secured LLM endpoint.