package com.recommender.api;

import com.recommender.api.arxiv.ArxivClient;
import com.recommender.api.arxiv.ArxivClient.ArxivPaper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArxivParserTest {

    /** Minimal valid Atom feed with one entry. */
    private static final String ATOM_FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom"
                  xmlns:arxiv="http://arxiv.org/schemas/atom">
              <entry>
                <id>http://arxiv.org/abs/1706.03762v5</id>
                <title>  Attention Is All You Need  </title>
                <summary>
                  The dominant sequence transduction models are based on complex
                  recurrent or convolutional neural networks.
                </summary>
                <published>2017-06-12T17:57:34Z</published>
                <updated>2023-08-02T05:17:48Z</updated>
                <author><name>Ashish Vaswani</name></author>
                <author><name>Noam Shazeer</name></author>
                <category xmlns="http://www.w3.org/2005/Atom" term="cs.CL"/>
                <category xmlns="http://www.w3.org/2005/Atom" term="cs.LG"/>
                <arxiv:primary_category xmlns:arxiv="http://arxiv.org/schemas/atom"
                                        term="cs.CL"/>
                <link rel="alternate" type="text/html"
                      href="http://arxiv.org/abs/1706.03762v5"/>
                <link rel="related" type="application/pdf"
                      href="http://arxiv.org/pdf/1706.03762v5"/>
              </entry>
            </feed>
            """;

    @SuppressWarnings("unchecked")
    private List<ArxivPaper> parse(String xml) throws Exception {
        ArxivClient client = new ArxivClient();
        Method m = ArxivClient.class.getDeclaredMethod("parseAtomFeed", String.class);
        m.setAccessible(true);
        return (List<ArxivPaper>) m.invoke(client, xml);
    }

    @Test
    void parsesTitle() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertEquals("Attention Is All You Need", p.title);
    }

    @Test
    void parsesArxivId() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertEquals("1706.03762", p.arxivId);
    }

    @Test
    void parsesAuthors() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertEquals(2, p.authors.size());
        assertTrue(p.authors.contains("Ashish Vaswani"));
        assertTrue(p.authors.contains("Noam Shazeer"));
    }

    @Test
    void parsesCategories() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertTrue(p.categories.contains("cs.CL"));
        assertTrue(p.categories.contains("cs.LG"));
        assertEquals("cs.CL", p.primaryCategory);
    }

    @Test
    void parsesPdfUrl() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertEquals("http://arxiv.org/pdf/1706.03762v5", p.pdfUrl);
    }

    @Test
    void parsesTimestamps() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertNotNull(p.published);
        assertNotNull(p.updated);
        assertEquals(2017, p.published.atZone(java.time.ZoneOffset.UTC).getYear());
    }

    @Test
    void abstractIsNormalisedWhitespace() throws Exception {
        ArxivPaper p = parse(ATOM_FEED).get(0);
        assertFalse(p.abstractText.contains("\n"), "abstract should have no newlines");
        assertFalse(p.abstractText.startsWith(" "), "abstract should be trimmed");
    }

    @Test
    void emptyFeedReturnsEmptyList() throws Exception {
        String empty = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom"></feed>
                """;
        List<ArxivPaper> result = parse(empty);
        assertTrue(result.isEmpty());
    }
}
