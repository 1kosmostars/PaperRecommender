package com.recommender.api.openalex;

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
 * Client for the OpenAlex API.
 *
 * <p>OpenAlex provides topics hierarchy, concept taxonomy, and rich bibliographic metadata.
 * Documentation: <a href="https://docs.openalex.org">docs.openalex.org</a>
 *
 * <p>Polite pool: add {@code email} to enable faster responses (no hard rate limit).
 *
 * <p>Usage:
 * <pre>{@code
 *   OpenAlexClient client = new OpenAlexClient("you@example.com");
 *   List<OAWork> works = client.searchWorks("transformer neural network", 25);
 *   OAWork work = client.getWorkByDoi("10.18653/v1/2020.acl-main.463");
 * }</pre>
 */
public class OpenAlexClient {

    private static final String BASE_URL = "https://api.openalex.org";

    /** Fields requested for works. */
    private static final String WORK_SELECT =
            "id,doi,title,publication_year,cited_by_count,concepts,open_access," +
            "authorships,primary_location,topics";

    private final HttpClient http;
    private final ObjectMapper mapper;
    /** Polite pool email – appended as mailto= query param. May be null. */
    private final String email;

    public OpenAlexClient() {
        this(null);
    }

    public OpenAlexClient(String email) {
        this.email = email;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Full-text search over works (papers).
     *
     * @param query  search query string
     * @param perPage results per page (max 200)
     */
    public List<OAWork> searchWorks(String query, int perPage) throws IOException, InterruptedException {
        String url = BASE_URL + "/works"
                + "?search=" + encode(query)
                + "&select=" + encode(WORK_SELECT)
                + "&per-page=" + perPage
                + politeParam();

        String body = get(url);
        WorksResponse response = mapper.readValue(body, WorksResponse.class);
        return response.results != null ? response.results : Collections.emptyList();
    }

    /**
     * Get a single work by its OpenAlex id (e.g. "W2741809807") or DOI.
     *
     * @param id OpenAlex id or "doi:10.xxxx/..."
     */
    public OAWork getWork(String id) throws IOException, InterruptedException {
        String url = BASE_URL + "/works/" + encode(id)
                + "?select=" + encode(WORK_SELECT)
                + politeParam();
        String body = get(url);
        return mapper.readValue(body, OAWork.class);
    }

    /**
     * Get a work by DOI.
     */
    public OAWork getWorkByDoi(String doi) throws IOException, InterruptedException {
        return getWork("doi:" + doi);
    }

    /**
     * Retrieve top-level concepts (topics hierarchy).
     *
     * @param perPage number of concepts per page
     */
    public List<OAConcept> getConcepts(int perPage) throws IOException, InterruptedException {
        String url = BASE_URL + "/concepts"
                + "?select=id,display_name,level,description,works_count"
                + "&per-page=" + perPage
                + "&sort=works_count:desc"
                + politeParam();

        String body = get(url);
        ConceptsResponse response = mapper.readValue(body, ConceptsResponse.class);
        return response.results != null ? response.results : Collections.emptyList();
    }

    /**
     * Find works for a given concept id (e.g. "C41008148" for Computer Science).
     */
    public List<OAWork> getWorksByConcept(String conceptId, int perPage) throws IOException, InterruptedException {
        String url = BASE_URL + "/works"
                + "?filter=concepts.id:" + conceptId
                + "&select=" + encode(WORK_SELECT)
                + "&per-page=" + perPage
                + "&sort=cited_by_count:desc"
                + politeParam();

        String body = get(url);
        WorksResponse response = mapper.readValue(body, WorksResponse.class);
        return response.results != null ? response.results : Collections.emptyList();
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

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
            throw new IOException("OpenAlex API returned " + response.statusCode()
                    + " for " + url + ": " + response.body());
        }
        return response.body();
    }

    private String politeParam() {
        return (email != null && !email.isBlank()) ? "&mailto=" + encode(email) : "";
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAWork {
        @JsonProperty("id")               public String id;
        @JsonProperty("doi")              public String doi;
        @JsonProperty("title")            public String title;
        @JsonProperty("publication_year") public Integer publicationYear;
        @JsonProperty("cited_by_count")   public Integer citedByCount;
        @JsonProperty("concepts")         public List<OAConcept> concepts;
        @JsonProperty("topics")           public List<OATopic> topics;
        @JsonProperty("open_access")      public OAOpenAccess openAccess;
        @JsonProperty("authorships")      public List<OAAuthorship> authorships;
        @JsonProperty("primary_location") public OALocation primaryLocation;

        /** Returns the OpenAlex short id, e.g. "W2741809807". */
        public String shortId() {
            return id != null ? id.replace("https://openalex.org/", "") : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAConcept {
        @JsonProperty("id")           public String id;
        @JsonProperty("display_name") public String displayName;
        @JsonProperty("level")        public Integer level;
        @JsonProperty("score")        public Double score;          // relevance to this work
        @JsonProperty("works_count")  public Integer worksCount;
        @JsonProperty("description")  public String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OATopic {
        @JsonProperty("id")           public String id;
        @JsonProperty("display_name") public String displayName;
        @JsonProperty("score")        public Double score;
        @JsonProperty("subfield")     public OAConceptRef subfield;
        @JsonProperty("field")        public OAConceptRef field;
        @JsonProperty("domain")       public OAConceptRef domain;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAConceptRef {
        @JsonProperty("id")           public String id;
        @JsonProperty("display_name") public String displayName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAOpenAccess {
        @JsonProperty("is_oa")  public Boolean isOa;
        @JsonProperty("oa_url") public String oaUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAAuthorship {
        @JsonProperty("author") public OAAuthor author;
        @JsonProperty("institutions") public List<OAInstitution> institutions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAAuthor {
        @JsonProperty("id")           public String id;
        @JsonProperty("display_name") public String displayName;
        @JsonProperty("orcid")        public String orcid;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAInstitution {
        @JsonProperty("id")           public String id;
        @JsonProperty("display_name") public String displayName;
        @JsonProperty("country_code") public String countryCode;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OALocation {
        @JsonProperty("source") public OASource source;
        @JsonProperty("pdf_url") public String pdfUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OASource {
        @JsonProperty("id")           public String id;
        @JsonProperty("display_name") public String displayName;
        @JsonProperty("issn_l")       public String issnL;
        @JsonProperty("type")         public String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WorksResponse {
        @JsonProperty("results") public List<OAWork> results;
        @JsonProperty("meta")    public Meta meta;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ConceptsResponse {
        @JsonProperty("results") public List<OAConcept> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Meta {
        @JsonProperty("count")    public int count;
        @JsonProperty("page")     public int page;
        @JsonProperty("per_page") public int perPage;
    }
}
