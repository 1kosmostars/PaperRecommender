package com.recommender.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommender.api.openalex.OpenAlexClient.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAlexDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseWorkJson() throws Exception {
        String json = """
                {
                  "id": "https://openalex.org/W2741809807",
                  "doi": "https://doi.org/10.18653/v1/p16-1162",
                  "title": "Neural Machine Translation by Jointly Learning to Align and Translate",
                  "publication_year": 2015,
                  "cited_by_count": 25000,
                  "concepts": [
                    { "id": "https://openalex.org/C41008148", "display_name": "Computer science",
                      "level": 0, "score": 0.9 }
                  ],
                  "topics": [
                    {
                      "id": "https://openalex.org/T10001",
                      "display_name": "Neural Machine Translation",
                      "score": 0.95,
                      "subfield": { "id": "SF1", "display_name": "NLP" },
                      "field":    { "id": "F1",  "display_name": "CS" },
                      "domain":   { "id": "D1",  "display_name": "Science" }
                    }
                  ],
                  "open_access": { "is_oa": true, "oa_url": "https://arxiv.org/pdf/1409.0473" },
                  "authorships": [
                    {
                      "author": { "id": "A1", "display_name": "Dzmitry Bahdanau", "orcid": null },
                      "institutions": []
                    }
                  ],
                  "extra_unknown_field": 42
                }
                """;

        OAWork work = mapper.readValue(json, OAWork.class);

        assertEquals("https://openalex.org/W2741809807", work.id);
        assertEquals("W2741809807", work.shortId());
        assertEquals(2015, work.publicationYear);
        assertEquals(25000, work.citedByCount);
        assertNotNull(work.concepts);
        assertEquals(1, work.concepts.size());
        assertEquals("Computer science", work.concepts.get(0).displayName);
        assertNotNull(work.topics);
        assertEquals(1, work.topics.size());
        assertEquals("NLP", work.topics.get(0).subfield.displayName);
        assertTrue(work.openAccess.isOa);
        assertEquals("Dzmitry Bahdanau", work.authorships.get(0).author.displayName);
    }

    @Test
    void parseConceptJson() throws Exception {
        String json = """
                {
                  "id": "https://openalex.org/C41008148",
                  "display_name": "Computer science",
                  "level": 0,
                  "works_count": 1000000,
                  "description": "Science of computation"
                }
                """;

        OAConcept concept = mapper.readValue(json, OAConcept.class);
        assertEquals("Computer science", concept.displayName);
        assertEquals(0, concept.level);
        assertEquals(1000000, concept.worksCount);
    }
}
