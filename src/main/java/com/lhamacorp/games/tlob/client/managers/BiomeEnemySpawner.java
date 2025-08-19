package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Golen;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.maps.Biome;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.util.List;
import java.util.Random;

/**
 * Handles spawning biome-specific enemies with different characteristics
 * and spawn patterns based on the current biome.
 */
public class BiomeEnemySpawner extends SpawnManager {
    
    /**
     * Enemy type enumeration for consistent weight management
     */
    public enum EnemyType {
        SOLDIER,
        ARCHER,
        GOLEN
    }
    
    /**
     * Biome-specific enemy spawn weights matrix.
     * Each biome has weights for each enemy type.
     * Weights are relative probabilities (higher = more likely).
     * 
     * Format: [BIOME][ENEMY_TYPE] = weight
     */
    private static final double[][] BIOME_ENEMY_WEIGHTS = {
        // MEADOWS biome weights
        {8.0, 2.0, 0.0},  // Soldier: 8, Archer: 2, Golen: 0 (disabled in meadows)
        
        // FOREST biome weights  
        {4.0, 6.0, 0.0},  // Soldier: 4, Archer: 6, Golen: 0 (disabled in forest)
        
        // CAVE biome weights
        {6.0, 4.0, 0.0},  // Soldier: 6, Archer: 4, Golen: 0 (disabled in cave)
        
        // DESERT biome weights
        {5.0, 5.0, 0.0},  // Soldier: 5, Archer: 5, Golen: 0 (disabled in desert)
        
        // VULCAN biome weights
        {7.0, 3.0, 0.0}   // Soldier: 7, Archer: 3, Golen: 0 (disabled in vulcan)
    };
    
    /**
     * Biome-specific enemy count multipliers.
     * These affect the total number of enemies spawned in each biome.
     */
    private static final double[] BIOME_ENEMY_MULTIPLIERS = {
        1.0,   // MEADOWS: standard count
        1.2,   // FOREST: more enemies
        0.8,   // CAVE: fewer enemies  
        1.1,   // DESERT: slightly more
        1.3    // VULCAN: more enemies (challenging)
    };
    
    // Golen spawning configuration - using parent class constants
    
    public BiomeEnemySpawner(Weapon enemyWeapon) {
        super(enemyWeapon);
    }
    
    public BiomeEnemySpawner(Weapon enemyWeapon, Random rng) {
        super(enemyWeapon, rng);
    }
    
    @Override
    public void spawn(TileMap map, Player player, List<Entity> out, int completedMaps, int tileSize) {
        Biome biome = map.getBiome();
        
        // Calculate base enemy count (same as SpawnManager)
        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int totalCount = Math.max(1, (int) (base * mult));
        
        // Apply biome-specific adjustments
        totalCount = applyBiomeAdjustments(totalCount, biome);
        
        out.clear();
        
        // Determine Golen spawning logic
        int golenCount = calculateGolenSpawnCount(totalCount);
        // Each Golen replaces 5 regular enemies
        int regularEnemyCount = totalCount - (golenCount * getGolenReplacementRatio());
        
        // Spawn Golen enemies first (if any)
        for (int i = 0; i < golenCount; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                double x = pos[0] * tileSize + tileSize / 2.0;
                double y = pos[1] * tileSize + tileSize / 2.0;
                
                Entity golen = new Golen(x, y, getEnemyWeapon());
                // Golen get 5 perks by default to make them elite enemies
                applyGolenPerks(golen, completedMaps);
                out.add(golen);
            }
        }
        
