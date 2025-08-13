package com.lhamacorp.games.tlob.client.save;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the active perks that have been selected by the player.
 * This class stores the applied perks with their specific values so they can be 
 * re-applied exactly when loading a save.
 */
public class ActivePerks implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final List<AppliedPerk> appliedPerks;
    
    public ActivePerks() {
        this.appliedPerks = new ArrayList<>();
    }
    
    public ActivePerks(List<AppliedPerk> appliedPerks) {
        this.appliedPerks = new ArrayList<>(appliedPerks);
    }
    
    /**
     * Adds an applied perk to the active perks list.
     * @param appliedPerk the applied perk to add
     */
    public void addPerk(AppliedPerk appliedPerk) {
        if (appliedPerk != null) {
            appliedPerks.add(appliedPerk);
        }
    }
    
    /**
     * Gets the list of applied perks.
     * @return the list of applied perks
     */
    public List<AppliedPerk> getAppliedPerks() {
        return new ArrayList<>(appliedPerks);
    }
    
    /**
     * Checks if a specific perk type is active.
     * @param perkType the perk type to check
     * @return true if the perk type is active, false otherwise
     */
    public boolean hasPerkType(String perkType) {
        return appliedPerks.stream().anyMatch(p -> p.getPerkType().equals(perkType));
    }
    
    /**
     * Gets the number of perks of a specific type.
     * @param perkType the perk type to count
     * @return the count of perks of that type
     */
    public int getPerkTypeCount(String perkType) {
        return (int) appliedPerks.stream()
            .filter(p -> p.getPerkType().equals(perkType))
            .count();
    }
    
    /**
     * Gets all perks of a specific type.
     * @param perkType the perk type to get
     * @return a list of perks of that type
     */
    public List<AppliedPerk> getPerksOfType(String perkType) {
        return appliedPerks.stream()
            .filter(p -> p.getPerkType().equals(perkType))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Gets the total value for a specific perk type (useful for cumulative effects).
     * @param perkType the perk type to sum
     * @return the sum of all values for that perk type
     */
    public double getTotalValueForType(String perkType) {
        return appliedPerks.stream()
            .filter(p -> p.getPerkType().equals(perkType))
            .mapToDouble(AppliedPerk::getValue)
            .sum();
    }
    
    /**
     * Gets the number of active perks.
     * @return the count of active perks
     */
    public int getPerkCount() {
        return appliedPerks.size();
    }
    
    /**
     * Clears all active perks.
     */
    public void clear() {
        appliedPerks.clear();
    }
    
    @Override
    public String toString() {
        return "ActivePerks{count=" + appliedPerks.size() + ", perks=" + appliedPerks + "}";
    }
}
