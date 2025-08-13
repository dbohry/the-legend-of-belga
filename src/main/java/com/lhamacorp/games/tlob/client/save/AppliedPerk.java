package com.lhamacorp.games.tlob.client.save;

import java.io.Serializable;

/**
 * Represents a perk that has been applied to the player with its specific values.
 * This class stores the perk type and the exact values that were applied,
 * allowing for precise restoration of player stats when loading a save.
 */
public class AppliedPerk implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String perkType;
    private final double value;
    
    public AppliedPerk(String perkType, double value) {
        this.perkType = perkType;
        this.value = value;
    }
    
    public String getPerkType() {
        return perkType;
    }
    
    public double getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return "AppliedPerk{type='" + perkType + "', value=" + value + "}";
    }
}
