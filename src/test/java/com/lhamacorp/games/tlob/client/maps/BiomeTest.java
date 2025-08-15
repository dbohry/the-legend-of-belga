package com.lhamacorp.games.tlob.client.maps;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

/**
 * Tests for the biome system, including biome progression and map generation.
 */
public class BiomeTest {
    
    @Test
    public void testBiomeProgression() {
        Biome[] biomes = Biome.values();
        assertEquals(4, biomes.length, "Should have 4 biomes");
        
        // Test biome cycling
        Biome current = Biome.MEADOWS;
        assertEquals(Biome.FOREST, current.getNext(), "MEADOWS should cycle to FOREST");
        
        current = Biome.FOREST;
        assertEquals(Biome.CAVE, current.getNext(), "FOREST should cycle to CAVE");
        
        current = Biome.CAVE;
        assertEquals(Biome.DESERT, current.getNext(), "CAVE should cycle to DESERT");
        
        current = Biome.DESERT;
        assertEquals(Biome.MEADOWS, current.getNext(), "DESERT should cycle back to MEADOWS");
    }
    
    @Test
    public void testBiomeFromName() {
        assertEquals(Biome.MEADOWS, Biome.fromName("meadows"), "Should find meadows biome");
        assertEquals(Biome.FOREST, Biome.fromName("FOREST"), "Should find forest biome (case insensitive)");
        assertEquals(Biome.CAVE, Biome.fromName("cave"), "Should find cave biome");
        assertEquals(Biome.DESERT, Biome.fromName("Desert"), "Should find desert biome (case insensitive)");
        
        // Test fallback
        assertEquals(Biome.MEADOWS, Biome.fromName("invalid"), "Should fallback to meadows for invalid names");
    }
    
    @Test
    public void testBiomeParameters() {
        Biome meadows = Biome.MEADOWS;
        assertEquals(0.45, meadows.getWallDensity(), 0.01, "Meadows should have 0.45 wall density");
        assertEquals(2500, meadows.getCarveSteps(), "Meadows should have 2500 carve steps");
        assertEquals(0.6, meadows.getPlantDensity(), 0.01, "Meadows should have 0.6 plant density");
        
        Biome forest = Biome.FOREST;
        assertEquals(0.55, forest.getWallDensity(), 0.01, "Forest should have 0.55 wall density");
        assertEquals(3000, forest.getCarveSteps(), "Forest should have 3000 carve steps");
        assertEquals(0.7, forest.getPlantDensity(), 0.01, "Forest should have 0.7 plant density");
    }
    
    @Test
    public void testMapGeneratorWithBiome() {
        Random rng = new Random(42L); // Fixed seed for deterministic testing
        MapGenerator generator = new MapGenerator(20, 15, Biome.FOREST, rng);
        int[][] tiles = generator.generate();
        
        assertNotNull(tiles, "Generated tiles should not be null");
        assertEquals(20, tiles.length, "Should have correct width");
        assertEquals(15, tiles[0].length, "Should have correct height");
        

        
        // Check that edge walls are present
        for (int x = 0; x < 20; x++) {
            assertEquals(TileMap.EDGE_WALL, tiles[x][0], "Top edge should be wall");
            assertEquals(TileMap.EDGE_WALL, tiles[x][14], "Bottom edge should be wall");
        }
        for (int y = 0; y < 15; y++) {
            assertEquals(TileMap.EDGE_WALL, tiles[0][y], "Left edge should be wall");
            assertEquals(TileMap.EDGE_WALL, tiles[19][y], "Right edge should be wall");
        }
    }
    
    @Test
    public void testTileMapWithBiome() {
        int[][] tiles = new int[10][10];
        // Fill with forest tiles
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x == 0 || y == 0 || x == 9 || y == 9) {
                    tiles[x][y] = TileMap.EDGE_WALL;
                } else {
                    tiles[x][y] = TileMap.FLOOR_FOREST_GROUND;
                }
            }
        }
        
        Random rng = new Random(42L);
        TileMap tileMap = new TileMap(tiles, Biome.FOREST, rng);
        
        assertEquals(Biome.FOREST, tileMap.getBiome(), "TileMap should have correct biome");
        assertFalse(tileMap.isWall(5, 5), "Center should not be wall");
        assertTrue(tileMap.isWall(0, 0), "Corner should be wall");
    }
}
