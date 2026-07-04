package com.recommender.api.arxiv;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client for the arXiv API (Atom feed over HTTP).
 *
 * <p>Documentation: <a href="https://info.arxiv.org/help/api/index.html">arXiv API docs</a>
 *
 * <p>Rate limit: 1 request / 3 seconds (enforced by the caller if needed).
 *
 * <p>Usage:
 * <pre>{@code
 *   ArxivClient client = new ArxivClient();
 *   List<ArxivPaper> papers = client.search("ti:transformer AND cat:cs.LG", 20);
 *   ArxivPaper paper = client.getById("2301.07041");
 * }</pre>
 */
public class ArxivClient {

    private static final String BASE_URL = "http://export.arxiv.org/api/query";
    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final String ARXIV_NS = "http://arxiv.org/schemas/atom";

    private final HttpClient http;
    private final DocumentBuilder xmlParser;

    public ArxivClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            this.xmlParser = dbf.newDocumentBuilder();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XML parser", e);
        }
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Search arXiv using the query language.
     *
     * <p>Query examples:
     * <ul>
     *   <li>{@code "ti:attention is all you need"} – title search</li>
     *   <li>{@code "abs:contrastive learning"} – abstract search</li>
     *   <li>{@code "cat:cs.LG"} – category filter</li>
     *   <li>{@code "au:Vaswani"} – author search</li>
     *   <li>{@code "ti:BERT AND cat:cs.CL"} – combined</li>
     * </ul>
     *
     * @param query      arXiv query string
     * @param maxResults max number of results (≤ 2000)
     * @return list of papers sorted by relevance (default arXiv ordering)
     */
    public List<ArxivPaper> search(String query, int maxResults) throws IOException, InterruptedException {
        return search(query, maxResults, 0, SortBy.RELEVANCE, SortOrder.DESCENDING);
    }

    /**
     * Search with full control over sorting and pagination.
     */
    public List<ArxivPaper> search(String query, int maxResults, int start,
                                   SortBy sortBy, SortOrder sortOrder)
            throws IOException, InterruptedException {

        String url = BASE_URL
                + "?search_query=" + encode(query)
                + "&max_results=" + maxResults
                + "&start=" + start
                + "&sortBy=" + sortBy.value
                + "&sortOrder=" + sortOrder.value;

        String body = get(url);
        return parseAtomFeed(body);
    }

    /**
     * Fetch a specific paper by its arXiv id (e.g. "2301.07041" or "2301.07041v2").
     */
    public ArxivPaper getById(String arxivId) throws IOException, InterruptedException {
        String url = BASE_URL + "?id_list=" + encode(arxivId);
        String body = get(url);
        List<ArxivPaper> results = parseAtomFeed(body);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Fetch multiple papers by their arXiv ids.
     */
    public List<ArxivPaper> getByIds(List<String> arxivIds) throws IOException, InterruptedException {
        if (arxivIds.isEmpty()) return Collections.emptyList();
        String url = BASE_URL + "?id_list=" + encode(String.join(",", arxivIds));
        String body = get(url);
        return parseAtomFeed(body);
    }

    // ── enums ─────────────────────────────────────────────────────────────────

    public enum SortBy {
        RELEVANCE("relevance"),
        LAST_UPDATED("lastUpdatedDate"),
        SUBMITTED("submittedDate");

        final String value;
        SortBy(String v) { this.value = v; }
    }

    public enum SortOrder {
        ASCENDING("ascending"),
        DESCENDING("descending");

        final String value;
        SortOrder(String v) { this.value = v; }
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public static class ArxivPaper {
        public String id;           // full URL: http://arxiv.org/abs/2301.07041v2
        public String arxivId;      // short id: 2301.07041
        public String title;
        public String abstractText;
        public Instant published;
        public Instant updated;
        public List<String> authors;
        public List<String> categories; // e.g. ["cs.LG", "cs.AI"]
        public String primaryCategory;
        public String pdfUrl;
        public String htmlUrl;

        @Override
        public String toString() {
            return "ArxivPaper{id='" + arxivId + "', title='" + title + "'}";
        }
    }

    // ── XML parsing ───────────────────────────────────────────────────────────

    private List<ArxivPaper> parseAtomFeed(String xml) {
        try {
            Document doc = xmlParser.parse(new InputSource(new StringReader(xml)));
            NodeList entries = doc.getElementsByTagNameNS(ATOM_NS, "entry");
            List<ArxivPaper> papers = new ArrayList<>(entries.getLength());

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                ArxivPaper paper = parseEntry(entry);
                papers.add(paper);
            }
            return papers;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse arXiv Atom feed", e);
        }
    }

    private ArxivPaper parseEntry(Element entry) {
        ArxivPaper p = new ArxivPaper();

        p.id      = text(entry, ATOM_NS, "id");
        p.title   = normaliseWhitespace(text(entry, ATOM_NS, "title"));
        p.abstractText = normaliseWhitespace(text(entry, ATOM_NS, "summary"));

        String published = text(entry, ATOM_NS, "published");
        String updated   = text(entry, ATOM_NS, "updated");
        if (published != null) try { p.published = Instant.parse(published); } catch (Exception ignored) {}
        if (updated   != null) try { p.updated   = Instant.parse(updated);   } catch (Exception ignored) {}

        // Short arXiv id from the full URL
        if (p.id != null) {
            p.arxivId = p.id.replaceAll(".*abs/", "").replaceAll("v\\d+$", "");
        }

        // Authors
        p.authors = new ArrayList<>();
        NodeList authorNodes = entry.getElementsByTagNameNS(ATOM_NS, "author");
        for (int i = 0; i < authorNodes.getLength(); i++) {
            Element author = (Element) authorNodes.item(i);
            String name = text(author, ATOM_NS, "name");
            if (name != null) p.authors.add(name.trim());
        }

        // Links
        NodeList links = entry.getElementsByTagNameNS(ATOM_NS, "link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String rel   = link.getAttribute("rel");
            String type  = link.getAttribute("type");
            String href  = link.getAttribute("href");
            if ("related".equals(rel) && "application/pdf".equals(type)) {
                p.pdfUrl = href;
            } else if ("alternate".equals(rel)) {
                p.htmlUrl = href;
            }
        }

        // Categories
        p.categories = new ArrayList<>();
        NodeList cats = entry.getElementsByTagNameNS(ARXIV_NS, "primary_category");
        if (cats.getLength() > 0) {
            p.primaryCategory = ((Element) cats.item(0)).getAttribute("term");
        }
        NodeList catNodes = entry.getElementsByTagNameNS(ATOM_NS, "category");
        for (int i = 0; i < catNodes.getLength(); i++) {
            String term = ((Element) catNodes.item(i)).getAttribute("term");
            if (term != null && !term.isBlank()) p.categories.add(term);
        }

        return p;
    }

    // ── utilities ─────────────────────────────────────────────────────────────

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "PaperRecommender/0.1")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("arXiv API returned " + response.statusCode()
                    + " for " + url);
        }
        return response.body();
    }

    private static String text(Element parent, String ns, String tag) {
        NodeList nodes = parent.getElementsByTagNameNS(ns, tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private static String normaliseWhitespace(String s) {
        return (s == null) ? null : s.replaceAll("\\s+", " ").trim();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
