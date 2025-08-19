package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BaseGameManager level-up functionality
 */
class BaseGameManagerTest {

    private TestGameManager gameManager;
    private Player player;

    @BeforeEach
    void setUp() {
        gameManager = new TestGameManager();
        player = new Player(100, 100, new Sword(2, 28, 12, 10, 16));
        gameManager.player = player;
        player.setLevelUpListener(gameManager);
    }

    @Test
    void testLevelUpListenerSetup() {
        assertNotNull(player);
        assertNotNull(gameManager.player);
        assertEquals(gameManager, player.getLevelUpListener());
    }

    @Test
    void testOnLevelUp() {
        // Initially no level-up calls
        assertFalse(gameManager.wasLevelUpCalled());
        assertEquals(0, gameManager.getLastLevelUpLevel());

        // Trigger level-up
        gameManager.onLevelUp(2);

        // Should now have level-up call tracked
        assertTrue(gameManager.wasLevelUpCalled());
        assertEquals(2, gameManager.getLastLevelUpLevel());
    }

    @Test
    void testLevelUpPerkPriority() {
        // Set up a level-up perk by calling onLevelUp
        gameManager.onLevelUp(3);
        gameManager.resetLevelUpTracking(); // Reset for clean test

        // Check that level-up perk reason is shown
        String reason = gameManager.getPerkChoiceReason();
        assertEquals("Level 3! Choose a perk:", reason);
    }

    @Test
    void testMapCompletionPerkReason() {
        // No level-up perk, should show map completion reason
        String reason = gameManager.getPerkChoiceReason();
        assertEquals("Map Complete! Choose a perk:", reason);
    }

    /**
     * Test implementation of BaseGameManager for testing purposes
     */
    private static class TestGameManager extends BaseGameManager {
        // Track level-up calls for testing
        private boolean levelUpCalled = false;
        private int lastLevelUpLevel = 0;

        @Override
        protected void updatePlaying(java.awt.Point aimWorld) {
            // Test implementation - do nothing
        }

        @Override
        protected void autoSave() {
            // Test implementation - do nothing
        }

        @Override
        public void onLevelUp(int newLevel) {
            levelUpCalled = true;
            lastLevelUpLevel = newLevel;
            super.onLevelUp(newLevel);
        }

        // Test helper methods
        public boolean wasLevelUpCalled() {
            return levelUpCalled;
        }

        public int getLastLevelUpLevel() {
            return lastLevelUpLevel;
        }

        public void resetLevelUpTracking() {
            levelUpCalled = false;
            lastLevelUpLevel = 0;
        }

        @Override
        public boolean shouldShowPerkChoices() {
            return true;
        }

        @Override
        public String getPerkChoiceReason() {
            if (hasLevelUpPerk) {
                return "Level " + pendingLevelUp + "! Choose a perk:";
            }
            return "Map Complete! Choose a perk:";
        }
    }
}
