package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.List;

/**
 * Behavior that handles enemy movement.
 * Supports different movement patterns and strategies.
 */
public class MovementBehavior extends AbstractBehavior {
    
    public static final int PRIORITY = 50; // Medium priority - movement when not attacking
    
    public enum MovementType {
        APPROACH_PLAYER,    // Move towards the player
        RETREAT_FROM_PLAYER, // Move away from the player
        MAINTAIN_DISTANCE,  // Keep a specific distance from the player
        PATROL,            // Move between patrol points
        WANDER,            // Random movement
        GROUP_MOVEMENT,    // Move with nearby allies
        IDLE              // Stay mostly still
    }
    
    private final MovementType movementType;
    private final double aggressionRadius;
    private final double preferredDistance; // For maintain distance
    private final double speedMultiplier;
    private final boolean canStrafe;
    private final double strafeStrength;
    private final double strafeFrequency;
    
    // Movement state
    private double currentDx = 0;
    private double currentDy = 0;
    private int movementTimer = 0;
    private int patrolPointIndex = 0;
    private final double[] patrolPointsX = new double[3];
    private final double[] patrolPointsY = new double[3];
    private boolean movedThisTick = false;
    
    /**
     * Creates a new movement behavior.
     * @param movementType The type of movement to perform
     * @param aggressionRadius The radius at which the enemy becomes aggressive
     * @param preferredDistance The preferred distance to maintain from the player
     * @param speedMultiplier Multiplier for movement speed
     * @param canStrafe Whether the enemy can strafe while moving
     * @param strafeStrength How much the enemy strafes
     * @param strafeFrequency How often the enemy strafes
     */
    public MovementBehavior(MovementType movementType, double aggressionRadius, double preferredDistance,
                           double speedMultiplier, boolean canStrafe, double strafeStrength, double strafeFrequency) {
        super("Movement", PRIORITY);
        this.movementType = movementType;
        this.aggressionRadius = aggressionRadius;
        this.preferredDistance = preferredDistance;
        this.speedMultiplier = speedMultiplier;
        this.canStrafe = canStrafe;
        this.strafeStrength = strafeStrength;
        this.strafeFrequency = strafeFrequency;
        
        // Initialize patrol points if using patrol movement
        if (movementType == MovementType.PATROL) {
            initializePatrolPoints();
        }
    }
    
    /**
     * Creates a simple movement behavior.
     * @param movementType The type of movement to perform
     * @param aggressionRadius The radius at which the enemy becomes aggressive
     */
    public MovementBehavior(MovementType movementType, double aggressionRadius) {
        this(movementType, aggressionRadius, 0, 1.0, false, 0, 0);
    }
    
    @Override
    public boolean canExecute(Entity entity, Player player, TileMap map) {
        // This method will be called with specific parameters when needed
        // For now, always allow execution
        return true;
    }
    
    @Override
    public boolean update(Entity entity, Player player, TileMap map, List<Entity> nearbyAllies, Object... args) {
        incrementActiveTicks();
        
        // Check if we should change movement type based on player proximity
        MovementType effectiveMovementType = determineEffectiveMovementType(entity, player);
        
        // Execute movement based on type
        switch (effectiveMovementType) {
            case APPROACH_PLAYER:
                return approachPlayer(entity, player, map);
            case RETREAT_FROM_PLAYER:
                return retreatFromPlayer(entity, player, map);
            case MAINTAIN_DISTANCE:
                return maintainDistance(entity, player, map);
            case PATROL:
                return patrol(entity, player, map);
            case WANDER:
                return wander(entity, player, map);
            case GROUP_MOVEMENT:
                return groupMovement(entity, player, map, nearbyAllies);
            case IDLE:
                return idle(entity, player, map);
            default:
                return false;
        }
    }
    
    /**
     * Determines the effective movement type based on current conditions.
     */
    private MovementType determineEffectiveMovementType(Entity entity, Player player) {
        double distance = calculateDistance(entity, player);
        
        // If player is within aggression radius, prioritize player-focused movement
        if (distance <= aggressionRadius) {
            if (movementType == MovementType.MAINTAIN_DISTANCE) {
                if (distance < preferredDistance) {
                    return MovementType.RETREAT_FROM_PLAYER;
                } else if (distance > preferredDistance + 20) {
                    return MovementType.APPROACH_PLAYER;
                }
            } else if (movementType == MovementType.APPROACH_PLAYER || 
                      movementType == MovementType.PATROL || 
                      movementType == MovementType.WANDER) {
                return MovementType.APPROACH_PLAYER;
            }
        }
        
        return movementType;
    }
    
