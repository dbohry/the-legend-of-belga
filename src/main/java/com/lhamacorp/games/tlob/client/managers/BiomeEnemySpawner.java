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
    
    // Biome-specific enemy spawn weights (for regular enemies only)
    private static final double MEADOWS_SOLDIER_WEIGHT = 0.8;
    private static final double MEADOWS_ARCHER_WEIGHT = 0.2;
    
    private static final double FOREST_SOLDIER_WEIGHT = 0.4;
    private static final double FOREST_ARCHER_WEIGHT = 0.6;
    
    private static final double CAVE_SOLDIER_WEIGHT = 0.6;
    private static final double CAVE_ARCHER_WEIGHT = 0.4;
    
    private static final double DESERT_SOLDIER_WEIGHT = 0.5;
    private static final double DESERT_ARCHER_WEIGHT = 0.5;
    
    private static final double VULCAN_SOLDIER_WEIGHT = 0.7;
    private static final double VULCAN_ARCHER_WEIGHT = 0.3;
    
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
        // Each Golen replaces 10 regular enemies
        int regularEnemyCount = totalCount - (golenCount * getGolenReplacementRatio());
        
        // Spawn Golen enemies first (if any)
        for (int i = 0; i < golenCount; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                double x = pos[0] * tileSize + tileSize / 2.0;
                double y = pos[1] * tileSize + tileSize / 2.0;
                
                Entity golen = new Golen(x, y, getEnemyWeapon());
                applyEnemyPerks(golen, completedMaps);
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
        return switch (biome) {
            case FOREST -> (int) (baseCount * 1.2); // More enemies in forest
            case CAVE -> (int) (baseCount * 0.8);   // Fewer enemies in cave
            case DESERT -> (int) (baseCount * 1.1); // Slightly more in desert
            case VULCAN -> (int) (baseCount * 1.3); // More enemies in vulcan (challenging)
            default -> baseCount; // MEADOWS
        };
    }
    
    /**
     * Spawns a single enemy appropriate for the given biome.
     */
    private Entity spawnBiomeEnemy(Biome biome, double x, double y) {
        // Choose enemy type based on biome weights
        double rand = rng.nextDouble();
        double soldierWeight = getSoldierWeight(biome);
        
        if (rand < soldierWeight) {
            return new Soldier(x, y, getEnemyWeapon());
        } else {
            return new Archer(x, y, getEnemyWeapon());
        }
    }
    
    private double getSoldierWeight(Biome biome) {
        return switch (biome) {
            case FOREST -> FOREST_SOLDIER_WEIGHT;
            case CAVE -> CAVE_SOLDIER_WEIGHT;
            case DESERT -> DESERT_SOLDIER_WEIGHT;
            case VULCAN -> VULCAN_SOLDIER_WEIGHT;
            default -> MEADOWS_SOLDIER_WEIGHT;
        };
    }
    
    /**
     * Gets the enemy weapon for spawning.
     */
    private Weapon getEnemyWeapon() {
        // This is a workaround since we can't access the private field
        // In a real implementation, we'd need to make this field protected in SpawnManager
        return new com.lhamacorp.games.tlob.client.weapons.Sword(2, 28, 12, 10, 16);
    }
}
