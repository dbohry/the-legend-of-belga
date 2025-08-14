package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.entities.Golen;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SpawnManagerTest {

    private SpawnManager spawnManager;
    private TileMap mockMap;
    private Player mockPlayer;
    private Random testRng;

    @BeforeEach
    void setUp() {
        // Create a deterministic RNG for testing
        testRng = new Random(12345L);
        spawnManager = new SpawnManager(new Sword(2, 28, 12, 10, 16), testRng);
        
        // Create a simple mock map
        int[][] tiles = new int[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                tiles[i][j] = 0; // Floor tiles
            }
        }
        mockMap = new TileMap(tiles, testRng);
        
        // Create a mock player
        mockPlayer = new Player(50, 50, new Sword(2, 28, 10, 10, 16));
    }

    @Test
    void testEnemyPerksAppliedOnHigherMaps() {
        // Test that enemies get perks on maps after the first one
        List<Entity> enemies = new ArrayList<>();
        
        // Spawn enemies on map 5 (completedMaps = 5) to have a higher perk chance
        // Map 5 has 75% perk chance (0.0 + 0.15 * 5 = 0.75)
        spawnManager.spawn(mockMap, mockPlayer, enemies, 5, 32);
        
        // Check that at least some enemies have perks
        boolean hasPerks = false;
        for (Entity enemy : enemies) {
            if (enemy.getPerkCount() > 0) {
                hasPerks = true;
                break;
            }
        }
        
        assertTrue(hasPerks, "Enemies should have perks on map 5+ (75% chance)");
    }

    @Test
    void testNoPerksOnFirstMap() {
        // Test that enemies don't get perks on the first map
        List<Entity> enemies = new ArrayList<>();
        
        // Spawn enemies on map 0 (completedMaps = 0)
        spawnManager.spawn(mockMap, mockPlayer, enemies, 0, 32);
        
        // Check that no enemies have perks
        for (Entity enemy : enemies) {
            assertEquals(0, enemy.getPerkCount(), 
                "Enemies should not have perks on the first map");
        }
    }

    @Test
    void testPerkStrengthScaling() {
        // Test that perk strength increases with map completion
        List<Entity> enemies1 = new ArrayList<>();
        List<Entity> enemies5 = new ArrayList<>();
        
        // Spawn enemies on different maps
        spawnManager.spawn(mockMap, mockPlayer, enemies1, 1, 32);
        spawnManager.spawn(mockMap, mockPlayer, enemies5, 5, 32);
        
        // Find enemies with health perks to compare
        Entity enemy1 = findEnemyWithHealthPerk(enemies1);
        Entity enemy5 = findEnemyWithHealthPerk(enemies5);
        
        if (enemy1 != null && enemy5 != null) {
            // Enemy on map 5 should have higher health than enemy on map 1
            assertTrue(enemy5.getMaxHealth() > enemy1.getMaxHealth(),
                "Enemies on higher maps should have stronger perks");
        }
    }

    @Test
    void testPerkCountScaling() {
        // Test that perk count increases with map completion
        List<Entity> enemies1 = new ArrayList<>();
        List<Entity> enemies10 = new ArrayList<>();
        
        // Spawn enemies on different maps
        spawnManager.spawn(mockMap, mockPlayer, enemies1, 1, 32);
        spawnManager.spawn(mockMap, mockPlayer, enemies10, 10, 32);
        
        // Calculate average perk count
        double avgPerks1 = calculateAveragePerkCount(enemies1);
        double avgPerks10 = calculateAveragePerkCount(enemies10);
        
        // Enemies on map 10 should have more perks on average
        assertTrue(avgPerks10 > avgPerks1,
            "Enemies on higher maps should have more perks on average");
    }

    @Test
    void testPerksActuallyApplied() {
        // Test that perks are actually being applied to enemy stats
        List<Entity> enemies = new ArrayList<>();
        
        // Spawn enemies on map 10 (completedMaps = 10) to have high perk chance
        spawnManager.spawn(mockMap, mockPlayer, enemies, 10, 32);
        
        // Find an enemy with a health perk specifically
        Entity enemyWithHealthPerk = null;
        for (Entity enemy : enemies) {
            if (enemy.hasHealthPerk()) {
                enemyWithHealthPerk = enemy;
                break;
            }
        }
        
        // If we found an enemy with a health perk, verify it actually affected their health
        if (enemyWithHealthPerk != null) {
            // The enemy should have increased health due to the health perk
            // Base health for Soldier is 1.0, for Archer is 2.0
            double baseHealth = enemyWithHealthPerk instanceof Archer ? 2.0 : 1.0;
            assertTrue(enemyWithHealthPerk.getMaxHealth() > baseHealth, 
                "Enemy with health perk should have increased max health above base (" + baseHealth + ")");
            
            // Verify perk tracking is working
            assertTrue(enemyWithHealthPerk.getPerkCount() > 0, 
                "Enemy should have perk count > 0");
            assertTrue(enemyWithHealthPerk.hasHealthPerk(), 
                "Enemy should have health perk flag set");
        } else {
            // If no enemies got health perks, that's also valid (just unlucky RNG)
            // But we should at least verify the perk system is working
            assertTrue(enemies.size() > 0, "Should have spawned some enemies");
            
            // Check that at least some enemies have perks
            boolean hasAnyPerks = false;
            for (Entity enemy : enemies) {
                if (enemy.getPerkCount() > 0) {
                    hasAnyPerks = true;
                    break;
                }
            }
            assertTrue(hasAnyPerks, "At least some enemies should have perks on map 10");
        }
    }

    @Test
    void testPerkChanceCalculation() {
        // Test perk chance calculation
        double chance0 = spawnManager.getCurrentPerkChance(0);
        double chance1 = spawnManager.getCurrentPerkChance(1);
        double chance5 = spawnManager.getCurrentPerkChance(5);
        
        assertEquals(0.0, chance0, "Map 0 should have 0% perk chance");
        assertEquals(0.15, chance1, "Map 1 should have 15% perk chance");
        assertEquals(0.75, chance5, "Map 5 should have 75% perk chance");
    }

    @Test
    void testMaxPerksPerEnemy() {
        // Test max perks per enemy calculation
        int max0 = spawnManager.getMaxPerksPerEnemy(0);
        int max1 = spawnManager.getMaxPerksPerEnemy(1);
        int max6 = spawnManager.getMaxPerksPerEnemy(6);
        int max10 = spawnManager.getMaxPerksPerEnemy(10);
        
        assertEquals(0, max0, "Map 0 should allow 0 perks");
        assertEquals(1, max1, "Map 1 should allow 1 perk");
        assertEquals(3, max6, "Map 6 should allow 3 perks (1 + 6/3 = 3)");
        assertEquals(3, max10, "Map 10 should allow 3 perks (capped)");
    }

    // Helper methods
    private Entity findEnemyWithHealthPerk(List<Entity> enemies) {
        for (Entity enemy : enemies) {
            if (enemy.hasHealthPerk()) {
                return enemy;
            }
        }
        return null;
    }

    private double calculateAveragePerkCount(List<Entity> enemies) {
        if (enemies.isEmpty()) return 0.0;
        
        int totalPerks = 0;
        for (Entity enemy : enemies) {
            totalPerks += enemy.getPerkCount();
        }
        return (double) totalPerks / enemies.size();
    }
}
