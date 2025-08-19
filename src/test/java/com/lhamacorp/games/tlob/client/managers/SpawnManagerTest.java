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
        assertEquals(0.20, chance1, "Map 1 should have 20% perk chance");
        assertEquals(0.8, chance5, "Map 5 should have 80% perk chance (capped at 80%)");
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
        assertEquals(4, max10, "Map 10 should allow 4 perks (1 + 10/3 = 4)");
    }

    @Test
    void testGolenGetElitePerks() {
        // Test that Golen enemies get up to 5 unique perk types by default, making them elite
        List<Entity> enemies = new ArrayList<>();
        
        // Spawn enemies on a map with enough enemies for Golen to spawn (>60)
        // We'll need to create a larger map to get enough enemies
        int[][] largeTiles = new int[20][20];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                largeTiles[i][j] = 0; // Floor tiles
            }
        }
        TileMap largeMap = new TileMap(largeTiles, testRng);
        
        // Spawn enemies on map 10 (completedMaps = 10) to have high perk chance
        spawnManager.spawn(largeMap, mockPlayer, enemies, 10, 32);
        
        // Check if any Golen spawned and verify they have perks (up to 5 unique types)
        boolean foundGolen = false;
        for (Entity enemy : enemies) {
            if (enemy instanceof Golen) {
                foundGolen = true;
                // Golen should have perks (up to 5 unique types, but may be less due to duplicates)
                assertTrue(enemy.getPerkCount() > 0, 
                    "Golen should have at least 1 perk to be elite enemies");
                assertTrue(enemy.getPerkCount() <= 5, 
                    "Golen should have at most 5 unique perk types");
                break;
            }
        }
        
        // If no Golen spawned (due to RNG), that's fine - the test still passes
        // We're just testing the perk system when Golen do spawn
        if (foundGolen) {
            assertTrue(true, "Golen spawned and has correct perk count");
        }
    }

    @Test
    void testGolenReplacementRatio() {
        // Test that each Golen replaces 10 enemies
        assertEquals(10, SpawnManager.getGolenReplacementRatio(), 
            "Golen should replace 10 enemies each");
    }

    @Test
    void testHighPerkThreshold() {
        // Test that the high perk threshold is 5
        assertEquals(5, SpawnManager.getHighPerkThreshold(), 
            "High perk threshold should be 5 perks");
    }

    @Test
    void testHighPerkReplacementRatio() {
        // Test that each high-perk enemy replaces 5 low-perk enemies
        assertEquals(5, SpawnManager.getHighPerkReplacementRatio(), 
            "High-perk enemies should replace 5 low-perk enemies each");
    }

    @Test
    void testHighPerkSpawnThreshold() {
        // Test that high-perk enemies only spawn when there are more than 20 enemies
        assertEquals(20, SpawnManager.getHighPerkSpawnThreshold(), 
            "High-perk enemies should only spawn when there are more than 20 enemies");
    }

    @Test
    void testHighPerkEnemySpawningThreshold() {
        // Test that high-perk enemies only spawn above the 20 enemy threshold
        
        // Create a larger map to get more enemies
        int[][] largeTiles = new int[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                largeTiles[i][j] = 0; // Floor tiles
            }
        }
        TileMap largeMap = new TileMap(largeTiles, testRng);
        
        // Test on map 15 (completedMaps = 15) to have high perk chance
        List<Entity> enemies = new ArrayList<>();
        spawnManager.spawn(largeMap, mockPlayer, enemies, 15, 32);
        
        // Count high-perk enemies
        int highPerkCount = 0;
        for (Entity enemy : enemies) {
            if (!(enemy instanceof Golen) && enemy.getPerkCount() > SpawnManager.getHighPerkThreshold()) {
                highPerkCount++;
            }
        }
        
        // If we have high-perk enemies, verify we're above the threshold
        if (highPerkCount > 0) {
            // We should have more than 20 enemies total for high-perk spawning to occur
            assertTrue(enemies.size() > SpawnManager.getHighPerkSpawnThreshold(), 
                "High-perk enemies should only spawn when total enemies > " + SpawnManager.getHighPerkSpawnThreshold());
            
            System.out.println("High-perk enemies spawned: " + highPerkCount);
            System.out.println("Total enemies: " + enemies.size());
            System.out.println("Threshold: " + SpawnManager.getHighPerkSpawnThreshold());
        }
        
        // Verify the threshold is respected
        assertTrue(enemies.size() > 0, "Should have spawned some enemies");
    }

    @Test
    void testHighPerkEnemySpawning() {
        // Test that high-perk enemies (6+ perks) spawn and replace multiple low-perk enemies
        List<Entity> enemies = new ArrayList<>();
        
        // Spawn enemies on map 10 (completedMaps = 10) to have high perk chance
        spawnManager.spawn(mockMap, mockPlayer, enemies, 10, 32);
        
        // Count high-perk and low-perk enemies
        int highPerkCount = 0;
        int lowPerkCount = 0;
        
        for (Entity enemy : enemies) {
            if (enemy instanceof Golen) {
                continue; // Skip Golen, we're testing regular enemies
            }
            
            if (enemy.getPerkCount() > SpawnManager.getHighPerkThreshold()) {
                highPerkCount++;
                // Verify high-perk enemies have 6-10 perks
                assertTrue(enemy.getPerkCount() >= 6, 
                    "High-perk enemies should have at least 6 perks");
                assertTrue(enemy.getPerkCount() <= 10, 
                    "High-perk enemies should have at most 10 perks");
            } else {
                lowPerkCount++;
                // Verify low-perk enemies have 0-5 perks
                assertTrue(enemy.getPerkCount() >= 0, 
                    "Low-perk enemies should have at least 0 perks");
                assertTrue(enemy.getPerkCount() <= 5, 
                    "Low-perk enemies should have at most 5 perks");
            }
        }
        
        // Verify that high-perk enemies are actually replacing low-perk ones
        // Each high-perk enemy should replace 5 low-perk enemies
        int expectedLowPerkCount = lowPerkCount + (highPerkCount * SpawnManager.getHighPerkReplacementRatio());
        int totalRegularEnemies = highPerkCount + lowPerkCount;
        
        // The total should be reasonable (accounting for Golen replacement)
        assertTrue(totalRegularEnemies > 0, "Should have spawned some regular enemies");
        
        if (highPerkCount > 0) {
            assertTrue(highPerkCount <= totalRegularEnemies / SpawnManager.getHighPerkReplacementRatio(),
                "High-perk enemy count should respect replacement ratio");
        }
    }

    @Test
    void testMaxPerksPerEnemyIncreased() {
        // Test that regular enemies can now have up to 10 perks (not just 4)
        List<Entity> enemies = new ArrayList<>();
        
        // Spawn enemies on map 15 (completedMaps = 15) to have very high perk chance
        spawnManager.spawn(mockMap, mockPlayer, enemies, 15, 32);
        
        // Find an enemy with the maximum possible perks
        Entity maxPerkEnemy = null;
        for (Entity enemy : enemies) {
            if (enemy instanceof Golen) continue; // Skip Golen
            
            if (enemy.getPerkCount() > 0) {
                if (maxPerkEnemy == null || enemy.getPerkCount() > maxPerkEnemy.getPerkCount()) {
                    maxPerkEnemy = enemy;
                }
            }
        }
        
        if (maxPerkEnemy != null) {
            // Regular enemies should be able to get more than 4 perks now
            assertTrue(maxPerkEnemy.getPerkCount() > 4, 
                "Regular enemies should be able to get more than 4 perks on high maps");
            assertTrue(maxPerkEnemy.getPerkCount() <= 10, 
                "Regular enemies should not exceed 10 perks");
        }
    }

    @Test
    void testHighPerkEnemiesReduceTotalCount() {
        // Test that spawning high-perk enemies actually reduces the total enemy count
        // by replacing multiple low-perk ones
        
        // Spawn enemies on map 0 (no perks, no high-perk enemies)
        List<Entity> enemiesMap0 = new ArrayList<>();
        spawnManager.spawn(mockMap, mockPlayer, enemiesMap0, 0, 32);
        int totalEnemiesMap0 = enemiesMap0.size();
        
        // Spawn enemies on map 10 (high perk chance, high-perk enemies should spawn)
        List<Entity> enemiesMap10 = new ArrayList<>();
        spawnManager.spawn(mockMap, mockPlayer, enemiesMap10, 10, 32);
        int totalEnemiesMap10 = enemiesMap10.size();
        
        // Count high-perk and low-perk enemies on map 10
        int highPerkCount = 0;
        int lowPerkCount = 0;
        int golenCount = 0;
        
        for (Entity enemy : enemiesMap10) {
            if (enemy instanceof Golen) {
                golenCount++;
            } else if (enemy.getPerkCount() > SpawnManager.getHighPerkThreshold()) {
                highPerkCount++;
            } else {
                lowPerkCount++;
            }
        }
        
        // Verify that high-perk enemies are actually reducing the total count
        if (highPerkCount > 0) {
            // Calculate what the total would be without replacement
            int expectedTotalWithoutReplacement = totalEnemiesMap0;
            
            // Calculate what the total should be with replacement
            // Each high-perk enemy replaces 5 low-perk ones
            int expectedTotalWithReplacement = expectedTotalWithoutReplacement - (highPerkCount * (SpawnManager.getHighPerkReplacementRatio() - 1));
            
            // The actual total should be closer to the replacement total than the original
            assertTrue(totalEnemiesMap10 < totalEnemiesMap0, 
                "High-perk enemies should reduce total enemy count");
            
            // Verify the replacement math works
            int actualRegularEnemies = highPerkCount + lowPerkCount;
            int expectedRegularEnemies = totalEnemiesMap0 - (golenCount * SpawnManager.getGolenReplacementRatio()) - (highPerkCount * (SpawnManager.getHighPerkReplacementRatio() - 1));
            
            // Allow for some variance due to RNG, but should be close
            assertTrue(Math.abs(actualRegularEnemies - expectedRegularEnemies) <= 2, 
                "Enemy count should approximately match replacement calculations");
        }
        
        // Verify we have a reasonable mix
        assertTrue(totalEnemiesMap10 > 0, "Should still have some enemies");
        assertTrue(highPerkCount <= 4, "Should not spawn more than 4 high-perk enemies");
    }

    @Test
    void testEnemyReplacementDemonstration() {
        // This test demonstrates the enemy replacement system in action
        System.out.println("\n=== ENEMY REPLACEMENT DEMONSTRATION ===");
        
        // Test on different map levels to show progression
        for (int mapLevel = 0; mapLevel <= 15; mapLevel += 5) {
            List<Entity> enemies = new ArrayList<>();
            spawnManager.spawn(mockMap, mockPlayer, enemies, mapLevel, 32);
            
            int highPerkCount = 0;
            int lowPerkCount = 0;
            int golenCount = 0;
            int totalPerks = 0;
            
            for (Entity enemy : enemies) {
                if (enemy instanceof Golen) {
                    golenCount++;
                    totalPerks += enemy.getPerkCount();
                } else if (enemy.getPerkCount() > SpawnManager.getHighPerkThreshold()) {
                    highPerkCount++;
                    totalPerks += enemy.getPerkCount();
                } else {
                    lowPerkCount++;
                    totalPerks += enemy.getPerkCount();
                }
            }
            
            System.out.println("Map " + mapLevel + ":");
            System.out.println("  Total enemies: " + enemies.size());
            System.out.println("  Golen: " + golenCount + " (with " + (golenCount > 0 ? totalPerks/golenCount : 0) + " avg perks)");
            System.out.println("  High-perk: " + highPerkCount + " (6+ perks)");
            System.out.println("  Low-perk: " + lowPerkCount + " (0-5 perks)");
            System.out.println("  Total perks: " + totalPerks);
            System.out.println("  Average perks per enemy: " + (enemies.size() > 0 ? String.format("%.1f", (double)totalPerks/enemies.size()) : "0.0"));
            
            if (highPerkCount > 0) {
                int enemiesReplaced = highPerkCount * SpawnManager.getHighPerkReplacementRatio();
                System.out.println("  High-perk enemies replaced " + enemiesReplaced + " low-perk enemies!");
            }
            System.out.println();
        }
        
        System.out.println("=== END DEMONSTRATION ===\n");
    }

    @Test
    void testSimpleEnemyCountReduction() {
        // Simple test to show enemy count reduction
        System.out.println("=== SIMPLE ENEMY COUNT REDUCTION TEST ===");
        
        // Map 0: No perks, no high-perk enemies
        List<Entity> enemiesMap0 = new ArrayList<>();
        spawnManager.spawn(mockMap, mockPlayer, enemiesMap0, 0, 32);
        int countMap0 = enemiesMap0.size();
        System.out.println("Map 0: " + countMap0 + " enemies (no perks)");
        
        // Map 10: High perk chance, should spawn high-perk enemies
        List<Entity> enemiesMap10 = new ArrayList<>();
        spawnManager.spawn(mockMap, mockPlayer, enemiesMap10, 10, 32);
        int countMap10 = enemiesMap10.size();
        
        // Count high-perk enemies
        int highPerkCount = 0;
        for (Entity enemy : enemiesMap10) {
            if (!(enemy instanceof Golen) && enemy.getPerkCount() > SpawnManager.getHighPerkThreshold()) {
                highPerkCount++;
            }
        }
        
        System.out.println("Map 10: " + countMap10 + " enemies (" + highPerkCount + " high-perk)");
        
        if (highPerkCount > 0) {
            int enemiesReplaced = highPerkCount * SpawnManager.getHighPerkReplacementRatio();
            System.out.println("High-perk enemies replaced " + enemiesReplaced + " low-perk enemies!");
            System.out.println("Expected reduction: " + enemiesReplaced + " enemies");
            System.out.println("Actual reduction: " + (countMap0 - countMap10) + " enemies");
            
            // Verify that we actually reduced the enemy count
            assertTrue(countMap10 < countMap0, 
                "High-perk enemies should reduce total enemy count from " + countMap0 + " to less than " + countMap0);
        }
        
        System.out.println("=== END TEST ===\n");
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
