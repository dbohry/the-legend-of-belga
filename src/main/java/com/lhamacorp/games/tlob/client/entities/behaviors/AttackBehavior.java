package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.List;

/**
 * Behavior that handles enemy attacks.
 * Supports different attack types and ranges.
 */
public class AttackBehavior extends AbstractBehavior {
    
    public static final int PRIORITY = 100; // High priority - attack when possible
    
    private final double attackRange;
    private final double minAttackRange; // For ranged enemies
    private final double attackDamage;
    private final int attackCooldownTicks;
    private final int attackDurationTicks;
    private final boolean isRanged;
    private final boolean requiresLineOfSight;
    
    private int cooldownTimer = 0;
    private int attackTimer = 0;
    private boolean hasAttacked = false;
    
    /**
     * Creates a new attack behavior.
     * @param attackRange The maximum range at which the enemy can attack
     * @param attackDamage The damage dealt by the attack
     * @param attackCooldownTicks The cooldown between attacks in ticks
     * @param attackDurationTicks The duration of the attack animation in ticks
     * @param isRanged Whether this is a ranged attack
     * @param requiresLineOfSight Whether the attack requires line of sight to the player
     */
    public AttackBehavior(double attackRange, double attackDamage, int attackCooldownTicks, 
                         int attackDurationTicks, boolean isRanged, boolean requiresLineOfSight) {
        super("Attack", PRIORITY);
        this.attackRange = attackRange;
        this.minAttackRange = isRanged ? attackRange * 0.5 : 0; // Ranged enemies have minimum range
        this.attackDamage = attackDamage;
        this.attackCooldownTicks = attackCooldownTicks;
        this.attackDurationTicks = attackDurationTicks;
        this.isRanged = isRanged;
        this.requiresLineOfSight = requiresLineOfSight;
    }
    
    /**
     * Creates a melee attack behavior.
     * @param attackRange The maximum range at which the enemy can attack
     * @param attackDamage The damage dealt by the attack
     * @param attackCooldownTicks The cooldown between attacks in ticks
     * @param attackDurationTicks The duration of the attack animation in ticks
     */
    public AttackBehavior(double attackRange, double attackDamage, int attackCooldownTicks, int attackDurationTicks) {
        this(attackRange, attackDamage, attackCooldownTicks, attackDurationTicks, false, true);
    }
    
    @Override
    public boolean canExecute(Entity entity, Player player, TileMap map) {
        if (cooldownTimer > 0 || attackTimer > 0) {
            return false;
        }
        
        double distance = calculateDistance(entity, player);
        
        // Check if player is within attack range
        if (distance > attackRange) {
            return false;
        }
        
        // Check minimum range for ranged enemies
        if (isRanged && distance < minAttackRange) {
            return false;
        }
        
        // Check line of sight if required
        if (requiresLineOfSight && !hasLineOfSight(entity, player, map)) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean update(Entity entity, Player player, TileMap map, List<Entity> nearbyAllies, Object... args) {
        incrementActiveTicks();
        
        // Update timers
        if (cooldownTimer > 0) {
            cooldownTimer--;
        }
        
        if (attackTimer > 0) {
            attackTimer--;
            if (attackTimer == 0) {
                // Attack finished
                onStop(entity);
            }
            return true;
        }
        
        // Check if we can attack
        if (canExecute(entity, player, map)) {
            executeAttack(entity, player);
            return true;
        }
        
        return false;
    }
    
    /**
     * Executes the attack.
     * @param entity The attacking entity
     * @param player The target player
     */
    protected void executeAttack(Entity entity, Player player) {
        // Start attack
        attackTimer = attackDurationTicks;
        cooldownTimer = attackCooldownTicks;
        hasAttacked = true;
        
        // Apply damage
        player.damage(attackDamage);
        
        // Apply knockback for melee attacks
        if (!isRanged) {
            player.applyKnockback(entity.getX(), entity.getY());
        }
        
        onStart(entity);
    }
    
    /**
     * Checks if the entity has line of sight to the player.
     * @param entity The entity
     * @param player The player
     * @param map The tile map
     * @return true if there's line of sight, false otherwise
     */
    protected boolean hasLineOfSight(Entity entity, Player player, TileMap map) {
        // Simple line of sight check - could be enhanced with raycasting
        double dx = player.getX() - entity.getX();
        double dy = player.getY() - entity.getY();
        double distance = Math.hypot(dx, dy);
        
        if (distance == 0) return true;
        
        // Check a few points along the line
        int steps = (int) Math.ceil(distance / 10.0); // Check every 10 pixels
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            double checkX = entity.getX() + dx * t;
            double checkY = entity.getY() + dy * t;
            
            // Check if this point is a wall
            int tileX = (int) (checkX / 32); // Assuming 32x32 tiles
            int tileY = (int) (checkY / 32);
            
            if (map.isWall(tileX, tileY)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets the current attack timer value.
     * @return The attack timer
     */
    public int getAttackTimer() {
        return attackTimer;
    }
    
    /**
     * Gets the current cooldown timer value.
     * @return The cooldown timer
     */
    public int getCooldownTimer() {
        return cooldownTimer;
    }
    
    /**
     * Checks if the entity is currently attacking.
     * @return true if attacking, false otherwise
     */
    public boolean isAttacking() {
        return attackTimer > 0;
    }
    
    /**
     * Checks if the attack is on cooldown.
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown() {
        return cooldownTimer > 0;
    }
    
    /**
     * Gets the attack range.
     * @return The attack range
     */
    public double getAttackRange() {
        return attackRange;
    }
    
    /**
     * Gets the minimum attack range (for ranged enemies).
     * @return The minimum attack range
     */
    public double getMinAttackRange() {
        return minAttackRange;
    }
    
    /**
     * Gets whether this is a ranged attack.
     * @return true if ranged, false if melee
     */
    public boolean isRanged() {
        return isRanged;
    }
    
    /**
     * Resets the attack state (useful for level transitions).
     */
    public void reset() {
        cooldownTimer = 0;
        attackTimer = 0;
        hasAttacked = false;
        onStop(null);
    }
}
