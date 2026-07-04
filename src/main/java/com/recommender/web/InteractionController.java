package com.recommender.web;

import com.recommender.db.entity.InteractionEntity;
import com.recommender.db.repository.PaperRepository;
import com.recommender.model.Interaction.ActionType;
import com.recommender.service.InteractionService;
import com.recommender.web.dto.InteractionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;
    private final PaperRepository    paperRepo;

    public InteractionController(InteractionService interactionService,
                                 PaperRepository paperRepo) {
        this.interactionService = interactionService;
        this.paperRepo          = paperRepo;
    }

    /** Record a user–paper interaction. actionType: VIEW | CLICK | SAVE | DOWNLOAD | CITE */
    @PostMapping
    public ResponseEntity<Map<String, Object>> record(@RequestBody InteractionRequest req) {
        if (req.userId() == null || req.paperId() == null || req.actionType() == null) {
            return ResponseEntity.badRequest().build();
        }
        ActionType actionType;
        try {
            actionType = ActionType.valueOf(req.actionType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        InteractionEntity saved = interactionService.record(req.userId(), req.paperId(), actionType);
        return ResponseEntity.ok(Map.of(
                "id",         saved.getId(),
                "userId",     saved.getUserId(),
                "paperId",    saved.getPaperId(),
                "actionType", saved.getActionType().name(),
                "weight",     saved.getWeight(),
                "createdAt",  saved.getCreatedAt()
        ));
    }

    /** Get all interactions for a user, including paper titles. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getByUser(@PathVariable UUID userId) {
        List<InteractionEntity> interactions = interactionService.findByUser(userId);

        Set<UUID> paperIds = new HashSet<>();
        for (InteractionEntity ie : interactions) paperIds.add(ie.getPaperId());
        Map<UUID, String> titleMap = new HashMap<>();
        if (!paperIds.isEmpty()) {
            paperRepo.findAllById(paperIds).forEach(p -> titleMap.put(p.getId(), p.getTitle()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (InteractionEntity e : interactions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",         e.getId());
            m.put("paperId",    e.getPaperId());
            m.put("paperTitle", titleMap.getOrDefault(e.getPaperId(), ""));
            m.put("actionType", e.getActionType().name());
            m.put("weight",     e.getWeight());
            m.put("createdAt",  e.getCreatedAt());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    /** Get IDs of papers the user has already seen (for client-side exclusion). */
    @GetMapping("/user/{userId}/seen")
    public ResponseEntity<Map<String, Object>> getSeenPapers(@PathVariable UUID userId) {
        return ResponseEntity.ok(Map.of(
                "userId",   userId,
                "paperIds", interactionService.getSeenPaperIds(userId)
        ));
    }
}
