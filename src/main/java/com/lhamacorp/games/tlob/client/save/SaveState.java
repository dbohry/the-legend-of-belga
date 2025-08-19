package com.lhamacorp.games.tlob.client.save;

import java.io.Serializable;

/**
 * Represents the save state for a single player game.
 * Contains the world seed, number of completed maps, active perks, and player XP/level.
 */
public class SaveState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final long worldSeed;
    private final int completedMaps;
    private final ActivePerks activePerks;
    private final int playerXP;
    private final int playerLevel;
    
    public SaveState(long worldSeed, int completedMaps, ActivePerks activePerks) {
        this(worldSeed, completedMaps, activePerks, 0, 1);
    }
    
    public SaveState(long worldSeed, int completedMaps, ActivePerks activePerks, int playerXP, int playerLevel) {
        this.worldSeed = worldSeed;
        this.completedMaps = completedMaps;
        this.activePerks = activePerks;
        this.playerXP = Math.max(0, playerXP);
        this.playerLevel = Math.max(1, playerLevel);
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
    
    public int getPlayerXP() {
        return playerXP;
    }
    
    public int getPlayerLevel() {
        return playerLevel;
    }
    
    @Override
    public String toString() {
        return "SaveState{seed=" + worldSeed + ", completedMaps=" + completedMaps + 
               ", activePerks=" + activePerks + ", playerXP=" + playerXP + 
               ", playerLevel=" + playerLevel + "}";
    }
}
