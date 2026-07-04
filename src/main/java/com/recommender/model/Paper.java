package com.recommender.model;

import java.util.List;

/**
 * Represents a scientific paper with metadata and embeddings.
 */
public class Paper {

    private String id;            // internal id
    private String s2Id;          // Semantic Scholar paperId
    private String arxivId;       // arXiv id (e.g. "2301.07041")
    private String doi;
    private String title;
    private String abstractText;
    private int year;
    private List<String> topics;  // topic labels (from OpenAlex concepts / LDA)
    private float[] embedding;    // SPECTER2 vector, dim=768
    private int citationCount;
    private int referenceCount;
    private List<String> authorNames;

    public Paper() {}

    public Paper(String s2Id, String title, String abstractText, int year) {
        this.s2Id = s2Id;
        this.title = title;
        this.abstractText = abstractText;
        this.year = year;
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getId()                       { return id; }
    public void   setId(String id)              { this.id = id; }

    public String getS2Id()                     { return s2Id; }
    public void   setS2Id(String s2Id)          { this.s2Id = s2Id; }

    public String getArxivId()                  { return arxivId; }
    public void   setArxivId(String arxivId)    { this.arxivId = arxivId; }

    public String getDoi()                      { return doi; }
    public void   setDoi(String doi)            { this.doi = doi; }

    public String getTitle()                    { return title; }
    public void   setTitle(String title)        { this.title = title; }

    public String getAbstractText()                         { return abstractText; }
    public void   setAbstractText(String abstractText)      { this.abstractText = abstractText; }

    public int  getYear()                       { return year; }
    public void setYear(int year)               { this.year = year; }

    public List<String> getTopics()                     { return topics; }
    public void         setTopics(List<String> topics)  { this.topics = topics; }

    public float[] getEmbedding()                       { return embedding; }
    public void    setEmbedding(float[] embedding)      { this.embedding = embedding; }

    public int  getCitationCount()                          { return citationCount; }
    public void setCitationCount(int citationCount)         { this.citationCount = citationCount; }

    public int  getReferenceCount()                         { return referenceCount; }
    public void setReferenceCount(int referenceCount)       { this.referenceCount = referenceCount; }

    public List<String> getAuthorNames()                            { return authorNames; }
    public void         setAuthorNames(List<String> authorNames)    { this.authorNames = authorNames; }

    @Override
    public String toString() {
        return "Paper{s2Id='" + s2Id + "', title='" + title + "', year=" + year + "}";
    }
}
