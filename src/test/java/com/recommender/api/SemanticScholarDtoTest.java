package com.recommender.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommender.api.semanticscholar.SemanticScholarClient.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemanticScholarDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsePaperJson() throws Exception {
        String json = """
                {
                  "paperId": "abc123",
                  "title": "Attention Is All You Need",
                  "abstract": "The dominant sequence...",
                  "year": 2017,
                  "citationCount": 60000,
                  "referenceCount": 35,
                  "externalIds": { "ArXiv": "1706.03762", "DOI": "10.48550/arXiv.1706.03762" },
                  "authors": [
                    { "authorId": "a1", "name": "Ashish Vaswani" }
                  ],
                  "embedding": {
                    "model": "SPECTER_V2",
                    "vector": [0.1, 0.2, 0.3]
                  },
                  "unknownField": "should be ignored"
                }
                """;

        S2Paper paper = mapper.readValue(json, S2Paper.class);

        assertEquals("abc123", paper.paperId);
        assertEquals("Attention Is All You Need", paper.title);
        assertEquals(2017, paper.year);
        assertEquals(60000, paper.citationCount);
        assertEquals("1706.03762", paper.getArxivId());
        assertNotNull(paper.authors);
        assertEquals(1, paper.authors.size());
        assertEquals("Ashish Vaswani", paper.authors.get(0).name);

        float[] emb = paper.getEmbeddingVector();
        assertNotNull(emb);
        assertEquals(3, emb.length);
        assertEquals(0.1f, emb[0], 1e-5f);
    }

    @Test
    void parsePaperWithNullEmbeddingReturnsNull() throws Exception {
        String json = """
                { "paperId": "x", "title": "T", "year": 2020 }
                """;
        S2Paper paper = mapper.readValue(json, S2Paper.class);
        assertNull(paper.getEmbeddingVector());
        assertNull(paper.getArxivId());
    }

    @Test
    void parseCitationJson() throws Exception {
        String json = """
                {
                  "citingPaper": { "paperId": "citer1", "title": "Citing Paper", "year": 2019 },
                  "citedPaper":  { "paperId": "cited1", "title": "Cited Paper",  "year": 2017 }
                }
                """;
        S2Citation citation = mapper.readValue(json, S2Citation.class);
        assertEquals("citer1", citation.citingPaper.paperId);
        assertEquals("cited1", citation.citedPaper.paperId);
    }
}
