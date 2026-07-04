package com.recommender.service;

import com.recommender.db.entity.PaperEntity;
import com.recommender.db.repository.PaperRepository;
import com.recommender.model.Paper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class PaperService {

    private final PaperRepository paperRepo;

    public PaperService(PaperRepository paperRepo) {
        this.paperRepo = paperRepo;
    }

    // ── writes ────────────────────────────────────────────────────────────────

    @Transactional
    public PaperEntity save(Paper paper) {
        // Prefer upsert by s2Id when available
        PaperEntity entity = paper.getS2Id() != null
                ? paperRepo.findByS2Id(paper.getS2Id()).orElse(new PaperEntity())
                : new PaperEntity();
        fillEntity(entity, paper);
        return paperRepo.save(entity);
    }

    @Transactional
    public List<PaperEntity> saveAll(List<Paper> papers) {
        List<PaperEntity> entities = new ArrayList<>(papers.size());
        for (Paper p : papers) entities.add(save(p));
        return entities;
    }

    /** Attach the citation graph edge: citing → cited (both must already be persisted). */
    @Transactional
    public void addCitation(UUID citingId, UUID citedId) {
        PaperEntity citing = paperRepo.findById(citingId)
                .orElseThrow(() -> new NoSuchElementException("Paper not found: " + citingId));
        citing.getCitedPaperIds().add(citedId);
        paperRepo.save(citing);
    }

    // ── reads ─────────────────────────────────────────────────────────────────

    public Optional<Paper> findById(UUID id) {
        return paperRepo.findById(id).map(this::toModel);
    }

    public Optional<Paper> findByS2Id(String s2Id) {
        return paperRepo.findByS2Id(s2Id).map(this::toModel);
    }

    public Optional<Paper> findByArxivId(String arxivId) {
        return paperRepo.findByArxivId(arxivId).map(this::toModel);
    }

    public Optional<PaperEntity> findEntityById(UUID id) {
        return paperRepo.findById(id);
    }

    public Optional<PaperEntity> findEntityByS2Id(String s2Id) {
        return paperRepo.findByS2Id(s2Id);
    }

    public Optional<PaperEntity> findEntityByArxivId(String arxivId) {
        return paperRepo.findByArxivId(arxivId);
    }

    public List<PaperEntity> findAllEntities() {
        return paperRepo.findAll();
    }

    public List<PaperEntity> findAllWithEmbedding() {
        return paperRepo.findAllWithEmbedding();
    }

    public List<PaperEntity> findByIds(Collection<UUID> ids) {
        return paperRepo.findAllById(ids);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    public Paper toModel(PaperEntity e) {
        Paper p = new Paper(e.getS2Id(), e.getTitle(), e.getAbstractText(),
                e.getYear() != null ? e.getYear() : 0);
        p.setId(e.getId().toString());
        p.setArxivId(e.getArxivId());
        p.setDoi(e.getDoi());
        p.setTopics(new ArrayList<>(e.getTopics()));
        p.setEmbedding(e.getEmbedding());
        p.setCitationCount(e.getCitationCount());
        p.setReferenceCount(e.getReferenceCount());
        p.setAuthorNames(new ArrayList<>(e.getAuthorNames()));
        return p;
    }

    private void fillEntity(PaperEntity e, Paper p) {
        e.setS2Id(p.getS2Id());
        e.setArxivId(p.getArxivId());
        e.setDoi(p.getDoi());
        e.setTitle(p.getTitle());
        e.setAbstractText(p.getAbstractText());
        e.setYear(p.getYear() > 0 ? p.getYear() : null);
        e.setEmbedding(p.getEmbedding());
        e.setCitationCount(p.getCitationCount());
        e.setReferenceCount(p.getReferenceCount());
        if (p.getTopics() != null)     e.setTopics(new ArrayList<>(p.getTopics()));
        if (p.getAuthorNames() != null) e.setAuthorNames(new ArrayList<>(p.getAuthorNames()));
    }
}
