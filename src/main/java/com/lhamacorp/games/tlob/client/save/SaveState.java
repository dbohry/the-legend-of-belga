package com.lhamacorp.games.tlob.client.save;

import java.io.Serializable;

/**
 * Represents the save state for a single player game.
 * Contains the world seed and number of completed maps.
 */
public class SaveState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final long worldSeed;
    private final int completedMaps;
    
    public SaveState(long worldSeed, int completedMaps) {
        this.worldSeed = worldSeed;
        this.completedMaps = completedMaps;
    }
    
    public long getWorldSeed() {
        return worldSeed;
    }
    
    public int getCompletedMaps() {
        return completedMaps;
    }
    
    @Override
    public String toString() {
        return "SaveState{seed=" + worldSeed + ", completedMaps=" + completedMaps + "}";
    }
}