        // Spawn regular enemies (Soldiers and Archers)
        for (int i = 0; i < regularEnemyCount; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                double x = pos[0] * tileSize + tileSize / 2.0;
                double y = pos[1] * tileSize + tileSize / 2.0;
                
                Entity enemy = spawnBiomeEnemy(biome, x, y);
                
                // Apply perks based on map completion
                applyEnemyPerks(enemy, completedMaps);
                
                out.add(enemy);
            }
        }
    }
    
    /**
     * Applies biome-specific adjustments to enemy count.
     */
    private int applyBiomeAdjustments(int baseCount, Biome biome) {
        double multiplier = BIOME_ENEMY_MULTIPLIERS[biome.ordinal()];
        return Math.max(1, (int) (baseCount * multiplier));
    }
    
    /**
     * Spawns a single enemy appropriate for the given biome.
     */
    private Entity spawnBiomeEnemy(Biome biome, double x, double y) {
        // Choose enemy type based on biome weights
        double rand = rng.nextDouble();
        double soldierWeight = getEnemyTypeWeightInternal(biome, EnemyType.SOLDIER);
        double archerWeight = getEnemyTypeWeightInternal(biome, EnemyType.ARCHER);
        
        // Normalize weights to probabilities
        double totalWeight = soldierWeight + archerWeight;
        double soldierProb = soldierWeight / totalWeight;
        
        if (rand < soldierProb) {
            return new Soldier(x, y, getEnemyWeapon());
        } else {
            return new Archer(x, y, getEnemyWeapon());
        }
    }
    
    /**
     * Gets the weight for a specific enemy type in a specific biome.
     */
    private double getEnemyTypeWeightInternal(Biome biome, EnemyType enemyType) {
        return BIOME_ENEMY_WEIGHTS[biome.ordinal()][enemyType.ordinal()];
    }
    

    
    /**
     * Gets the enemy weapon for spawning.
     */
    private Weapon getEnemyWeapon() {
        // This is a workaround since we can't access the private field
        // In a real implementation, we'd need to make this field protected in SpawnManager
        return new com.lhamacorp.games.tlob.client.weapons.Sword(2, 28, 12, 10, 16);
    }
    
    // ===== Configuration methods for easy tweaking =====
    
    /**
     * Updates the weight for a specific enemy type in a specific biome.
     * Useful for runtime configuration or difficulty adjustments.
     */
    public static void setEnemyTypeWeight(Biome biome, EnemyType enemyType, double weight) {
        BIOME_ENEMY_WEIGHTS[biome.ordinal()][enemyType.ordinal()] = Math.max(0.0, weight);
    }
    
    /**
     * Updates the enemy count multiplier for a specific biome.
     */
    public static void setBiomeEnemyMultiplier(Biome biome, double multiplier) {
        BIOME_ENEMY_MULTIPLIERS[biome.ordinal()] = Math.max(0.1, multiplier);
    }
    
    /**
     * Gets the current weight for a specific enemy type in a specific biome.
     */
    public static double getEnemyTypeWeight(Biome biome, EnemyType enemyType) {
        return BIOME_ENEMY_WEIGHTS[biome.ordinal()][enemyType.ordinal()];
    }
    
    /**
     * Gets the current enemy count multiplier for a specific biome.
     */
    public static double getBiomeEnemyMultiplier(Biome biome) {
        return BIOME_ENEMY_MULTIPLIERS[biome.ordinal()];
    }
    
    /**
     * Resets all weights to their default values.
     */
    public static void resetToDefaults() {
        // Reset biome enemy weights
        BIOME_ENEMY_WEIGHTS[0] = new double[]{8.0, 2.0, 0.0}; // MEADOWS
        BIOME_ENEMY_WEIGHTS[1] = new double[]{4.0, 6.0, 0.0}; // FOREST
        BIOME_ENEMY_WEIGHTS[2] = new double[]{6.0, 4.0, 0.0}; // CAVE
        BIOME_ENEMY_WEIGHTS[3] = new double[]{5.0, 5.0, 0.0}; // DESERT
        BIOME_ENEMY_WEIGHTS[4] = new double[]{7.0, 3.0, 0.0}; // VULCAN
        
        // Reset biome multipliers
        BIOME_ENEMY_MULTIPLIERS[0] = 1.0;  // MEADOWS
        BIOME_ENEMY_MULTIPLIERS[1] = 1.2;  // FOREST
        BIOME_ENEMY_MULTIPLIERS[2] = 0.8;  // CAVE
        BIOME_ENEMY_MULTIPLIERS[3] = 1.1;  // DESERT
        BIOME_ENEMY_MULTIPLIERS[4] = 1.3;  // VULCAN
    }
}
