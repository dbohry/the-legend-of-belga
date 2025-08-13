package com.lhamacorp.games.tlob.client.save;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the active perks that have been selected by the player.
 * This class stores the perk identifiers so they can be re-applied when loading a save.
 */
public class ActivePerks implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final List<String> perkIds;
    
    public ActivePerks() {
        this.perkIds = new ArrayList<>();
    }
    
    public ActivePerks(List<String> perkIds) {
        this.perkIds = new ArrayList<>(perkIds);
    }
    
    /**
     * Adds a perk ID to the active perks list.
     * @param perkId the identifier of the perk to add
     */
    public void addPerk(String perkId) {
        if (!perkIds.contains(perkId)) {
            perkIds.add(perkId);
        }
    }
    
    /**
     * Gets the list of active perk IDs.
     * @return the list of perk identifiers
     */
    public List<String> getPerkIds() {
        return new ArrayList<>(perkIds);
    }
    
    /**
     * Checks if a specific perk is active.
     * @param perkId the perk identifier to check
     * @return true if the perk is active, false otherwise
     */
    public boolean hasPerk(String perkId) {
        return perkIds.contains(perkId);
    }
    
    /**
     * Gets the number of active perks.
     * @return the count of active perks
     */
    public int getPerkCount() {
        return perkIds.size();
    }
    
    /**
     * Clears all active perks.
     */
    public void clear() {
        perkIds.clear();
    }
    
    @Override
    public String toString() {
        return "ActivePerks{count=" + perkIds.size() + ", perks=" + perkIds + "}";
    }
}
