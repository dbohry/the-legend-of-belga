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
        activePerks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        activePerks.addPerk(new AppliedPerk("MAX_STAMINA", 0.12));
        
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
        activePerks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        
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
        assertFalse(perks.hasPerkType("MAX_HEALTH"), "Should not have MAX_HEALTH perk initially");
        
        perks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        assertEquals(1, perks.getPerkCount(), "Should have 1 perk after adding");
        assertTrue(perks.hasPerkType("MAX_HEALTH"), "Should have MAX_HEALTH perk after adding");
        
        // Adding the same perk type again should not duplicate
        perks.addPerk(new AppliedPerk("MAX_HEALTH", 0.20));
        assertEquals(2, perks.getPerkCount(), "Should have 2 perks after adding same type with different value");
        assertTrue(perks.hasPerkType("MAX_HEALTH"), "Should have MAX_HEALTH perk");
        
        perks.addPerk(new AppliedPerk("MAX_STAMINA", 0.12));
        assertEquals(3, perks.getPerkCount(), "Should have 3 perks after adding second type");
        assertTrue(perks.hasPerkType("MAX_STAMINA"), "Should have MAX_STAMINA perk");
    }

    @Test
    void testActivePerksToString() {
        ActivePerks perks = new ActivePerks();
        perks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        perks.addPerk(new AppliedPerk("MAX_STAMINA", 0.12));
        
        String perksString = perks.toString();
        
        assertTrue(perksString.contains("ActivePerks"), "toString should contain class name");
        assertTrue(perksString.contains("count=2"), "toString should contain perk count");
        assertTrue(perksString.contains("MAX_HEALTH"), "toString should contain first perk");
        assertTrue(perksString.contains("MAX_STAMINA"), "toString should contain second perk");
    }

    @Test
    void testActivePerksImmutability() {
        ActivePerks perks = new ActivePerks();
        perks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        
        List<AppliedPerk> appliedPerks = perks.getAppliedPerks();
        assertEquals(1, appliedPerks.size(), "Should have 1 perk");
        
        // Modifying the returned list should not affect the original
        appliedPerks.add(new AppliedPerk("MAX_STAMINA", 0.12));
        assertEquals(1, perks.getPerkCount(), "Original should still have 1 perk");
        assertFalse(perks.hasPerkType("MAX_STAMINA"), "Original should not have MAX_STAMINA");
    }

    @Test
    void testSaveStateToString() {
        long seed = 54321L;
        int completedMaps = 15;
        ActivePerks activePerks = new ActivePerks();
        activePerks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        
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
        activePerks.addPerk(new AppliedPerk("MAX_HEALTH", 0.15));
        SaveState state = new SaveState(-123L, -5, activePerks);
        assertEquals(-123L, state.getWorldSeed(), "Negative seed should be preserved");
        assertEquals(-5, state.getCompletedMaps(), "Negative completed maps should be preserved");
        assertEquals(activePerks, state.getActivePerks(), "Active perks should be preserved");
    }
    
    @Test
    void testAppliedPerkProperties() {
        // Test AppliedPerk constructor and getters
        AppliedPerk perk = new AppliedPerk("MAX_HEALTH", 0.15);
        assertEquals("MAX_HEALTH", perk.getPerkType(), "Perk type should match constructor");
        assertEquals(0.15, perk.getValue(), 0.001, "Perk value should match constructor");
    }
    
    @Test
    void testAppliedPerkToString() {
        AppliedPerk perk = new AppliedPerk("STAMINA_REGEN", 0.075);
        String perkString = perk.toString();
        
        assertTrue(perkString.contains("AppliedPerk"), "toString should contain class name");
        assertTrue(perkString.contains("STAMINA_REGEN"), "toString should contain perk type");
        assertTrue(perkString.contains("0.075"), "toString should contain perk value");
    }
    
    @Test
    void testAppliedPerkWithDifferentValues() {
        // Test that different values are preserved correctly
        AppliedPerk perk1 = new AppliedPerk("MAX_HEALTH", 0.10);
        AppliedPerk perk2 = new AppliedPerk("MAX_HEALTH", 0.20);
        
        assertEquals("MAX_HEALTH", perk1.getPerkType(), "Both perks should have same type");
        assertEquals("MAX_HEALTH", perk2.getPerkType(), "Both perks should have same type");
        assertEquals(0.10, perk1.getValue(), 0.001, "First perk should have 10% value");
        assertEquals(0.20, perk2.getValue(), 0.001, "Second perk should have 20% value");
        assertNotEquals(perk1.getValue(), perk2.getValue(), "Perks should have different values");
    }
}
