package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.List;

/**
 * Interface for all enemy behaviors.
 * Behaviors define how enemies act and react in the game world.
 */
public interface Behavior {
    
    /**
     * Updates the behavior for the current game tick.
     * @param entity The entity this behavior is controlling
     * @param player The player entity
     * @param map The current tile map
     * @param nearbyAllies List of nearby allied entities
     * @param args Additional arguments that might be needed
     * @return true if the behavior executed an action, false otherwise
     */
    boolean update(Entity entity, Player player, TileMap map, List<Entity> nearbyAllies, Object... args);
    
    /**
     * Gets the priority of this behavior. Higher priority behaviors are executed first.
     * @return The priority value (higher = more important)
     */
    int getPriority();
    
    /**
     * Checks if this behavior can be executed at the current time.
     * @param entity The entity this behavior is controlling
     * @param player The player entity
     * @param map The current tile map
     * @return true if the behavior can execute, false otherwise
     */
    boolean canExecute(Entity entity, Player player, TileMap map);
    
    /**
     * Called when the behavior starts executing.
     * @param entity The entity this behavior is controlling
     */
    void onStart(Entity entity);
    
    /**
     * Called when the behavior stops executing.
     * @param entity The entity this behavior is controlling
     */
    void onStop(Entity entity);
    
    /**
     * Gets the name of this behavior for debugging and identification.
     * @return The behavior name
     */
    String getName();
}
