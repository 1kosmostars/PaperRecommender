package com.recommender.db.repository;

import com.recommender.db.entity.PaperEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaperRepository extends JpaRepository<PaperEntity, UUID> {

    Optional<PaperEntity> findByS2Id(String s2Id);

    Optional<PaperEntity> findByArxivId(String arxivId);

    Optional<PaperEntity> findByDoi(String doi);

    List<PaperEntity> findByYear(int year);

    List<PaperEntity> findByYearBetween(int from, int to);

    /** Papers that have an embedding stored (needed to load HNSW index). */
    @Query("SELECT p FROM PaperEntity p WHERE p.embedding IS NOT NULL")
    List<PaperEntity> findAllWithEmbedding();

    /** Papers citing a given paper (in-edges for PageRank). */
    @Query(value = """
            SELECT p.* FROM papers p
            JOIN paper_citations c ON c.citing_paper_id = p.id
            WHERE c.cited_paper_id = :paperId
            """, nativeQuery = true)
    List<PaperEntity> findCitingPapers(UUID paperId);

    /** Fetch all papers together with their cited-paper-id sets in one query (avoids N+1 on warm-up). */
    @Query("SELECT DISTINCT p FROM PaperEntity p LEFT JOIN FETCH p.citedPaperIds")
    List<PaperEntity> findAllWithCitations();

    @Query(value = "SELECT topic, COUNT(*) AS cnt FROM paper_topics GROUP BY topic ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByTopic();

    @Query(value = "SELECT year, COUNT(*) AS cnt FROM papers WHERE year IS NOT NULL GROUP BY year ORDER BY year ASC", nativeQuery = true)
    List<Object[]> countByYear();
}
