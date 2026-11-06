# PaperRecommender

A hybrid research-paper recommender built with **Java 17 + Spring Boot 3**. Every ranking
algorithm — HNSW, BPR, PageRank, BM25, Reciprocal Rank Fusion and MMR — is implemented
from scratch with no ML libraries. The compute-heavy ones (HNSW, BPR, PageRank) are written
in C11 and called through JNI.

Paper data (metadata, abstracts, 768-d SPECTER embeddings, citation graph) is pulled from
**Semantic Scholar**, **arXiv** and **OpenAlex** and stored in PostgreSQL.

---

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Algorithms and math](#algorithms-and-math)
  - [Notation](#notation)
  - [1. User profile with time decay](#1-user-profile-with-time-decay)
  - [2. HNSW — content-based retrieval](#2-hnsw--content-based-retrieval)
  - [3. BM25 — text retrieval](#3-bm25--text-retrieval)
  - [4. BPR — collaborative filtering](#4-bpr--collaborative-filtering)
  - [5. PageRank — citation authority](#5-pagerank--citation-authority)
  - [6. Reciprocal Rank Fusion](#6-reciprocal-rank-fusion)
  - [7. MMR — diversity re-ranking](#7-mmr--diversity-re-ranking)
  - [Complexity summary](#complexity-summary)
  - [Hyperparameters](#hyperparameters)
- [Tech stack](#tech-stack)
- [Project layout](#project-layout)
- [Getting started](#getting-started)
- [REST API](#rest-api)
- [Security notes](#security-notes)

---

## Features

- Personalized recommendations from each user's interaction history
- Full-text search over titles and abstracts
- Citation-graph authority signal
- Diversified results — near-duplicate papers are pushed apart
- Import by search query or by paper ID, with recursive loading of references
- Web UI (`/index.html`) and admin console (`/admin.html`)
- Per-paper explanations: which signals put a paper into the list

## Architecture

```
                ┌──────────────── user history ────────────────┐
                │                                               │
   profile vector  p_u                               implicit feedback D
   (time-decayed weighted mean                       (user, paper) pairs
    of paper embeddings)                                         │
                │                                                │
          ┌─────▼─────┐   ┌──────────┐   ┌──────────────┐   ┌────▼─────┐
          │   HNSW    │   │   BM25   │   │     BPR      │   │ PageRank │
          │  cosine   │   │  query q │   │  p_u · q_i   │   │  graph   │
          └─────┬─────┘   └────┬─────┘   └──────┬───────┘   └────┬─────┘
                │   top-5k     │   top-5k       │   top-5k       │ top-5k
                └──────────────┴────────┬───────┴────────────────┘
                              Reciprocal Rank Fusion (k = 60)
                                        │
                              MMR re-ranking, λ = 0.7, pool 2k
                                        │
                          filter already-seen papers → top-k
```

Each signal contributes only when it has data: HNSW needs a user with history, BPR needs
a trained model containing that user, BM25 needs a non-empty query; PageRank is always
available. An anonymous request with no query therefore degrades gracefully to
"most authoritative papers, diversified".

---

## Algorithms and math

### Notation

| Symbol | Meaning |
|---|---|
| $N$ | number of papers in the index |
| $\mathbf{e}_i \in \mathbb{R}^{768}$ | SPECTER embedding of paper $i$ |
| $u$, $i$, $j$ | user, positive paper, negative paper |
| $\mathcal{I}_u$ | interactions of user $u$ |
| $k$ | number of results requested ($1 \le k \le 100$) |
| $C = 5k$ | candidates requested from every signal |

Similarity between embeddings is always cosine:

```math
\operatorname{sim}(\mathbf{a},\mathbf{b}) = \frac{\mathbf{a}\cdot\mathbf{b}}{\lVert\mathbf{a}\rVert\,\lVert\mathbf{b}\rVert},
\qquad
d(\mathbf{a},\mathbf{b}) = 1 - \operatorname{sim}(\mathbf{a},\mathbf{b}) \in [0, 2]
```

A zero vector is defined to have $\operatorname{sim} = 0$ (distance $1$) to everything.

---

### 1. User profile with time decay

`IndexService.userProfileVector`

A user is represented in the same space as papers: a weighted mean of the embeddings of
papers they interacted with. Every interaction gets an action weight $a$ and an
exponential recency decay:

```math
w_t = a_t \cdot e^{-\lambda \Delta_t},
\qquad
\lambda = \frac{\ln 2}{T_{1/2}},\quad T_{1/2} = 30\ \text{days}
```

```math
\mathbf{p}_u = \frac{\sum_{t \in \mathcal{I}_u} w_t\, \mathbf{e}_{i_t}}{\sum_{t \in \mathcal{I}_u} w_t}
```

where $\Delta_t$ is the age of interaction $t$ in days. An interaction loses half its
influence every 30 days: one `CITE` today ($w = 3$) outweighs a `SAVE` from a month ago
($w = 0.5$) six times.

| Action | Weight $a$ | Meaning |
|---|---|---|
| `VIEW` | 0.2 | card hovered for 2 s |
| `CLICK` | 0.5 | card opened |
| `SAVE` | 1.0 | bookmarked |
| `DOWNLOAD` | 1.5 | PDF downloaded |
| `CITE` | 3.0 | added to personal reference list |

Papers without an embedding are skipped. If the total weight is $0$, the user has no
profile and the HNSW signal is disabled for the request.

---

### 2. HNSW — content-based retrieval

`native/hnsw/hnsw.c` · Malkov & Yashunin, 2016

**Structure.** A multi-layer proximity graph. Layer 0 contains every node; each higher
layer is an exponentially thinner subset. A node's top layer is sampled once on insert:

```math
\ell = \left\lfloor -\ln(U) \cdot m_L \right\rfloor,
\qquad U \sim \mathcal{U}(0,1],
\qquad m_L = \frac{1}{\ln M}
```

so $P(\ell \ge L) = M^{-L}$: with $M = 16$, one node in 16 reaches layer 1, one in 256
reaches layer 2, and so on. The expected number of layers is $O(\log_M N)$. Randomness
comes from a seeded xorshift64 generator, so builds are reproducible.

Each node keeps at most $M = 16$ neighbours per layer, and $M_0 = 2M = 32$ on layer 0.

**Search** (`hnsw_search`):

1. Start at the global entry point on the top layer.
2. For each layer $L_{\max} \dots 1$: run a greedy search with beam width $ef = 1$
   and move to the closest node found.
3. On layer 0: run a beam search with $ef_{search} = \max(50, k)$ and return the $k$
   closest nodes.

The beam search (`search_layer`) keeps two heaps: a min-heap of candidates to expand and a
max-heap $W$ of the best $ef$ results. It stops when the closest unexpanded candidate is
farther than the worst result in $W$ — no closer node can be reached through it.

**Insertion** (`hnsw_add`): descend greedily to layer $\ell + 1$, then on each layer
$\min(\ell, L_{\max}) \dots 0$ run a beam search with $ef_{construction} = 200$, connect
the new node to its $M$ (or $M_0$) nearest results, and add the reverse edges. A neighbour
whose list overflows is pruned back to its $M_{\max}$ closest nodes.

| Operation | Expected cost |
|---|---|
| Insert | $O(ef_{construction} \cdot M \cdot \log N \cdot D)$ |
| Query | $O(ef_{search} \cdot M \cdot \log N \cdot D)$ |
| Memory | $O(N \cdot (D + M_0))$ |

$D = 768$. Papers without an embedding are inserted as zero vectors so that
**HNSW node id = paper index**. Their distance is always $1$, so they never rank above a
real match.

---

### 3. BM25 — text retrieval

`algorithm/bm25/Bm25Index.java` · Robertson & Spärck Jones

**Tokenization** (`Tokenizer`): lower-case the title and abstract, split on
`[^a-z0-9]+`, and drop tokens shorter than 2 characters.

**Score** of document $d$ for query $Q$:

```math
\operatorname{BM25}(d, Q) = \sum_{t \in Q} \operatorname{IDF}(t)\cdot
\frac{f(t,d)\,(k_1 + 1)}{f(t,d) + k_1\left(1 - b + b\,\dfrac{|d|}{\text{avgdl}}\right)}
```

```math
\operatorname{IDF}(t) = \ln\!\left(\frac{N - n(t) + 0.5}{n(t) + 0.5} + 1\right)
```

- $f(t,d)$ — term frequency of $t$ in $d$
- $n(t)$ — number of documents containing $t$
- $|d|$, avgdl — document length and average document length, in tokens
- $k_1 = 1.2$ — term-frequency saturation: the TF part is bounded by $k_1 + 1 = 2.2$, so
  repeating a word has diminishing returns
- $b = 0.75$ — strength of length normalisation

The $+1$ inside the logarithm keeps IDF positive even for terms that appear in more than
half of the corpus (the classic Robertson IDF would go negative there).

**Index:** an inverted index `term → [(docId, tf)]` is built once. A query touches only
the posting lists of its own terms: $O\!\left(\sum_{t \in Q} n(t)\right)$, followed by a
sort of the matched documents.

---

### 4. BPR — collaborative filtering

`native/bpr/bpr.c` · Rendle et al., 2009

**Data.** Implicit feedback: $D = \{(u, i)\}$ — user $u$ interacted with paper $i$ at
least once, with any action type.

**Model.** Matrix factorization with $F = 64$ latent factors:

```math
\hat{x}_{ui} = \mathbf{p}_u^{\top}\mathbf{q}_i,
\qquad \mathbf{p}_u, \mathbf{q}_i \in \mathbb{R}^{F}
```

**Objective.** BPR optimises pairwise order, not absolute scores: an observed paper $i$
should rank above an unobserved paper $j$.

```math
\hat{x}_{uij} = \hat{x}_{ui} - \hat{x}_{uj} = \mathbf{p}_u^{\top}(\mathbf{q}_i - \mathbf{q}_j)
```

```math
\max_{\Theta}\ \sum_{(u,i,j)} \ln \sigma(\hat{x}_{uij}) \;-\; \lambda_{\Theta}\lVert\Theta\rVert^2,
\qquad \sigma(x) = \frac{1}{1 + e^{-x}}
```

This is a smooth approximation of AUC maximisation.

**Training** — stochastic gradient ascent. For each epoch the positive pairs are shuffled
(Fisher–Yates). For every pair $(u, i)$ a negative $j$ is sampled uniformly from all papers
($j \ne i$). With $g = 1 - \sigma(\hat{x}_{uij})$ and $\mathbf{p}_u$ taken *before* the
step:

```math
\begin{aligned}
\mathbf{p}_u &\leftarrow \mathbf{p}_u + \eta\,\big(g\,(\mathbf{q}_i - \mathbf{q}_j) - \lambda\,\mathbf{p}_u\big)\\
\mathbf{q}_i &\leftarrow \mathbf{q}_i + \eta\,\big(g\,\mathbf{p}_u - \lambda\,\mathbf{q}_i\big)\\
\mathbf{q}_j &\leftarrow \mathbf{q}_j + \eta\,\big(-g\,\mathbf{p}_u - \lambda\,\mathbf{q}_j\big)
\end{aligned}
```

$g$ is large when the model still orders the pair wrongly and approaches $0$ once
$\hat{x}_{ui} \gg \hat{x}_{uj}$, so well-learned pairs stop contributing.

| Parameter | Value |
|---|---|
| factors $F$ | 64 |
| epochs | 100 |
| learning rate $\eta$ | 0.01 |
| regularisation $\lambda$ | 0.001 |
| initialisation | $\mathcal{U}[0, 0.01]$, seed 42 |

**Inference.** Score every paper with $\mathbf{p}_u^{\top}\mathbf{q}_i$, give already-seen
papers a score of $-10^{30}$, and take the top $C$ by partial selection sort.

Cost: training $O(\text{epochs}\cdot|D|\cdot F)$, inference $O(N F + C N)$. The model is
retrained on every reindex.

---

### 5. PageRank — citation authority

`native/pagerank/pagerank.c` · Brin & Page, 1998

The citation graph has an edge $s \to d$ when paper $s$ cites paper $d$. Authority flows
along citations:

```math
r^{(t+1)}_d \;=\; \underbrace{\frac{1-\alpha}{N}}_{\text{teleport}}
\;+\; \underbrace{\frac{\alpha}{N}\sum_{s:\ \text{out}(s)=0} r^{(t)}_s}_{\text{dangling mass}}
\;+\; \alpha \sum_{s \to d} \frac{r^{(t)}_s}{\text{out}(s)}
```

with $r^{(0)} = \frac{1}{N}\mathbf{1}$ and damping $\alpha = 0.85$.

- **Teleportation** models a reader who jumps to a random paper with probability $1-\alpha$.
  It makes the Markov chain irreducible and aperiodic, so the stationary distribution
  exists and is unique.
- **Dangling papers** (no outgoing citations within the corpus — very common for papers
  whose references were not ingested) redistribute their mass uniformly instead of
  leaking it. This keeps $\sum_d r_d = 1$ at every iteration.

**Convergence.** The update is a contraction with factor $\alpha$ in the $L_1$ norm:

```math
\lVert r^{(t)} - r^{*}\rVert_1 \le 2\,\alpha^{t}
```

After the configured 20 iterations: $\alpha^{20} = 0.85^{20} \approx 0.039$.

Cost: $O(\text{iterations}\cdot(N + |E|))$. The score is global (not personalised): it
ranks papers by authority and acts as the cold-start signal.

---

### 6. Reciprocal Rank Fusion

`algorithm/rerank/FusionReranker.java` · Cormack et al., 2009

The four signals produce incomparable scores (cosine distance, BM25, dot products,
probabilities), so they are fused by **rank**, not by score:

```math
\operatorname{RRF}(d) = \sum_{s \in \text{signals}} \frac{1}{k_{\text{rrf}} + \operatorname{rank}_s(d)},
\qquad k_{\text{rrf}} = 60
```

$\operatorname{rank}_s(d)$ starts at 1. A paper missing from a signal's list gets nothing
from that signal.

The constant $k_{\text{rrf}}$ flattens the curve: rank 1 gives $1/61 \approx 0.0164$ and
rank 10 gives $1/70 \approx 0.0143$. **Agreement between signals beats a single first
place**: a paper ranked #2 by two signals scores $2/62 \approx 0.0323$, almost twice a
paper ranked #1 by only one.

---

### 7. MMR — diversity re-ranking

`algorithm/rerank/MmrReranker.java` · Carbonell & Goldstein, 1998

The fused list is often redundant: several near-identical papers on the same subtopic.
Maximal Marginal Relevance builds the final list greedily:

```math
d^{*} = \arg\max_{d \in R \setminus S}
\Big[\, \lambda \cdot \operatorname{rel}(d) \;-\; (1-\lambda)\cdot \max_{s \in S} \operatorname{sim}(\mathbf{e}_d, \mathbf{e}_s) \Big]
```

- $R$ — fused candidates, $S$ — papers already selected
- $\operatorname{rel}(d) = \dfrac{1}{1 + \text{position of } d \text{ in the fused list}}$ (positions start at 0)
- $\operatorname{sim}$ — cosine similarity of embeddings ($0$ if either embedding is missing)
- $\lambda = 0.7$ — mostly relevance, with a noticeable diversity penalty

$\lambda = 1$ reproduces the fused order. $\lambda = 0$ picks the paper least similar to
everything already chosen. MMR selects a pool of $2k$; papers the user has already
interacted with are then removed and the first $k$ are returned.

Cost: $O(P \cdot |R| \cdot D)$ for a pool of size $P = 2k$.

---

### Complexity summary

$N$ papers, $|E|$ citation edges, $|D|$ interactions, $D = 768$, $F = 64$, $k \le 100$.

| Stage | Build / train (on reindex) | Per request |
|---|---|---|
| User profile | — | $O(\lvert\mathcal{I}_u\rvert \cdot D)$ |
| HNSW | $O(N \log N \cdot ef_c M D)$ | $O(\log N \cdot ef\, M D)$ |
| BM25 | $O(\text{total tokens})$ | $O(\sum_{t\in Q} n(t) + m \log m)$ |
| BPR | $O(\text{epochs}\cdot \lvert D\rvert \cdot F)$ | $O(NF + CN)$ |
| PageRank | $O(\text{iter}\cdot(N + \lvert E\rvert))$ | $O(N \log N)$ (sort) |
| RRF | — | $O(4C \log C)$ |
| MMR | — | $O(k \cdot C \cdot D)$ |

### Hyperparameters

| Component | Parameter | Value | Defined in |
|---|---|---|---|
| Profile | half-life | 30 days | `IndexService` |
| HNSW | $M$ / $M_0$ | 16 / 32 | `IndexService`, `hnsw.c` |
| HNSW | $ef_{construction}$ / $ef_{search}$ | 200 / 50 | `IndexService` |
| BM25 | $k_1$ / $b$ | 1.2 / 0.75 | `Bm25Index` |
| BPR | $F$, epochs, $\eta$, $\lambda$ | 64, 100, 0.01, 0.001 | `IndexService` |
| PageRank | $\alpha$, iterations | 0.85, 20 | `IndexService` |
| RRF | $k_{\text{rrf}}$ | 60 | `FusionReranker` |
| MMR | $\lambda$ | 0.7 | `RecommendationService` |
| Pipeline | candidates per signal / MMR pool | $5k$ / $2k$ | `RecommendationService` |

All indexes share one integer key, `paperIdx` (HNSW node = BM25 document = BPR item =
PageRank node), so signals can be fused without ID translation. Embeddings are stored in
PostgreSQL as little-endian float32 `bytea`. Index rebuilds are protected against
concurrent runs by an `AtomicBoolean`.

---

## Tech stack

- Java 17, Spring Boot 3.2 (Web, Data JPA)
- PostgreSQL + Flyway migrations, HikariCP
- Jackson for external API responses
- C11 + CMake + JNI for the native libraries `hnsw`, `bpr`, `pagerank`
- JUnit 5

## Project layout

```
src/main/java/com/recommender/
├── algorithm/        BM25, fusion (RRF), MMR
├── api/              Semantic Scholar, OpenAlex and arXiv clients
├── config/           initial data seeding, index warm-up
├── db/               JPA entities, repositories, float[] ↔ bytea converter
├── jni/              Java wrappers for native libraries + library loader
├── model/            domain models (Paper, User, Interaction)
├── service/          business logic: indexing, ingestion, recommendations
└── web/              REST controllers and DTOs

src/main/resources/
├── application.properties
├── db/migration/     Flyway SQL migrations
├── native/           built native libraries (copied here by CMake)
└── static/           index.html (UI), admin.html (admin console)

src/test/java/        unit tests (BM25, RRF/MMR, API parsers, interactions)

native/               C sources: hnsw/, bpr/, pagerank/ + CMakeLists.txt
requests.http         ready-made requests for the IntelliJ HTTP Client
```

## Getting started

### Prerequisites

- JDK 17+
- PostgreSQL (local or hosted)
- CMake ≥ 3.20 and a C compiler (MSVC on Windows, GCC/Clang on Linux/macOS)

### 1. Build the native libraries

Needed once, and again after any change in `native/`. The libraries are copied to
`src/main/resources/native/` automatically.

```bash
cmake -S native -B native/build -DJAVA_HOME=<path to JDK>
cmake --build native/build --config Release
```

On Windows you can choose the generator explicitly: `-G "Visual Studio 17 2022"`.

### 2. Configure the database

Credentials are **not** stored in the repository. Use either option:

**Environment variables**

```bash
DB_URL=jdbc:postgresql://<host>:5432/<db>
DB_USERNAME=<user>
DB_PASSWORD=<password>
```

**Local config file** — `config/application.properties` in the project root (git-ignored,
loaded by Spring Boot automatically when the app is started from the project root):

```properties
spring.datasource.url=jdbc:postgresql://<host>:5432/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>
```

Without either, the app connects to `jdbc:postgresql://localhost:5432/paperrecommender`
as `postgres`. The schema is created by Flyway on first start.

Optional settings:

```properties
semanticscholar.api-key=   # Semantic Scholar API key — higher rate limits
openalex.email=            # email for the OpenAlex "polite pool"
```

### 3. Run

Run `PaperRecommenderApplication` from your IDE, or:

```bash
mvn spring-boot:run
```

If the database holds fewer than 10 papers, the app seeds ~40 papers across 5 topics from
Semantic Scholar and builds every index.

- User UI: <http://localhost:8080/>
- Admin console: <http://localhost:8080/admin.html>

### 4. Populate the citation graph (recommended)

PageRank needs citation edges. After the first start, run a deep ingest once:

```
POST /api/admin/deep-ingest-all?maxRefs=20&delayMs=400
```

### Tests

```bash
mvn clean test
```

## REST API

### Recommendations

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/recommend?userId=&q=&k=` | Recommendations (all parameters optional, `k` ≤ 100) |
| `GET` | `/api/recommend/explain?userId=&q=&k=` | Same, with per-signal breakdown |

### Users and interactions

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/users` | Create a user |
| `GET` | `/api/users` | List users |
| `GET` | `/api/users/{id}` | Get a user |
| `POST` | `/api/interactions` | Record an action (`VIEW`, `CLICK`, `SAVE`, `DOWNLOAD`, `CITE`) |
| `GET` | `/api/interactions/user/{userId}` | Interaction history |
| `GET` | `/api/interactions/user/{userId}/seen` | IDs of papers the user has seen |

### Papers

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/papers` | Add a paper |
| `POST` | `/api/papers/batch` | Add papers in bulk |
| `GET` | `/api/papers/{id}` | Get by internal UUID |
| `GET` | `/api/papers/s2/{s2Id}` | Get by Semantic Scholar ID |
| `GET` | `/api/papers/arxiv/{arxivId}` | Get by arXiv ID |
| `POST` | `/api/papers/{citingId}/citations/{citedId}` | Add a citation edge |

### Ingestion

Indexes are rebuilt automatically after every ingest.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/ingest/s2?query=&limit=20` | Search and import from Semantic Scholar (`limit` ≤ 100) |
| `POST` | `/api/ingest/s2/{paperId}?maxRefs=50` | Paper + references + citation edges (`maxRefs` ≤ 500) |
| `POST` | `/api/ingest/arxiv?query=&maxResults=20` | Import from arXiv (`maxResults` ≤ 200) |

### Administration

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/reindex` | Rebuild HNSW, BM25, BPR and PageRank |
| `POST` | `/api/admin/deep-ingest-all` | Deep-ingest every Semantic Scholar paper |
| `GET` | `/api/admin/stats` | Index status |
| `GET` | `/api/admin/search-debug?q=` | Debug BM25 search |
| `GET` | `/api/admin/users-overview` | Users summary |
| `GET` | `/api/admin/user-detail/{userId}` | User details |
| `GET` | `/api/admin/paper-stats` | Paper statistics |
| `GET` | `/api/admin/index-detail` | Index details |
| `GET` | `/api/admin/recommend-explain` | Recommendation explanation |

Example requests are in [`requests.http`](requests.http).

## Security notes

- **No authentication.** All endpoints, including `/api/admin/**` and `/api/ingest/**`,
  are open; users are identified by name only. Run the app on `localhost` or behind an
  authenticating reverse proxy — do not expose it to the internet as-is.
- Database credentials come from environment variables or the git-ignored
  `config/application.properties`; never commit them.
- Numeric request parameters (`k`, `limit`, `maxRefs`, `delayMs`, …) are clamped
  server-side, so a request cannot make the native code allocate unbounded buffers.
- Unexpected errors return a generic 500 message; details are only written to the server log.
- The UI HTML-escapes all data that comes from external APIs or from users before
  inserting it into the page.
- Outgoing requests to Semantic Scholar, arXiv and OpenAlex URL-encode every
  user-supplied value.
