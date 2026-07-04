package com.recommender.config;

import com.recommender.service.IndexService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Triggers index warm-up after the full Spring context is ready.
 * Using ApplicationRunner ensures @Transactional works correctly in IndexService
 * (unlike @PostConstruct, which is called before the proxy chain is active).
 */
@Component
@Order(2)
public class IndexWarmUpRunner implements ApplicationRunner {

    private final IndexService indexService;

    public IndexWarmUpRunner(IndexService indexService) {
        this.indexService = indexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        indexService.reindex();
    }
}
