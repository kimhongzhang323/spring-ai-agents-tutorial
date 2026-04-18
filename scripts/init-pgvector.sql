CREATE EXTENSION IF NOT EXISTS vector;

-- Table used by Spring AI PgVectorStore (modules 05+)
CREATE TABLE IF NOT EXISTS vector_store (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content     TEXT,
    metadata    JSON,
    embedding   vector(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
