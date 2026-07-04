package com.recommender.db.entity;

import com.recommender.model.Interaction.ActionType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted user–paper interaction.
 * ActionType is stored as a VARCHAR string (not ordinal) for readability.
 */
@Entity
@Table(name = "interactions", indexes = {
        @Index(name = "idx_interactions_user",  columnList = "user_id"),
        @Index(name = "idx_interactions_paper", columnList = "paper_id"),
        @Index(name = "idx_interactions_user_paper", columnList = "user_id,paper_id")
})
public class InteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "paper_id", nullable = false)
    private UUID paperId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "weight", nullable = false)
    private float weight;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public InteractionEntity() {}

    public InteractionEntity(UUID userId, UUID paperId, ActionType actionType, float weight) {
        this.userId     = userId;
        this.paperId    = paperId;
        this.actionType = actionType;
        this.weight     = weight;
    }

    public UUID       getId()                          { return id; }
    public void       setId(UUID id)                   { this.id = id; }

    public UUID       getUserId()                      { return userId; }
    public void       setUserId(UUID userId)           { this.userId = userId; }

    public UUID       getPaperId()                     { return paperId; }
    public void       setPaperId(UUID paperId)         { this.paperId = paperId; }

    public ActionType getActionType()                  { return actionType; }
    public void       setActionType(ActionType v)      { this.actionType = v; }

    public float      getWeight()                      { return weight; }
    public void       setWeight(float weight)          { this.weight = weight; }

    public Instant    getCreatedAt()                   { return createdAt; }
}
