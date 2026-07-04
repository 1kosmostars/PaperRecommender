package com.recommender.service;

import com.recommender.api.arxiv.ArxivClient;
import com.recommender.api.semanticscholar.SemanticScholarClient;
import com.recommender.api.semanticscholar.SemanticScholarClient.S2Citation;
import com.recommender.api.semanticscholar.SemanticScholarClient.S2Paper;
import com.recommender.db.entity.PaperEntity;
import com.recommender.model.Paper;
import com.recommender.web.dto.IngestResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Orchestrates paper ingestion from external APIs (Semantic Scholar, arXiv).
 *
 * <p>Each ingest method fetches papers, converts them to the internal model,
 * persists via {@link PaperService}, and returns a summary of results.
 *
 * <p><b>Rate limits</b>: S2 allows 100 req/s unauthenticated; arXiv allows ~1 req/3s.
 * The caller is responsible for throttling when running bulk ingests.
 * A Semantic Scholar API key (semanticscholar.api-key in application.properties) is
 * recommended for large ingests.
 */
@Service
public class IngestService {

    private static final Logger log = Logger.getLogger(IngestService.class.getName());

    private final SemanticScholarClient s2Client;
    private final ArxivClient           arxivClient;
    private final PaperService          paperService;

    public IngestService(SemanticScholarClient s2Client,
                         ArxivClient arxivClient,
                         PaperService paperService) {
        this.s2Client    = s2Client;
        this.arxivClient = arxivClient;
        this.paperService = paperService;
    }

    // ── Semantic Scholar ──────────────────────────────────────────────────────

    /**
     * Search Semantic Scholar and save all results.
     *
     * @param query text query
     * @param limit number of papers to fetch (max 100 per S2 request)
     */
    @Transactional
    public IngestResult ingestFromSemanticScholar(String query, int limit) {
        List<String> errors = new ArrayList<>();
        int saved = 0, skipped = 0;

        try {
            List<S2Paper> papers = s2Client.searchPapers(query, Math.min(limit, 100));
            for (S2Paper s2 : papers) {
                if (s2.title == null || s2.title.isBlank()) { skipped++; continue; }
                try {
                    paperService.save(fromS2(s2));
                    saved++;
                } catch (Exception e) {
                    errors.add("s2:" + s2.paperId + " — " + e.getMessage());
                    skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("Search failed: " + e.getMessage());
        }

        log.info("[IngestService] S2 ingest done — saved=" + saved + " skipped=" + skipped);
        return IngestResult.of(saved, skipped, errors);
    }

    /**
     * Fetch a single paper from Semantic Scholar by its paperId (or "ARXIV:...", "DOI:..."),
     * save it, then fetch and save its references (papers it cites) up to {@code maxRefs}.
     * Citation edges are persisted so the PageRank graph is populated.
     *
     * @param s2PaperId Semantic Scholar paperId or qualified id ("ARXIV:2301.07041")
     * @param maxRefs   max number of references to also ingest (0 = paper only)
     */
    @Transactional
    public IngestResult ingestPaperDeep(String s2PaperId, int maxRefs) {
        List<String> errors = new ArrayList<>();
        int saved = 0, skipped = 0;

        // ── fetch and save the root paper ──────────────────────────────────────
        PaperEntity root;
        try {
            S2Paper s2 = s2Client.getPaper(s2PaperId);
            if (s2.title == null) {
                return IngestResult.of(0, 1, List.of("Paper not found or missing title: " + s2PaperId));
            }
            root = paperService.save(fromS2(s2));
            saved++;
        } catch (Exception e) {
            return IngestResult.of(0, 1, List.of("Failed to fetch root paper: " + e.getMessage()));
        }

        if (maxRefs <= 0) return IngestResult.of(saved, skipped, errors);

        // ── fetch references (papers cited by root) ────────────────────────────
        List<S2Citation> refs;
        try {
            refs = s2Client.getReferences(root.getS2Id(), maxRefs);
        } catch (Exception e) {
            errors.add("Could not fetch references: " + e.getMessage());
            return IngestResult.of(saved, skipped, errors);
        }

        for (S2Citation ref : refs) {
            S2Paper cited = ref.citedPaper;
            if (cited == null || cited.paperId == null || cited.title == null) {
                skipped++;
                continue;
            }
            try {
                PaperEntity citedEntity = paperService.save(fromS2(cited));
                paperService.addCitation(root.getId(), citedEntity.getId());
                saved++;
            } catch (Exception e) {
                errors.add("ref:" + cited.paperId + " — " + e.getMessage());
                skipped++;
            }
        }

        log.info("[IngestService] Deep ingest for " + s2PaperId
                + " done — saved=" + saved + " skipped=" + skipped);
        return IngestResult.of(saved, skipped, errors);
    }

    // ── arXiv ─────────────────────────────────────────────────────────────────

    /**
     * Search arXiv and save all results.
     * arXiv papers have no embeddings; those can be added later via a S2 batch lookup.
     *
     * @param query      arXiv query (e.g. "ti:attention AND cat:cs.LG")
     * @param maxResults max papers to fetch (arXiv allows up to 2000)
     */
    @Transactional
    public IngestResult ingestFromArxiv(String query, int maxResults) {
        List<String> errors = new ArrayList<>();
        int saved = 0, skipped = 0;

        try {
            List<ArxivClient.ArxivPaper> papers = arxivClient.search(query, maxResults);
            for (ArxivClient.ArxivPaper a : papers) {
                if (a.title == null || a.title.isBlank()) { skipped++; continue; }
                try {
                    paperService.save(fromArxiv(a));
                    saved++;
                } catch (Exception e) {
                    errors.add("arxiv:" + a.arxivId + " — " + e.getMessage());
                    skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("arXiv search failed: " + e.getMessage());
        }

        log.info("[IngestService] arXiv ingest done — saved=" + saved + " skipped=" + skipped);
        return IngestResult.of(saved, skipped, errors);
    }

    // ── model conversion ──────────────────────────────────────────────────────

    private Paper fromS2(S2Paper s2) {
        Paper p = new Paper(s2.paperId, s2.title, s2.abstractText,
                s2.year != null ? s2.year : 0);
        p.setS2Id(s2.paperId);
        p.setArxivId(s2.getArxivId());
        if (s2.externalIds != null) p.setDoi(s2.externalIds.doi);
        p.setCitationCount(s2.citationCount != null ? s2.citationCount : 0);
        p.setReferenceCount(s2.referenceCount != null ? s2.referenceCount : 0);
        p.setEmbedding(s2.getEmbeddingVector());
        if (s2.authors != null) {
            p.setAuthorNames(s2.authors.stream()
                    .map(a -> a.name)
                    .filter(Objects::nonNull)
                    .toList());
        }
        if (s2.s2FieldsOfStudy != null) {
            p.setTopics(s2.s2FieldsOfStudy.stream()
                    .map(f -> f.category)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
        }
        return p;
    }

    private Paper fromArxiv(ArxivClient.ArxivPaper a) {
        int year = 0;
        if (a.published != null) {
            year = java.time.ZonedDateTime
                    .ofInstant(a.published, java.time.ZoneOffset.UTC)
                    .getYear();
        }
        Paper p = new Paper(null, a.title, a.abstractText, year);
        p.setArxivId(a.arxivId);
        if (a.authors != null) p.setAuthorNames(a.authors);
        if (a.categories != null) p.setTopics(a.categories);
        return p;
    }
}
