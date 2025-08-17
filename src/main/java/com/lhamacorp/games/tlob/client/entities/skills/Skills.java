package com.lhamacorp.games.tlob.client.entities.skills;

/**
 * Defines the default skills available to different entity types.
 * Each skill has base damage that gets multiplied by the entity's damage multiplier.
 */
public final class Skills {
    
    // Player skills
    public static final Skill SLASH = new Skill(
        "Slash",
        "A powerful melee attack with the sword",
        1.0,  // Base damage (will be multiplied by player's damage multiplier)
        30,    // Cooldown ticks
        3,     // Duration ticks
        40,    // Range in pixels
        false, // Not ranged
        "slash-hit.wav"
    );
    
    // Soldier skills
    public static final Skill TACKLE = new Skill(
        "Tackle",
        "A charging melee attack that knocks back enemies",
        1.5,  // Base damage
        90,    // Cooldown ticks
        4,     // Duration ticks
        35,    // Range in pixels
        false, // Not ranged
        "slash-hit.wav"
    );
    
    // Archer skills
    public static final Skill ARROW_ATTACK = new Skill(
        "Arrow Attack",
        "A precise ranged attack with an arrow",
        0.8,  // Base damage
        90,    // Cooldown ticks
        8,     // Duration ticks (arrow travel time)
        120,   // Range in pixels
        true,  // Ranged attack
        "arrow-hit.wav"
    );
    
    // Golen skills (if needed)
    public static final Skill STOMP = new Skill(
        "Stomp",
        "A powerful area attack that damages nearby enemies",
        2.0,  // Base damage
        90,    // Cooldown ticks
        6,     // Duration ticks
        50,    // Range in pixels
        false, // Not ranged
        "slash-hit.wav"
    );
    
    // Prevent instantiation
    private Skills() {}
}
