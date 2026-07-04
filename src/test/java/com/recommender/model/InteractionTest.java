package com.recommender.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InteractionTest {

    @Test
    void defaultWeightsAreOrdered() {
        // cite > download > save > click > view
        assertTrue(Interaction.defaultWeight(Interaction.ActionType.CITE)
                 > Interaction.defaultWeight(Interaction.ActionType.DOWNLOAD));
        assertTrue(Interaction.defaultWeight(Interaction.ActionType.DOWNLOAD)
                 > Interaction.defaultWeight(Interaction.ActionType.SAVE));
        assertTrue(Interaction.defaultWeight(Interaction.ActionType.SAVE)
                 > Interaction.defaultWeight(Interaction.ActionType.CLICK));
        assertTrue(Interaction.defaultWeight(Interaction.ActionType.CLICK)
                 > Interaction.defaultWeight(Interaction.ActionType.VIEW));
    }

    @Test
    void constructorSetsWeightFromAction() {
        Interaction i = new Interaction("u1", "p1", Interaction.ActionType.SAVE);
        assertEquals(Interaction.defaultWeight(Interaction.ActionType.SAVE), i.getWeight(), 1e-6f);
    }

    @Test
    void constructorSetsTimestamp() {
        Interaction i = new Interaction("u1", "p1", Interaction.ActionType.VIEW);
        assertNotNull(i.getTimestamp());
    }

    @Test
    void allWeightsPositive() {
        for (Interaction.ActionType type : Interaction.ActionType.values()) {
            assertTrue(Interaction.defaultWeight(type) > 0,
                    type + " weight should be positive");
        }
    }
}
