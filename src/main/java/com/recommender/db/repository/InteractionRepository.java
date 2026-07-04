package com.recommender.db.repository;

import com.recommender.db.entity.InteractionEntity;
import com.recommender.model.Interaction.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<InteractionEntity, UUID> {

    List<InteractionEntity> findByUserId(UUID userId);

    List<InteractionEntity> findByPaperId(UUID paperId);

    List<InteractionEntity> findByUserIdAndPaperId(UUID userId, UUID paperId);

    List<InteractionEntity> findByUserIdAndActionType(UUID userId, ActionType actionType);

    /**
     * Returns (userId, paperId, sumWeight) for all users — used to build the
     * implicit feedback matrix for ALS / BPR training.
     */
    @Query("""
            SELECT i.userId, i.paperId, SUM(i.weight)
            FROM InteractionEntity i
            GROUP BY i.userId, i.paperId
            """)
    List<Object[]> aggregateWeightsByUserAndPaper();

    /** Paper IDs already interacted with by a user (used as exclusion set in BPR). */
    @Query("SELECT DISTINCT i.paperId FROM InteractionEntity i WHERE i.userId = :userId")
    List<UUID> findPaperIdsByUserId(UUID userId);
}
