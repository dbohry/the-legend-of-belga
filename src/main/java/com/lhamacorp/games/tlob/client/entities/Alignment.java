package com.lhamacorp.games.tlob.client.entities;

/**
 * Represents the alignment of an entity in the game world.
 * This determines how the entity interacts with other entities.
 */
public enum Alignment {
    /** Hostile entity that attacks the player */
    FOE,
    
    /** Friendly entity that helps the player */
    ALLY,
    
    /** Neutral entity that doesn't take sides */
    NEUTRAL
}
