package com.recommender.service;

import com.recommender.db.entity.InteractionEntity;
import com.recommender.db.repository.InteractionRepository;
import com.recommender.model.Interaction;
import com.recommender.model.Interaction.ActionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class InteractionService {

    private final InteractionRepository interactionRepo;

    public InteractionService(InteractionRepository interactionRepo) {
        this.interactionRepo = interactionRepo;
    }

    // ── writes ────────────────────────────────────────────────────────────────

    @Transactional
    public InteractionEntity record(UUID userId, UUID paperId, ActionType actionType) {
        float weight = Interaction.defaultWeight(actionType);
        return interactionRepo.save(new InteractionEntity(userId, paperId, actionType, weight));
    }

    // ── reads ─────────────────────────────────────────────────────────────────

    public List<InteractionEntity> findByUser(UUID userId) {
        return interactionRepo.findByUserId(userId);
    }

    public Set<UUID> getSeenPaperIds(UUID userId) {
        return new HashSet<>(interactionRepo.findPaperIdsByUserId(userId));
    }

    /**
     * Build the aggregated implicit feedback matrix for ALS training.
     * Returns: userIdx → (paperIdx → sumWeight).
     * Integer indices come from the caller-supplied maps.
     */
    public Map<Integer, Map<Integer, Float>> buildAlsMatrix(
            Map<UUID, Integer> userToIdx,
            Map<UUID, Integer> paperToIdx) {

        List<Object[]> rows = interactionRepo.aggregateWeightsByUserAndPaper();
        Map<Integer, Map<Integer, Float>> matrix = new HashMap<>();

        for (Object[] row : rows) {
            UUID uid  = (UUID) row[0];
            UUID pid  = (UUID) row[1];
            double w  = ((Number) row[2]).doubleValue();
            Integer ui = userToIdx.get(uid);
            Integer pi = paperToIdx.get(pid);
            if (ui != null && pi != null) {
                matrix.computeIfAbsent(ui, k -> new HashMap<>()).put(pi, (float) w);
            }
        }
        return matrix;
    }

    /**
     * Build positive-item sets for BPR training.
     * Returns: userIdx → set of paperIdx values with at least one interaction.
     */
    public Map<Integer, Set<Integer>> buildBprPositiveItems(
            Map<UUID, Integer> userToIdx,
            Map<UUID, Integer> paperToIdx) {

        List<Object[]> rows = interactionRepo.aggregateWeightsByUserAndPaper();
        Map<Integer, Set<Integer>> positive = new HashMap<>();

        for (Object[] row : rows) {
            UUID uid  = (UUID) row[0];
            UUID pid  = (UUID) row[1];
            Integer ui = userToIdx.get(uid);
            Integer pi = paperToIdx.get(pid);
            if (ui != null && pi != null) {
                positive.computeIfAbsent(ui, k -> new HashSet<>()).add(pi);
            }
        }
        return positive;
    }
}
