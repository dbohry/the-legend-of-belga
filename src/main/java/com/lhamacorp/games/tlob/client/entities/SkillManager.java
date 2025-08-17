package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.entities.skills.Skill;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages skills for an entity, handling cooldowns and skill usage.
 */
public class SkillManager {
    private final Map<Skill, Integer> skillCooldowns;
    private final Map<Skill, Integer> skillDurations;
    
    public SkillManager() {
        this.skillCooldowns = new HashMap<>();
        this.skillDurations = new HashMap<>();
    }
    
    /**
     * Checks if a skill is ready to use (no cooldown active).
     * 
     * @param skill The skill to check
     * @return true if the skill can be used, false otherwise
     */
    public boolean isSkillReady(Skill skill) {
        Integer cooldown = skillCooldowns.get(skill);
        return cooldown == null || cooldown <= 0;
    }
    
    /**
     * Checks if a skill is currently active (duration > 0).
     * 
     * @param skill The skill to check
     * @return true if the skill is active, false otherwise
     */
    public boolean isSkillActive(Skill skill) {
        Integer duration = skillDurations.get(skill);
        return duration != null && duration > 0;
    }
    
    /**
     * Uses a skill, setting its cooldown and duration.
     * 
     * @param skill The skill to use
     * @param entity The entity using the skill (for calculating effective values)
     */
    public void useSkill(Skill skill, Entity entity) {
        skillCooldowns.put(skill, skill.getEffectiveCooldown(entity));
        skillDurations.put(skill, skill.getEffectiveDuration(entity));
    }
    
    /**
     * Uses a skill, setting its cooldown and duration.
     * This method is kept for backward compatibility but should be avoided.
     * 
     * @param skill The skill to use
     * @deprecated Use useSkill(Skill skill, Entity entity) instead
     */
    @Deprecated
    public void useSkill(Skill skill) {
        // Use default values without entity context
        skillCooldowns.put(skill, skill.getCooldownTicks());
        skillDurations.put(skill, skill.getDurationTicks());
    }
    
    /**
     * Updates all skill timers. Should be called every tick.
     */
    public void update() {
        // Update cooldowns
        skillCooldowns.replaceAll((skill, cooldown) -> {
            if (cooldown > 0) {
                return cooldown - 1;
            }
            return 0;
        });
        
        // Update durations
        skillDurations.replaceAll((skill, duration) -> {
            if (duration > 0) {
                return duration - 1;
            }
            return 0;
        });
    }
    
    /**
     * Gets the remaining cooldown for a skill.
     * 
     * @param skill The skill to check
     * @return The remaining cooldown in ticks, or 0 if no cooldown
     */
    public int getRemainingCooldown(Skill skill) {
        Integer cooldown = skillCooldowns.get(skill);
        return cooldown != null ? cooldown : 0;
    }
    
    /**
     * Gets the remaining duration for a skill.
     * 
     * @param skill The skill to check
     * @return The remaining duration in ticks, or 0 if not active
     */
    public int getRemainingDuration(Skill skill) {
        Integer duration = skillDurations.get(skill);
        return duration != null ? duration : 0;
    }
    
    /**
     * Gets the cooldown progress as a percentage (0.0 to 1.0).
     * 
     * @param skill The skill to check
     * @return The cooldown progress, where 0.0 means ready and 1.0 means just used
     */
    public double getCooldownProgress(Skill skill) {
        int remaining = getRemainingCooldown(skill);
        if (remaining <= 0) return 0.0;
        return (double) remaining / skill.getCooldownTicks();
    }
    
    /**
     * Gets the duration progress as a percentage (0.0 to 1.0).
     * 
     * @param skill The skill to check
     * @return The duration progress, where 0.0 means not active and 1.0 means just started
     */
    public double getDurationProgress(Skill skill) {
        int remaining = getRemainingDuration(skill);
        if (remaining <= 0) return 0.0;
        return (double) remaining / skill.getDurationTicks();
    }
    
    /**
     * Resets all cooldowns and durations.
     */
    public void resetAll() {
        skillCooldowns.clear();
        skillDurations.clear();
    }
    
    /**
     * Gets the number of skills currently on cooldown.
     * 
     * @return The number of skills with active cooldowns
     */
    public int getSkillsOnCooldown() {
        return (int) skillCooldowns.values().stream().filter(cooldown -> cooldown > 0).count();
    }
    
    /**
     * Gets the number of skills currently active.
     * 
     * @return The number of skills with active durations
     */
    public int getActiveSkills() {
        return (int) skillDurations.values().stream().filter(duration -> duration > 0).count();
    }
    
    /**
     * Gets the cooldown progress as a percentage (0.0 to 1.0) using effective values.
     * @param skill The skill to check
     * @param entity The entity using the skill
     * @return The cooldown progress, where 0.0 means ready and 1.0 means just used
     */
    public double getEffectiveCooldownProgress(Skill skill, Entity entity) {
        int remaining = getRemainingCooldown(skill);
        int total = skill.getEffectiveCooldown(entity);
        return total > 0 ? (double) remaining / total : 0.0;
    }
    
    /**
     * Gets the duration progress as a percentage (0.0 to 1.0) using effective values.
     * @param skill The skill to check
     * @param entity The entity using the skill
     * @return The duration progress, where 0.0 means not active and 1.0 means just started
     */
    public double getEffectiveDurationProgress(Skill skill, Entity entity) {
        int remaining = getRemainingDuration(skill);
        int total = skill.getEffectiveDuration(entity);
        return total > 0 ? (double) remaining / total : 0.0;
    }
}
