package com.lhamacorp.games.tlob.client.save;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SaveState class.
 */
public class SaveStateTest {

    @Test
    void testSaveStateProperties() {
        // Test SaveState constructor and getters
        long seed = 99999L;
        int completedMaps = 10;
        ActivePerks activePerks = new ActivePerks();
        activePerks.addPerk("MAX_HEALTH");
        activePerks.addPerk("MAX_STAMINA");
        
        SaveState state = new SaveState(seed, completedMaps, activePerks);
        assertEquals(seed, state.getWorldSeed(), "World seed should match constructor parameter");
        assertEquals(completedMaps, state.getCompletedMaps(), "Completed maps should match constructor parameter");
        assertEquals(activePerks, state.getActivePerks(), "Active perks should match constructor parameter");
    }

    @Test
    void testSaveStateImmutability() {
        // Test that SaveState is immutable
        long seed = 12345L;
        int completedMaps = 7;
        ActivePerks activePerks = new ActivePerks();
        activePerks.addPerk("MAX_HEALTH");
        
        SaveState state = new SaveState(seed, completedMaps, activePerks);
        
        // Verify the values are correctly set
        assertEquals(seed, state.getWorldSeed(), "World seed should be immutable");
        assertEquals(completedMaps, state.getCompletedMaps(), "Completed maps should be immutable");
        assertEquals(activePerks, state.getActivePerks(), "Active perks should be immutable");
        
        // Create a new state with different values
        SaveState newState = new SaveState(seed + 1, completedMaps + 1, activePerks);
        assertNotEquals(state.getWorldSeed(), newState.getWorldSeed(), "Different states should have different seeds");
        assertNotEquals(state.getCompletedMaps(), newState.getCompletedMaps(), "Different states should have different completed maps");
        assertEquals(activePerks, newState.getActivePerks(), "Active perks should be the same when not changed");
    }

    @Test
    void testActivePerksProperties() {
        // Test ActivePerks constructor and methods
        ActivePerks perks = new ActivePerks();
        
        assertEquals(0, perks.getPerkCount(), "New ActivePerks should have 0 perks");
        assertFalse(perks.hasPerk("MAX_HEALTH"), "Should not have MAX_HEALTH perk initially");
        
        perks.addPerk("MAX_HEALTH");
        assertEquals(1, perks.getPerkCount(), "Should have 1 perk after adding");
        assertTrue(perks.hasPerk("MAX_HEALTH"), "Should have MAX_HEALTH perk after adding");
        
        // Adding the same perk again should not duplicate
        perks.addPerk("MAX_HEALTH");
        assertEquals(1, perks.getPerkCount(), "Should still have 1 perk after adding duplicate");
        
        perks.addPerk("MAX_STAMINA");
        assertEquals(2, perks.getPerkCount(), "Should have 2 perks after adding second");
        assertTrue(perks.hasPerk("MAX_STAMINA"), "Should have MAX_STAMINA perk");
    }

    @Test
    void testActivePerksToString() {
        ActivePerks perks = new ActivePerks();
        perks.addPerk("MAX_HEALTH");
        perks.addPerk("MAX_STAMINA");
        
        String perksString = perks.toString();
        
        assertTrue(perksString.contains("ActivePerks"), "toString should contain class name");
        assertTrue(perksString.contains("count=2"), "toString should contain perk count");
        assertTrue(perksString.contains("MAX_HEALTH"), "toString should contain first perk");
        assertTrue(perksString.contains("MAX_STAMINA"), "toString should contain second perk");
    }

    @Test
    void testActivePerksImmutability() {
        ActivePerks perks = new ActivePerks();
        perks.addPerk("MAX_HEALTH");
        
        List<String> perkIds = perks.getPerkIds();
        assertEquals(1, perkIds.size(), "Should have 1 perk");
        
        // Modifying the returned list should not affect the original
        perkIds.add("MAX_STAMINA");
        assertEquals(1, perks.getPerkCount(), "Original should still have 1 perk");
        assertFalse(perks.hasPerk("MAX_STAMINA"), "Original should not have MAX_STAMINA");
    }

    @Test
    void testSaveStateToString() {
        long seed = 54321L;
        int completedMaps = 15;
        ActivePerks activePerks = new ActivePerks();
        activePerks.addPerk("MAX_HEALTH");
        
        SaveState state = new SaveState(seed, completedMaps, activePerks);
        String stateString = state.toString();
        
        assertTrue(stateString.contains(String.valueOf(seed)), "toString should contain seed");
        assertTrue(stateString.contains(String.valueOf(completedMaps)), "toString should contain completed maps");
        assertTrue(stateString.contains("SaveState"), "toString should contain class name");
        assertTrue(stateString.contains("ActivePerks"), "toString should contain active perks");
    }

    @Test
    void testSaveStateWithZeroValues() {
        // Test edge case with zero values
        ActivePerks activePerks = new ActivePerks();
        SaveState state = new SaveState(0L, 0, activePerks);
        assertEquals(0L, state.getWorldSeed(), "Zero seed should be preserved");
        assertEquals(0, state.getCompletedMaps(), "Zero completed maps should be preserved");
        assertEquals(activePerks, state.getActivePerks(), "Empty active perks should be preserved");
    }

    @Test
    void testSaveStateWithNegativeValues() {
        // Test edge case with negative values
        ActivePerks activePerks = new ActivePerks();
        activePerks.addPerk("MAX_HEALTH");
        SaveState state = new SaveState(-123L, -5, activePerks);
        assertEquals(-123L, state.getWorldSeed(), "Negative seed should be preserved");
        assertEquals(-5, state.getCompletedMaps(), "Negative completed maps should be preserved");
        assertEquals(activePerks, state.getActivePerks(), "Active perks should be preserved");
    }
}
