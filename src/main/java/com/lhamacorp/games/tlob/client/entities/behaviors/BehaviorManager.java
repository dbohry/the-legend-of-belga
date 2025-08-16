package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.*;

/**
 * Manages multiple behaviors for an entity.
 * Behaviors are executed in priority order, with higher priority behaviors taking precedence.
 */
public class BehaviorManager {
    
    private final List<Behavior> behaviors;
    private final Map<String, Behavior> behaviorMap;
    private Behavior currentBehavior;
    private final Entity entity;
    
    /**
     * Creates a new behavior manager for the specified entity.
     * @param entity The entity this manager controls
     */
    public BehaviorManager(Entity entity) {
        this.entity = entity;
        this.behaviors = new ArrayList<>();
        this.behaviorMap = new HashMap<>();
        this.currentBehavior = null;
    }
    
    /**
     * Adds a behavior to the manager.
     * @param behavior The behavior to add
     */
    public void addBehavior(Behavior behavior) {
        behaviors.add(behavior);
        behaviorMap.put(behavior.getName(), behavior);
        
        // Sort behaviors by priority (highest first)
        behaviors.sort((b1, b2) -> Integer.compare(b2.getPriority(), b1.getPriority()));
    }
    
    /**
     * Removes a behavior from the manager.
     * @param behaviorName The name of the behavior to remove
     */
    public void removeBehavior(String behaviorName) {
        Behavior behavior = behaviorMap.remove(behaviorName);
        if (behavior != null) {
            behaviors.remove(behavior);
            if (currentBehavior == behavior) {
                currentBehavior = null;
            }
        }
    }
    
    /**
     * Gets a behavior by name.
     * @param behaviorName The name of the behavior
     * @return The behavior, or null if not found
     */
    public Behavior getBehavior(String behaviorName) {
        return behaviorMap.get(behaviorName);
    }
    
    /**
     * Updates all behaviors and executes the highest priority one that can execute.
     * @param player The player entity
     * @param map The current tile map
     * @param nearbyAllies List of nearby allied entities
     * @param args Additional arguments
     */
    public void update(Player player, TileMap map, List<Entity> nearbyAllies, Object... args) {
        // Find the highest priority behavior that can execute
        Behavior nextBehavior = null;
        for (Behavior behavior : behaviors) {
            if (behavior.canExecute(entity, player, map)) {
                nextBehavior = behavior;
                break;
            }
        }
        
        // If we have a new behavior, stop the current one and start the new one
        if (nextBehavior != currentBehavior) {
            if (currentBehavior != null) {
                currentBehavior.onStop(entity);
            }
            if (nextBehavior != null) {
                nextBehavior.onStart(entity);
            }
            currentBehavior = nextBehavior;
        }
        
        // Execute the current behavior
        if (currentBehavior != null) {
            currentBehavior.update(entity, player, map, nearbyAllies, args);
        }
    }
    
    /**
     * Gets the currently active behavior.
     * @return The active behavior, or null if none
     */
    public Behavior getCurrentBehavior() {
        return currentBehavior;
    }
    
    /**
     * Gets the name of the currently active behavior.
     * @return The name of the active behavior, or "None" if none
     */
    public String getCurrentBehaviorName() {
        return currentBehavior != null ? currentBehavior.getName() : "None";
    }
    
    /**
     * Checks if a specific behavior is currently active.
     * @param behaviorName The name of the behavior to check
     * @return true if the behavior is active, false otherwise
     */
    public boolean isBehaviorActive(String behaviorName) {
        return currentBehavior != null && currentBehavior.getName().equals(behaviorName);
    }
    
    /**
     * Gets all behaviors in priority order.
     * @return List of behaviors sorted by priority
     */
    public List<Behavior> getAllBehaviors() {
        return new ArrayList<>(behaviors);
    }
    
    /**
     * Gets the number of behaviors managed by this manager.
     * @return The number of behaviors
     */
    public int getBehaviorCount() {
        return behaviors.size();
    }
    
    /**
     * Clears all behaviors from the manager.
     */
    public void clearBehaviors() {
        if (currentBehavior != null) {
            currentBehavior.onStop(entity);
        }
        behaviors.clear();
        behaviorMap.clear();
        currentBehavior = null;
    }
    
    /**
     * Resets all behaviors to their initial state.
     */
    public void resetBehaviors() {
        if (currentBehavior != null) {
            currentBehavior.onStop(entity);
        }
        currentBehavior = null;
        
        // Reset each behavior if it has a reset method
        for (Behavior behavior : behaviors) {
            if (behavior instanceof AttackBehavior) {
                ((AttackBehavior) behavior).reset();
            }
        }
    }
    
    /**
     * Gets debug information about the behavior manager.
     * @return A string containing debug information
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("BehaviorManager Debug Info:\n");
        sb.append("Current Behavior: ").append(getCurrentBehaviorName()).append("\n");
        sb.append("Total Behaviors: ").append(getBehaviorCount()).append("\n");
        
        for (Behavior behavior : behaviors) {
            sb.append("  - ").append(behavior.getName())
              .append(" (Priority: ").append(behavior.getPriority())
              .append(")\n");
        }
        
        return sb.toString();
    }
}
