package com.recommender.model;

import java.time.Instant;

/**
 * Records a single user–paper interaction.
 * Weights are used as implicit feedback signals for collaborative filtering.
 */
public class Interaction {

    public enum ActionType {
        VIEW,      // user opened the abstract
        CLICK,     // user clicked through
        SAVE,      // user bookmarked
        DOWNLOAD,  // user downloaded PDF
        CITE       // user cited the paper
    }

    private String userId;
    private String paperId;
    private ActionType actionType;
    private float weight;
    private Instant timestamp;

    public Interaction() {}

    public Interaction(String userId, String paperId, ActionType actionType) {
        this.userId = userId;
        this.paperId = paperId;
        this.actionType = actionType;
        this.weight = defaultWeight(actionType);
        this.timestamp = Instant.now();
    }

    /** Default implicit-feedback weight for each action type. */
    public static float defaultWeight(ActionType action) {
        return switch (action) {
            case VIEW     -> 0.2f;
            case CLICK    -> 0.5f;
            case SAVE     -> 1.0f;
            case DOWNLOAD -> 1.5f;
            case CITE     -> 3.0f;
        };
    }

    public String     getUserId()                         { return userId; }
    public void       setUserId(String userId)            { this.userId = userId; }

    public String     getPaperId()                        { return paperId; }
    public void       setPaperId(String paperId)          { this.paperId = paperId; }

    public ActionType getActionType()                              { return actionType; }
    public void       setActionType(ActionType actionType)         { this.actionType = actionType; }

    public float      getWeight()                         { return weight; }
    public void       setWeight(float weight)             { this.weight = weight; }

    public Instant    getTimestamp()                          { return timestamp; }
    public void       setTimestamp(Instant timestamp)         { this.timestamp = timestamp; }
}
