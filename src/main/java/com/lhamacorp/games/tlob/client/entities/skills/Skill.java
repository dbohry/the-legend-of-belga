package com.lhamacorp.games.tlob.client.entities.skills;

import com.lhamacorp.games.tlob.client.entities.Entity;

/**
 * Represents a skill that an entity can use.
 * Skills have a base damage that gets multiplied by the entity's damage multiplier.
 */
public class Skill {
    private final String name;
    private final String description;
    private final double baseDamage;
    private final int cooldownTicks;
    private final int durationTicks;
    private final double range;
    private final boolean isRanged;
    private final String soundEffect;
    
    /**
     * Creates a new skill.
     * 
     * @param name The name of the skill
     * @param description Description of what the skill does
     * @param baseDamage Base damage of the skill (will be multiplied by entity's damage multiplier)
     * @param cooldownTicks Cooldown in ticks before the skill can be used again
     * @param durationTicks Duration in ticks for how long the skill effect lasts
     * @param range Range of the skill in pixels
     * @param isRanged Whether this is a ranged skill
     * @param soundEffect Sound effect to play when using the skill
     */
    public Skill(String name, String description, double baseDamage, int cooldownTicks, 
                 int durationTicks, double range, boolean isRanged, String soundEffect) {
        this.name = name;
        this.description = description;
        this.baseDamage = baseDamage;
        this.cooldownTicks = cooldownTicks;
        this.durationTicks = durationTicks;
        this.range = range;
        this.isRanged = isRanged;
        this.soundEffect = soundEffect;
    }
    
    /**
     * Creates a melee skill with default sound effect.
     */
    public Skill(String name, String description, double baseDamage, int cooldownTicks, 
                 int durationTicks, double range) {
        this(name, description, baseDamage, cooldownTicks, durationTicks, range, false, "slash-hit.wav");
    }
    
    /**
     * Gets the name of the skill.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the skill.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the base damage of the skill.
     */
    public double getBaseDamage() {
        return baseDamage;
    }
    
    /**
     * Gets the cooldown in ticks.
     */
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    /**
     * Gets the duration in ticks.
     */
    public int getDurationTicks() {
        return durationTicks;
    }
    
    /**
     * Gets the range of the skill.
     */
    public double getRange() {
        return range;
    }
    
    /**
     * Checks if this is a ranged skill.
     */
    public boolean isRanged() {
        return isRanged;
    }
    
    /**
     * Gets the sound effect to play.
     */
    public String getSoundEffect() {
        return soundEffect;
    }
    
    /**
     * Calculates the effective damage for this skill when used by the given entity.
     * 
     * @param entity The entity using the skill
     * @return The effective damage (base damage * entity's damage multiplier)
     */
    public double getEffectiveDamage(Entity entity) {
        return baseDamage * entity.getDamageMultiplier();
    }
    
    /**
     * Gets the total damage this skill deals when used by an entity.
     * This includes the entity's damage multiplier and base damage bonus.
     * 
     * @param entity The entity using the skill
     * @return The total damage the skill will deal
     */
    public double getTotalDamage(Entity entity) {
        return (baseDamage + entity.getBaseDamage()) * entity.getDamageMultiplier();
    }
    
    /**
     * Gets the effective cooldown for this skill when used by an entity.
     * This is modified by the entity's attack speed.
     * 
     * @param entity The entity using the skill
     * @return The effective cooldown in ticks
     */
    public int getEffectiveCooldown(Entity entity) {
        // Attack speed affects cooldown inversely (higher speed = lower cooldown)
        // Minimum cooldown is 1 tick to prevent instant attacks
        return Math.max(1, (int) Math.round(cooldownTicks / entity.getAttackSpeed()));
    }
    
    /**
     * Gets the effective duration for this skill when used by an entity.
     * This is modified by the entity's attack speed.
     * 
     * @param entity The entity using the skill
     * @return The effective duration in ticks
     */
    public int getEffectiveDuration(Entity entity) {
        // Attack speed affects duration (higher speed = shorter duration for attacks)
        // Minimum duration is 1 tick
        return Math.max(1, (int) Math.round(durationTicks / entity.getAttackSpeed()));
    }
    
    /**
     * Gets the effective range for this skill when used by an entity.
     * This can be modified by entity perks or attributes.
     * 
     * @param entity The entity using the skill
     * @return The effective range in pixels
     */
    public double getEffectiveRange(Entity entity) {
        // For now, range is not modified by entity attributes
        // This could be extended in the future with range modifiers
        return range;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%.1f damage, %d ticks cooldown)", name, baseDamage, cooldownTicks);
    }
}
