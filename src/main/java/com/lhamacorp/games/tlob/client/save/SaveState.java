package com.lhamacorp.games.tlob.client.save;

import java.io.Serializable;

/**
 * Represents the save state for a single player game.
 * Contains the world seed, number of completed maps, and active perks.
 */
public class SaveState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final long worldSeed;
    private final int completedMaps;
    private final ActivePerks activePerks;
    
    public SaveState(long worldSeed, int completedMaps, ActivePerks activePerks) {
        this.worldSeed = worldSeed;
        this.completedMaps = completedMaps;
        this.activePerks = activePerks;
    }
    
    public long getWorldSeed() {
        return worldSeed;
    }
    
    public int getCompletedMaps() {
        return completedMaps;
    }
    
    public ActivePerks getActivePerks() {
        return activePerks;
    }
    
    @Override
    public String toString() {
        return "SaveState{seed=" + worldSeed + ", completedMaps=" + completedMaps + ", activePerks=" + activePerks + "}";
    }
}
