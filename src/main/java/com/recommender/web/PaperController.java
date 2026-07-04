package com.recommender.web;

import com.recommender.db.entity.PaperEntity;
import com.recommender.model.Paper;
import com.recommender.service.PaperService;
import com.recommender.web.dto.PaperDto;
import com.recommender.web.dto.PaperRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    /** Save (or upsert by s2Id) a paper. Returns the persisted entity. */
    @PostMapping
    public ResponseEntity<PaperDto> create(@RequestBody PaperRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Paper paper = toPaper(req);
        PaperEntity saved = paperService.save(paper);
        return ResponseEntity
                .created(URI.create("/api/papers/" + saved.getId()))
                .body(PaperDto.from(saved));
    }

    /** Batch save. Returns the list of persisted papers. */
    @PostMapping("/batch")
    public ResponseEntity<List<PaperDto>> createBatch(@RequestBody List<PaperRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) return ResponseEntity.badRequest().build();
        List<Paper> papers = reqs.stream().map(this::toPaper).toList();
        List<PaperDto> result = paperService.saveAll(papers).stream()
                .map(PaperDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    /** Get paper by internal UUID. */
    @GetMapping("/{id}")
    public ResponseEntity<PaperDto> getById(@PathVariable UUID id) {
        return paperService.findEntityById(id)
                .map(e -> ResponseEntity.ok(PaperDto.from(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get paper by Semantic Scholar ID. */
    @GetMapping("/s2/{s2Id}")
    public ResponseEntity<PaperDto> getByS2Id(@PathVariable String s2Id) {
        return paperService.findEntityByS2Id(s2Id)
                .map(e -> ResponseEntity.ok(PaperDto.from(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get paper by arXiv ID. */
    @GetMapping("/arxiv/{arxivId}")
    public ResponseEntity<PaperDto> getByArxivId(@PathVariable String arxivId) {
        return paperService.findEntityByArxivId(arxivId)
                .map(e -> ResponseEntity.ok(PaperDto.from(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Add a citation edge: citingId cites citedId. */
    @PostMapping("/{citingId}/citations/{citedId}")
    public ResponseEntity<Void> addCitation(@PathVariable UUID citingId,
                                            @PathVariable UUID citedId) {
        paperService.addCitation(citingId, citedId);
        return ResponseEntity.noContent().build();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private Paper toPaper(PaperRequest req) {
        Paper p = new Paper(req.s2Id(), req.title(), req.abstractText(),
                req.year() != null ? req.year() : 0);
        p.setArxivId(req.arxivId());
        p.setDoi(req.doi());
        p.setTopics(req.topics());
        p.setAuthorNames(req.authorNames());
        p.setCitationCount(req.citationCount());
        p.setReferenceCount(req.referenceCount());
        p.setEmbedding(req.embedding());
        return p;
    }
}
