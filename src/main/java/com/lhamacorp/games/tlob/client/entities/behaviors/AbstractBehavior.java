package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.List;

/**
 * Abstract base class for behaviors that provides common functionality.
 */
public abstract class AbstractBehavior implements Behavior {
    
    protected final String name;
    protected final int priority;
    protected boolean isActive = false;
    protected int activeTicks = 0;
    
    /**
     * Creates a new behavior with the specified name and priority.
     * @param name The name of the behavior
     * @param priority The priority of the behavior (higher = more important)
     */
    protected AbstractBehavior(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void onStart(Entity entity) {
        isActive = true;
        activeTicks = 0;
    }
    
    @Override
    public void onStop(Entity entity) {
        isActive = false;
        activeTicks = 0;
    }
    
    /**
     * Checks if this behavior is currently active.
     * @return true if the behavior is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Gets the number of ticks this behavior has been active.
     * @return The number of active ticks
     */
    public int getActiveTicks() {
        return activeTicks;
    }
    
    /**
     * Increments the active tick counter. Should be called in update().
     */
    protected void incrementActiveTicks() {
        if (isActive) {
            activeTicks++;
        }
    }
    
    /**
     * Utility method to calculate distance between two points.
     * @param x1 First x coordinate
     * @param y1 First y coordinate
     * @param x2 Second x coordinate
     * @param y2 Second y coordinate
     * @return The distance between the points
     */
    protected double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }
    
    /**
     * Utility method to calculate distance between an entity and a point.
     * @param entity The entity
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The distance between the entity and the point
     */
    protected double calculateDistance(Entity entity, double x, double y) {
        return calculateDistance(entity.getX(), entity.getY(), x, y);
    }
    
    /**
     * Utility method to calculate distance between two entities.
     * @param entity1 The first entity
     * @param entity2 The second entity
     * @return The distance between the entities
     */
    protected double calculateDistance(Entity entity1, Entity entity2) {
        return calculateDistance(entity1.getX(), entity1.getY(), entity2.getX(), entity2.getY());
    }
    
    /**
     * Utility method to normalize a direction vector.
     * @param dx The x component of the direction
     * @param dy The y component of the direction
     * @return A normalized direction array [dx, dy]
     */
    protected double[] normalizeDirection(double dx, double dy) {
        double distance = Math.hypot(dx, dy);
        if (distance > 0) {
            return new double[]{dx / distance, dy / distance};
        }
        return new double[]{0, 0};
    }
    
    /**
     * Utility method to check if a point is within a specified range of an entity.
     * @param entity The entity to check from
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @param range The range to check within
     * @return true if the point is within range, false otherwise
     */
    protected boolean isWithinRange(Entity entity, double x, double y, double range) {
        return calculateDistance(entity, x, y) <= range;
    }
    
    /**
     * Utility method to check if a player is within a specified range of an entity.
     * @param entity The entity to check from
     * @param player The player to check
     * @param range The range to check within
     * @return true if the player is within range, false otherwise
     */
    protected boolean isPlayerWithinRange(Entity entity, Player player, double range) {
        return calculateDistance(entity, player) <= range;
    }
}
