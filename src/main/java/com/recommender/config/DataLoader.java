package com.recommender.config;

import com.recommender.db.repository.PaperRepository;
import com.recommender.service.IngestService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Seeds the database with papers on first run (when the papers table is empty).
 * Runs before IndexWarmUpRunner (@Order(1) vs @Order(2)).
 *
 * Fetches real papers from Semantic Scholar including SPECTER2 embeddings.
 * If S2 is unavailable the loader skips gracefully — the app still starts.
 */
@Component
@Order(1)
public class DataLoader implements ApplicationRunner {

    private static final Logger log = Logger.getLogger(DataLoader.class.getName());

    private static final int PAPERS_PER_TOPIC = 8;

    private static final String[] SEED_TOPICS = {
        "attention transformer neural network",
        "graph neural network link prediction",
        "BERT language model pretraining",
        "collaborative filtering recommendation system",
        "contrastive learning self-supervised representation"
    };

    private final PaperRepository paperRepo;
    private final IngestService   ingestService;

    public DataLoader(PaperRepository paperRepo, IngestService ingestService) {
        this.paperRepo    = paperRepo;
        this.ingestService = ingestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        long count = paperRepo.count();
        if (count >= 10) {
            log.info("[DataLoader] DB already has " + count + " papers — skipping seed.");
            return;
        }

        log.info("[DataLoader] Seeding database from Semantic Scholar…");
        int totalSaved = 0;

        for (String topic : SEED_TOPICS) {
            try {
                var result = ingestService.ingestFromSemanticScholar(topic, PAPERS_PER_TOPIC);
                totalSaved += result.saved();
                log.info("[DataLoader] Topic '" + topic + "': saved=" + result.saved()
                        + " skipped=" + result.skipped()
                        + (result.errors().isEmpty() ? "" : " errors=" + result.errors()));

                // S2 rate limit: 100 req/s unauthenticated — small pause between topics
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warning("[DataLoader] Failed for topic '" + topic + "': " + e.getMessage());
            }
        }

        log.info("[DataLoader] Seed complete — total papers saved: " + totalSaved);
    }
}
