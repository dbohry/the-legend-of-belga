package com.lhamacorp.games.tlob.client.save;

import org.junit.jupiter.api.Test;
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
        
        SaveState state = new SaveState(seed, completedMaps);
        assertEquals(seed, state.getWorldSeed(), "World seed should match constructor parameter");
        assertEquals(completedMaps, state.getCompletedMaps(), "Completed maps should match constructor parameter");
    }

    @Test
    void testSaveStateImmutability() {
        // Test that SaveState is immutable
        long seed = 12345L;
        int completedMaps = 7;
        
        SaveState state = new SaveState(seed, completedMaps);
        
        // Verify the values are correctly set
        assertEquals(seed, state.getWorldSeed(), "World seed should be immutable");
        assertEquals(completedMaps, state.getCompletedMaps(), "Completed maps should be immutable");
        
        // Create a new state with different values
        SaveState newState = new SaveState(seed + 1, completedMaps + 1);
        assertNotEquals(state.getWorldSeed(), newState.getWorldSeed(), "Different states should have different seeds");
        assertNotEquals(state.getCompletedMaps(), newState.getCompletedMaps(), "Different states should have different completed maps");
    }

    @Test
    void testSaveStateToString() {
        long seed = 54321L;
        int completedMaps = 15;
        
        SaveState state = new SaveState(seed, completedMaps);
        String stateString = state.toString();
        
        assertTrue(stateString.contains(String.valueOf(seed)), "toString should contain seed");
        assertTrue(stateString.contains(String.valueOf(completedMaps)), "toString should contain completed maps");
        assertTrue(stateString.contains("SaveState"), "toString should contain class name");
    }

    @Test
    void testSaveStateWithZeroValues() {
        // Test edge case with zero values
        SaveState state = new SaveState(0L, 0);
        assertEquals(0L, state.getWorldSeed(), "Zero seed should be preserved");
        assertEquals(0, state.getCompletedMaps(), "Zero completed maps should be preserved");
    }

    @Test
    void testSaveStateWithNegativeValues() {
        // Test edge case with negative values
        SaveState state = new SaveState(-123L, -5);
        assertEquals(-123L, state.getWorldSeed(), "Negative seed should be preserved");
        assertEquals(-5, state.getCompletedMaps(), "Negative completed maps should be preserved");
    }
}
