package com.recommender.db.entity;

import com.recommender.db.converter.FloatArrayConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity for a scientific paper.
 * Topics and author names are stored in separate collection tables.
 * The 768-dim embedding is stored as bytea via FloatArrayConverter.
 * Citation links (out-edges for PageRank) are kept in paper_citations.
 */
@Entity
@Table(name = "papers", indexes = {
        @Index(name = "idx_papers_s2_id",    columnList = "s2_id"),
        @Index(name = "idx_papers_arxiv_id", columnList = "arxiv_id"),
        @Index(name = "idx_papers_year",     columnList = "year")
})
public class PaperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "s2_id", unique = true, length = 100)
    private String s2Id;

    @Column(name = "arxiv_id", unique = true, length = 50)
    private String arxivId;

    @Column(name = "doi", unique = true, length = 200)
    private String doi;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "abstract_text", columnDefinition = "text")
    private String abstractText;

    @Column(name = "year")
    private Integer year;

    @Column(name = "citation_count")
    private int citationCount;

    @Column(name = "reference_count")
    private int referenceCount;

    /** SPECTER2 embedding vector (768 floats stored as bytea). */
    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "embedding", columnDefinition = "bytea")
    private float[] embedding;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "paper_topics",
            joinColumns = @JoinColumn(name = "paper_id"),
            indexes = @Index(name = "idx_paper_topics_paper", columnList = "paper_id"))
    @Column(name = "topic", length = 200)
    private List<String> topics = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "paper_authors",
            joinColumns = @JoinColumn(name = "paper_id"),
            indexes = @Index(name = "idx_paper_authors_paper", columnList = "paper_id"))
    @OrderColumn(name = "position")
    @Column(name = "author_name", length = 300)
    private List<String> authorNames = new ArrayList<>();

    /**
     * Papers cited by this paper (out-edges for PageRank / reference graph).
     * Stored in paper_citations(citing_paper_id, cited_paper_id).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "paper_citations",
            joinColumns = @JoinColumn(name = "citing_paper_id"),
            indexes = @Index(name = "idx_citations_citing", columnList = "citing_paper_id"))
    @Column(name = "cited_paper_id")
    private Set<UUID> citedPaperIds = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PaperEntity() {}

    // ── getters / setters ────────────────────────────────────────────────────

    public UUID getId()                              { return id; }
    public void setId(UUID id)                       { this.id = id; }

    public String getS2Id()                          { return s2Id; }
    public void   setS2Id(String s2Id)               { this.s2Id = s2Id; }

    public String getArxivId()                       { return arxivId; }
    public void   setArxivId(String arxivId)         { this.arxivId = arxivId; }

    public String getDoi()                           { return doi; }
    public void   setDoi(String doi)                 { this.doi = doi; }

    public String getTitle()                         { return title; }
    public void   setTitle(String title)             { this.title = title; }

    public String getAbstractText()                  { return abstractText; }
    public void   setAbstractText(String v)          { this.abstractText = v; }

    public Integer getYear()                         { return year; }
    public void    setYear(Integer year)             { this.year = year; }

    public int  getCitationCount()                   { return citationCount; }
    public void setCitationCount(int v)              { this.citationCount = v; }

    public int  getReferenceCount()                  { return referenceCount; }
    public void setReferenceCount(int v)             { this.referenceCount = v; }

    public float[] getEmbedding()                    { return embedding; }
    public void    setEmbedding(float[] embedding)   { this.embedding = embedding; }

    public List<String> getTopics()                  { return topics; }
    public void         setTopics(List<String> v)    { this.topics = v; }

    public List<String> getAuthorNames()             { return authorNames; }
    public void         setAuthorNames(List<String> v){ this.authorNames = v; }

    public Set<UUID> getCitedPaperIds()              { return citedPaperIds; }
    public void      setCitedPaperIds(Set<UUID> v)   { this.citedPaperIds = v; }

    public Instant getCreatedAt()                    { return createdAt; }
}
