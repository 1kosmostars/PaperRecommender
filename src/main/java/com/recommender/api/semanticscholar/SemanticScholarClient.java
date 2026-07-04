package com.recommender.api.semanticscholar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Client for the Semantic Scholar Academic Graph API (v1).
 *
 * <p>Documentation: <a href="https://api.semanticscholar.org/api-docs/graph">S2 API docs</a>
 *
 * <p>Rate limits: 100 req/s (unauthenticated); set {@code apiKey} for higher limits.
 *
 * <p>Usage:
 * <pre>{@code
 *   SemanticScholarClient client = new SemanticScholarClient();
 *   List<S2Paper> results = client.searchPapers("HNSW vector search", 10);
 *   S2Paper paper = client.getPaper("204e3073870fae3d05bcbc2f6a8e263d9b72e776");
 * }</pre>
 */
public class SemanticScholarClient {

    private static final String BASE_URL = "https://api.semanticscholar.org/graph/v1";

    /**
     * Fields requested for each paper.
     * Embedding requires the SPECTER_V2 model field.
     */
    private static final String PAPER_FIELDS =
            "paperId,externalIds,title,abstract,year,citationCount,referenceCount," +
            "authors,embedding,s2FieldsOfStudy";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey; // null = unauthenticated

    public SemanticScholarClient() {
        this(null);
    }

    public SemanticScholarClient(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Full-text paper search.
     *
     * @param query search query string
     * @param limit max results (≤ 100 per request)
     * @return list of matching papers
     */
    public List<S2Paper> searchPapers(String query, int limit) throws IOException, InterruptedException {
        String url = BASE_URL + "/paper/search"
                + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&limit=" + limit
                + "&fields=" + URLEncoder.encode(PAPER_FIELDS, StandardCharsets.UTF_8);

        String body = get(url);
        SearchResponse response = mapper.readValue(body, SearchResponse.class);
        return response.data != null ? response.data : Collections.emptyList();
    }

    /**
     * Fetch a single paper by its Semantic Scholar paper id or DOI/ArXiv id.
     *
     * @param paperId Semantic Scholar id (40-char hex), or "DOI:10.1234/...", or "ARXIV:2301.07041"
     */
    public S2Paper getPaper(String paperId) throws IOException, InterruptedException {
        String url = BASE_URL + "/paper/" + URLEncoder.encode(paperId, StandardCharsets.UTF_8)
                + "?fields=" + URLEncoder.encode(PAPER_FIELDS, StandardCharsets.UTF_8);
        String body = get(url);
        return mapper.readValue(body, S2Paper.class);
    }

    /**
     * Fetch the list of papers that cite {@code paperId}.
     *
     * @param limit max results per request (≤ 1000)
     */
    public List<S2Citation> getCitations(String paperId, int limit) throws IOException, InterruptedException {
        String url = BASE_URL + "/paper/" + URLEncoder.encode(paperId, StandardCharsets.UTF_8)
                + "/citations?fields=paperId,title,year&limit=" + limit;
        String body = get(url);
        CitationResponse response = mapper.readValue(body, CitationResponse.class);
        return response.data != null ? response.data : Collections.emptyList();
    }

    /**
     * Fetch the references of {@code paperId} (papers it cites).
     *
     * @param limit max results per request (≤ 1000)
     */
    public List<S2Citation> getReferences(String paperId, int limit) throws IOException, InterruptedException {
        String url = BASE_URL + "/paper/" + URLEncoder.encode(paperId, StandardCharsets.UTF_8)
                + "/references?fields=" + URLEncoder.encode(PAPER_FIELDS, StandardCharsets.UTF_8)
                + "&limit=" + limit;
        String body = get(url);
        CitationResponse response = mapper.readValue(body, CitationResponse.class);
        return response.data != null ? response.data : Collections.emptyList();
    }

    /**
     * Batch fetch paper details for up to 500 papers in a single request.
     */
    public List<S2Paper> getPapersBatch(List<String> paperIds) throws IOException, InterruptedException {
        if (paperIds.isEmpty()) return Collections.emptyList();

        String url = BASE_URL + "/paper/batch"
                + "?fields=" + URLEncoder.encode(PAPER_FIELDS, StandardCharsets.UTF_8);

        String jsonBody = mapper.writeValueAsString(
                java.util.Map.of("ids", paperIds));

        String respBody = post(url, jsonBody);
        return mapper.readValue(respBody,
                mapper.getTypeFactory().constructCollectionType(List.class, S2Paper.class));
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();
        if (apiKey != null) builder.header("x-api-key", apiKey);

        HttpResponse<String> response = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("Semantic Scholar API returned " + response.statusCode()
                    + " for " + url + ": " + response.body());
        }
        return response.body();
    }

    private String post(String url, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        if (apiKey != null) builder.header("x-api-key", apiKey);

        HttpResponse<String> response = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("Semantic Scholar API returned " + response.statusCode()
                    + " for POST " + url + ": " + response.body());
        }
        return response.body();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S2Paper {
        @JsonProperty("paperId")    public String paperId;
        @JsonProperty("externalIds") public ExternalIds externalIds;
        @JsonProperty("title")      public String title;
        @JsonProperty("abstract")   public String abstractText;
        @JsonProperty("year")       public Integer year;
        @JsonProperty("citationCount")  public Integer citationCount;
        @JsonProperty("referenceCount") public Integer referenceCount;
        @JsonProperty("authors")    public List<S2Author> authors;
        @JsonProperty("embedding")         public S2Embedding embedding;
        @JsonProperty("s2FieldsOfStudy")   public List<S2FieldOfStudy> s2FieldsOfStudy;

        /** Returns the SPECTER2 embedding as float[], or null if not present. */
        public float[] getEmbeddingVector() {
            if (embedding == null || embedding.vector == null) return null;
            float[] v = new float[embedding.vector.size()];
            for (int i = 0; i < v.length; i++) v[i] = embedding.vector.get(i).floatValue();
            return v;
        }

        public String getArxivId() {
            return (externalIds != null) ? externalIds.arxivId : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalIds {
        @JsonProperty("ArXiv") public String arxivId;
        @JsonProperty("DOI")   public String doi;
        @JsonProperty("PubMed") public String pubmedId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S2Author {
        @JsonProperty("authorId") public String authorId;
        @JsonProperty("name")     public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S2FieldOfStudy {
        @JsonProperty("category") public String category;
        @JsonProperty("source")   public String source;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S2Embedding {
        @JsonProperty("model")  public String model;
        @JsonProperty("vector") public List<Double> vector;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S2Citation {
        /** The citing/cited paper (depending on whether from getCitations or getReferences). */
        @JsonProperty("citingPaper")  public S2Paper citingPaper;
        @JsonProperty("citedPaper")   public S2Paper citedPaper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SearchResponse {
        @JsonProperty("total")  public int total;
        @JsonProperty("offset") public int offset;
        @JsonProperty("next")   public Integer next;
        @JsonProperty("data")   public List<S2Paper> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CitationResponse {
        @JsonProperty("data") public List<S2Citation> data;
    }
}