    /**
     * Moves the entity towards the player.
     */
    private boolean approachPlayer(Entity entity, Player player, TileMap map) {
        double dx = player.getX() - entity.getX();
        double dy = player.getY() - entity.getY();
        
        if (dx == 0 && dy == 0) return false;
        
        double[] direction = normalizeDirection(dx, dy);
        currentDx = direction[0];
        currentDy = direction[1];
        
        // Add strafing if enabled
        if (canStrafe) {
            addStrafeMovement(entity);
        }
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Moves the entity away from the player.
     */
    private boolean retreatFromPlayer(Entity entity, Player player, TileMap map) {
        double dx = entity.getX() - player.getX();
        double dy = entity.getY() - player.getY();
        
        if (dx == 0 && dy == 0) return false;
        
        double[] direction = normalizeDirection(dx, dy);
        currentDx = direction[0];
        currentDy = direction[1];
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Maintains a specific distance from the player.
     */
    private boolean maintainDistance(Entity entity, Player player, TileMap map) {
        double distance = calculateDistance(entity, player);
        double targetDistance = preferredDistance;
        
        if (Math.abs(distance - targetDistance) < 10) {
            // Close enough, make small adjustments
            return makeSmallAdjustments(entity, player, map);
        }
        
        if (distance < targetDistance) {
            return retreatFromPlayer(entity, player, map);
        } else {
            return approachPlayer(entity, player, map);
        }
    }
    
    /**
     * Makes small positional adjustments.
     */
    private boolean makeSmallAdjustments(Entity entity, Player player, TileMap map) {
        if (movementTimer <= 0) {
            // Small random movement
            double angle = Math.random() * Math.PI * 2;
            currentDx = Math.cos(angle) * 0.3;
            currentDy = Math.sin(angle) * 0.3;
            movementTimer = 30 + (int) (Math.random() * 60);
        } else {
            movementTimer--;
        }
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Patrols between defined points.
     */
    private boolean patrol(Entity entity, Player player, TileMap map) {
        if (movementTimer <= 0) {
            // Move to next patrol point
            double targetX = patrolPointsX[patrolPointIndex];
            double targetY = patrolPointsY[patrolPointIndex];
            
            double dx = targetX - entity.getX();
            double dy = targetY - entity.getY();
            double distance = Math.hypot(dx, dy);
            
            if (distance < 15) {
                // Reached patrol point, move to next
                patrolPointIndex = (patrolPointIndex + 1) % 3;
                movementTimer = 60 + (int) (Math.random() * 120);
            } else {
                // Move towards patrol point
                double[] direction = normalizeDirection(dx, dy);
                currentDx = direction[0];
                currentDy = direction[1];
            }
        } else {
            movementTimer--;
        }
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Random wandering movement.
     */
    private boolean wander(Entity entity, Player player, TileMap map) {
        if (movementTimer <= 0) {
            // Pick new random direction
            double angle = Math.random() * Math.PI * 2;
            currentDx = Math.cos(angle);
            currentDy = Math.sin(angle);
            movementTimer = 45 + (int) (Math.random() * 90);
        } else {
            movementTimer--;
        }
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Group movement with nearby allies.
     */
    private boolean groupMovement(Entity entity, Player player, TileMap map, List<Entity> nearbyAllies) {
        if (nearbyAllies == null || nearbyAllies.isEmpty()) {
            return wander(entity, player, map);
        }
        
        if (movementTimer <= 0) {
            // Move towards the center of nearby allies
            double centerX = 0, centerY = 0;
            for (Entity ally : nearbyAllies) {
                centerX += ally.getX();
                centerY += ally.getY();
            }
            centerX /= nearbyAllies.size();
            centerY /= nearbyAllies.size();
            
            double dx = centerX - entity.getX();
            double dy = centerY - entity.getY();
            double distance = Math.hypot(dx, dy);
            
            if (distance > 0) {
                double[] direction = normalizeDirection(dx, dy);
                currentDx = direction[0];
                currentDy = direction[1];
                movementTimer = 60 + (int) (Math.random() * 120);
            } else {
                return wander(entity, player, map);
            }
        } else {
            movementTimer--;
        }
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Idle movement (mostly staying still).
     */
    private boolean idle(Entity entity, Player player, TileMap map) {
        if (movementTimer <= 0) {
            // Occasionally move
            if (Math.random() < 0.2) {
                double angle = Math.random() * Math.PI * 2;
                currentDx = Math.cos(angle) * 0.2;
                currentDy = Math.sin(angle) * 0.2;
                movementTimer = 30 + (int) (Math.random() * 60);
            } else {
                movementTimer = 120 + (int) (Math.random() * 180);
            }
        } else {
            movementTimer--;
        }
        
        return moveEntity(entity, map, player);
    }
    
    /**
     * Adds strafing movement to the current direction.
     */
    private void addStrafeMovement(Entity entity) {
        if (!canStrafe) return;
        
        double strafeAngle = (entity.hashCode() % 1000) * 0.01 + System.currentTimeMillis() * 0.001 * strafeFrequency;
        double strafeDx = Math.cos(strafeAngle) * strafeStrength;
        double strafeDy = Math.sin(strafeAngle) * strafeStrength;
        
        currentDx += strafeDx;
        currentDy += strafeDy;
        
        // Renormalize
        double[] normalized = normalizeDirection(currentDx, currentDy);
        currentDx = normalized[0];
        currentDy = normalized[1];
    }
    
    /**
     * Actually moves the entity using the current direction.
     */
    private boolean moveEntity(Entity entity, TileMap map, Player player) {
        if (currentDx == 0 && currentDy == 0) return false;
        
        double moveSpeed = entity.getSpeed() * speedMultiplier;
        double newX = entity.getX() + currentDx * moveSpeed;
        double newY = entity.getY() + currentDy * moveSpeed;
        
        // Check collision and move
        boolean moved = false;
        if (!collidesWithMap(newX, entity.getY(), map) && !collidesWithPlayer(newX, entity.getY(), player)) {
            // Access protected x field directly
            try {
                java.lang.reflect.Field xField = Entity.class.getDeclaredField("x");
                xField.setAccessible(true);
                xField.set(entity, newX);
                moved = true;
            } catch (Exception e) {
                // Fallback: can't move
            }
        }
        
        if (!collidesWithMap(entity.getX(), newY, map) && !collidesWithPlayer(entity.getX(), newY, player)) {
            // Access protected y field directly
            try {
                java.lang.reflect.Field yField = Entity.class.getDeclaredField("y");
                yField.setAccessible(true);
                yField.set(entity, newY);
                moved = true;
            } catch (Exception e) {
                // Fallback: can't move
            }
        }
        
        movedThisTick = moved;
        return moved;
    }
    
    /**
     * Checks if a position collides with the map.
     */
    private boolean collidesWithMap(double x, double y, TileMap map) {
        // This would need to be implemented based on your map collision system
        // For now, return false to allow movement
        return false;
    }
    
    /**
     * Checks if a position collides with the player.
     */
    private boolean collidesWithPlayer(double x, double y, Player player) {
        // This would need to be implemented based on your collision detection system
        // For now, return false to allow movement
        return false;
    }
    
    /**
     * Initializes patrol points around the entity's starting position.
     */
    private void initializePatrolPoints() {
        // This would need to be called with the entity's initial position
        // For now, create default points
        for (int i = 0; i < 3; i++) {
            double angle = (i * 2 * Math.PI / 3);
            patrolPointsX[i] = Math.cos(angle) * 40;
            patrolPointsY[i] = Math.sin(angle) * 40;
        }
    }
    
    /**
     * Sets the entity's starting position for patrol point calculation.
     */
    public void setStartingPosition(double x, double y) {
        if (movementType == MovementType.PATROL) {
            for (int i = 0; i < 3; i++) {
                double angle = (i * 2 * Math.PI / 3);
                patrolPointsX[i] = x + Math.cos(angle) * 40;
                patrolPointsY[i] = y + Math.sin(angle) * 40;
            }
        }
    }
    
    /**
     * Gets the current movement direction.
     */
    public double[] getCurrentDirection() {
        return new double[]{currentDx, currentDy};
    }
    
    /**
     * Checks if the entity moved this tick.
     */
    public boolean hasMovedThisTick() {
        return movedThisTick;
    }
    
    /**
     * Gets the movement type.
     */
    public MovementType getMovementType() {
        return movementType;
    }
    
    /**
     * Gets the aggression radius.
     */
    public double getAggressionRadius() {
        return aggressionRadius;
    }
    
    /**
     * Gets the speed multiplier.
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }
}
