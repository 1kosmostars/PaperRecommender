-- ============================================================
-- V1: Initial schema for PaperRecommender
-- ============================================================

-- Users
CREATE TABLE users (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Papers
CREATE TABLE papers (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    s2_id            VARCHAR(100) UNIQUE,
    arxiv_id         VARCHAR(50)  UNIQUE,
    doi              VARCHAR(200) UNIQUE,
    title            TEXT         NOT NULL,
    abstract_text    TEXT,
    year             INT,
    citation_count   INT          NOT NULL DEFAULT 0,
    reference_count  INT          NOT NULL DEFAULT 0,
    -- SPECTER2 768-dim vector stored as raw little-endian bytes (4 bytes × 768 = 3072 bytes)
    embedding        BYTEA,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_papers_s2_id    ON papers (s2_id);
CREATE INDEX idx_papers_arxiv_id ON papers (arxiv_id);
CREATE INDEX idx_papers_year     ON papers (year);

-- Paper topics (LDA / OpenAlex concepts)
CREATE TABLE paper_topics (
    paper_id UUID         NOT NULL REFERENCES papers (id) ON DELETE CASCADE,
    topic    VARCHAR(200) NOT NULL,
    PRIMARY KEY (paper_id, topic)
);

CREATE INDEX idx_paper_topics_paper ON paper_topics (paper_id);

-- Paper authors (ordered)
CREATE TABLE paper_authors (
    paper_id    UUID         NOT NULL REFERENCES papers (id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    author_name VARCHAR(300) NOT NULL,
    PRIMARY KEY (paper_id, position)
);

CREATE INDEX idx_paper_authors_paper ON paper_authors (paper_id);

-- Citation graph: citing_paper_id → cited_paper_id (out-edges for PageRank)
CREATE TABLE paper_citations (
    citing_paper_id UUID NOT NULL REFERENCES papers (id) ON DELETE CASCADE,
    cited_paper_id  UUID NOT NULL REFERENCES papers (id) ON DELETE CASCADE,
    PRIMARY KEY (citing_paper_id, cited_paper_id)
);

CREATE INDEX idx_citations_citing ON paper_citations (citing_paper_id);
CREATE INDEX idx_citations_cited  ON paper_citations (cited_paper_id);

-- Interactions (implicit feedback)
CREATE TABLE interactions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    paper_id    UUID        NOT NULL REFERENCES papers (id) ON DELETE CASCADE,
    action_type VARCHAR(20) NOT NULL
                    CHECK (action_type IN ('VIEW','CLICK','SAVE','DOWNLOAD','CITE')),
    weight      REAL        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interactions_user       ON interactions (user_id);
CREATE INDEX idx_interactions_paper      ON interactions (paper_id);
CREATE INDEX idx_interactions_user_paper ON interactions (user_id, paper_id);
