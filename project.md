# Paper Recommendation System

## Stack
- Java / Kotlin + Spring Boot
- PostgreSQL (metadata, interactions, citations graph)
- Redis (cache, queues)
- No Python

## Algorithms to implement manually
- HNSW (vector similarity search)
- PageRank / Personalized PageRank (citation graph)
- SVD / ALS (collaborative filtering)
- BPR (ranking)
- TF-IDF + BM25 (text retrieval)
- LDA / BERTopic (topic modeling)
- Fusion + MMR reranking

## Data sources (APIs)
- Semantic Scholar API (papers, citations, SPECTER2 embeddings)
- OpenAlex API (topics hierarchy, metadata enrichment)
- arXiv API (preprints)

## HNSW — key idea
Multi-layer graph. Search starts at top layer (few nodes, long edges),
greedily moves toward query, descends when stuck (all neighbors are
farther than current node). Layer switch = local minimum reached.

## DB schema (key tables)
- papers (id, s2_id, title, abstract, year, topics[])
- paper_embeddings (paper_id, vector float[768])
- interactions (user_id, paper_id, action, weight, ts)
- citations (from_id, to_id)