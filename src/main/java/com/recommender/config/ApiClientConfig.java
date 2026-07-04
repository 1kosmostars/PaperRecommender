package com.recommender.config;

import com.recommender.api.arxiv.ArxivClient;
import com.recommender.api.semanticscholar.SemanticScholarClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiClientConfig {

    @Value("${semanticscholar.api-key:}")
    private String s2ApiKey;

    @Bean
    public SemanticScholarClient semanticScholarClient() {
        return new SemanticScholarClient(s2ApiKey.isBlank() ? null : s2ApiKey);
    }

    @Bean
    public ArxivClient arxivClient() {
        return new ArxivClient();
    }
}
