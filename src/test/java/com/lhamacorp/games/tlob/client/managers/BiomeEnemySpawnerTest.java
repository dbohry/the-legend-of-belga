package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Golen;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.maps.Biome;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests for the refactored BiomeEnemySpawner with weight matrix system.
 */
public class BiomeEnemySpawnerTest {
    
    private BiomeEnemySpawner spawner;
    private Random testRng;
    private TileMap mockTileMap;
    private Player mockPlayer;
    
    @BeforeEach
    public void setUp() {
        testRng = new Random(42L); // Fixed seed for deterministic testing
        spawner = new BiomeEnemySpawner(new Sword(2, 28, 12, 10, 16), testRng);
        
        // Reset to defaults before each test
        BiomeEnemySpawner.resetToDefaults();
    }
    
    @Test
    public void testDefaultWeightsAreCorrect() {
        // Test meadows biome weights
        assertEquals(8.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.MEADOWS, BiomeEnemySpawner.EnemyType.SOLDIER), 0.01);
        assertEquals(2.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.MEADOWS, BiomeEnemySpawner.EnemyType.ARCHER), 0.01);
        assertEquals(0.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.MEADOWS, BiomeEnemySpawner.EnemyType.GOLEN), 0.01);
        
        // Test forest biome weights
        assertEquals(4.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.SOLDIER), 0.01);
        assertEquals(6.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.ARCHER), 0.01);
        assertEquals(0.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.GOLEN), 0.01);
        
        // Test vulcan biome weights
        assertEquals(7.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.VULCAN, BiomeEnemySpawner.EnemyType.SOLDIER), 0.01);
        assertEquals(3.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.VULCAN, BiomeEnemySpawner.EnemyType.ARCHER), 0.01);
        assertEquals(0.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.VULCAN, BiomeEnemySpawner.EnemyType.GOLEN), 0.01);
    }
    
    @Test
    public void testDefaultBiomeMultipliersAreCorrect() {
        assertEquals(1.0, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.MEADOWS), 0.01);
        assertEquals(1.2, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.FOREST), 0.01);
        assertEquals(0.8, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.CAVE), 0.01);
        assertEquals(1.1, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.DESERT), 0.01);
        assertEquals(1.3, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.VULCAN), 0.01);
    }
    
    @Test
    public void testSetEnemyTypeWeight() {
        // Change forest archer weight
        BiomeEnemySpawner.setEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.ARCHER, 8.0);
        assertEquals(8.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.ARCHER), 0.01);
        
        // Verify other weights are unchanged
        assertEquals(4.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.SOLDIER), 0.01);
        assertEquals(0.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.GOLEN), 0.01);
    }
    
    @Test
    public void testSetBiomeEnemyMultiplier() {
        // Change cave multiplier
        BiomeEnemySpawner.setBiomeEnemyMultiplier(Biome.CAVE, 1.5);
        assertEquals(1.5, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.CAVE), 0.01);
        
        // Verify other multipliers are unchanged
        assertEquals(1.0, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.MEADOWS), 0.01);
        assertEquals(1.2, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.FOREST), 0.01);
    }
    
    @Test
    public void testWeightValidation() {
        // Test negative weight handling
        BiomeEnemySpawner.setEnemyTypeWeight(Biome.MEADOWS, BiomeEnemySpawner.EnemyType.SOLDIER, -5.0);
        assertEquals(0.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.MEADOWS, BiomeEnemySpawner.EnemyType.SOLDIER), 0.01);
        
        // Test negative multiplier handling
        BiomeEnemySpawner.setBiomeEnemyMultiplier(Biome.DESERT, -2.0);
        assertEquals(0.1, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.DESERT), 0.01);
    }
    
    @Test
    public void testResetToDefaults() {
        // Change some values
        BiomeEnemySpawner.setEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.ARCHER, 10.0);
        BiomeEnemySpawner.setBiomeEnemyMultiplier(Biome.CAVE, 2.0);
        
        // Reset to defaults
        BiomeEnemySpawner.resetToDefaults();
        
        // Verify values are back to defaults
        assertEquals(6.0, BiomeEnemySpawner.getEnemyTypeWeight(Biome.FOREST, BiomeEnemySpawner.EnemyType.ARCHER), 0.01);
        assertEquals(0.8, BiomeEnemySpawner.getBiomeEnemyMultiplier(Biome.CAVE), 0.01);
    }
    
    @Test
    public void testEnemyTypeEnumValues() {
        BiomeEnemySpawner.EnemyType[] types = BiomeEnemySpawner.EnemyType.values();
        assertEquals(3, types.length);
        
        assertTrue(contains(types, BiomeEnemySpawner.EnemyType.SOLDIER));
        assertTrue(contains(types, BiomeEnemySpawner.EnemyType.ARCHER));
        assertTrue(contains(types, BiomeEnemySpawner.EnemyType.GOLEN));
    }
    
    @Test
    public void testBiomeOrdinalMapping() {
        // Verify that biome ordinals match the weight matrix indices
        assertEquals(0, Biome.MEADOWS.ordinal());
        assertEquals(1, Biome.FOREST.ordinal());
        assertEquals(2, Biome.CAVE.ordinal());
        assertEquals(3, Biome.DESERT.ordinal());
        assertEquals(4, Biome.VULCAN.ordinal());
    }
    
    @Test
    public void testEnemyTypeOrdinalMapping() {
        // Verify that enemy type ordinals match the weight matrix indices
        assertEquals(0, BiomeEnemySpawner.EnemyType.SOLDIER.ordinal());
        assertEquals(1, BiomeEnemySpawner.EnemyType.ARCHER.ordinal());
        assertEquals(2, BiomeEnemySpawner.EnemyType.GOLEN.ordinal());
    }
    
    @Test
    public void testWeightMatrixStructure() {
        // Test that all biomes have weights for all enemy types
        for (Biome biome : Biome.values()) {
            for (BiomeEnemySpawner.EnemyType enemyType : BiomeEnemySpawner.EnemyType.values()) {
                double weight = BiomeEnemySpawner.getEnemyTypeWeight(biome, enemyType);
                assertTrue(weight >= 0.0, "Weight should be non-negative");
                assertTrue(weight <= 10.0, "Weight should be reasonable (<= 10.0)");
            }
        }
    }
    
    @Test
    public void testMultiplierRange() {
        // Test that all biome multipliers are reasonable
        for (Biome biome : Biome.values()) {
            double multiplier = BiomeEnemySpawner.getBiomeEnemyMultiplier(biome);
            assertTrue(multiplier >= 0.1, "Multiplier should be >= 0.1");
            assertTrue(multiplier <= 3.0, "Multiplier should be reasonable (<= 3.0)");
        }
    }
    
    private boolean contains(BiomeEnemySpawner.EnemyType[] types, BiomeEnemySpawner.EnemyType type) {
        for (BiomeEnemySpawner.EnemyType t : types) {
            if (t == type) return true;
        }
        return false;
    }
}
