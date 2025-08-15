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
        assertEquals(5, biomes.length, "Should have 5 biomes");
        
        // Test biome cycling
        Biome current = Biome.MEADOWS;
        assertEquals(Biome.FOREST, current.getNext(), "MEADOWS should cycle to FOREST");
        
        current = Biome.FOREST;
        assertEquals(Biome.CAVE, current.getNext(), "FOREST should cycle to CAVE");
        
        current = Biome.CAVE;
        assertEquals(Biome.DESERT, current.getNext(), "CAVE should cycle to DESERT");
        
        current = Biome.DESERT;
        assertEquals(Biome.VULCAN, current.getNext(), "DESERT should cycle to VULCAN");
        
        current = Biome.VULCAN;
        assertEquals(Biome.MEADOWS, current.getNext(), "VULCAN should cycle back to MEADOWS");
    }
    
    @Test
    public void testBiomeFromName() {
        assertEquals(Biome.MEADOWS, Biome.fromName("meadows"), "Should find meadows biome");
        assertEquals(Biome.FOREST, Biome.fromName("FOREST"), "Should find forest biome (case insensitive)");
        assertEquals(Biome.CAVE, Biome.fromName("cave"), "Should find cave biome");
        assertEquals(Biome.DESERT, Biome.fromName("Desert"), "Should find desert biome (case insensitive)");
        assertEquals(Biome.VULCAN, Biome.fromName("vulcan"), "Should find vulcan biome");
        
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
        
        Biome vulcan = Biome.VULCAN;
        assertEquals(0.40, vulcan.getWallDensity(), 0.01, "Vulcan should have 0.40 wall density");
        assertEquals(2200, vulcan.getCarveSteps(), "Vulcan should have 2200 carve steps");
        assertEquals(0.2, vulcan.getPlantDensity(), 0.01, "Vulcan should have 0.2 plant density");
        assertEquals(0.25, vulcan.getFeatureDensity(), 0.01, "Vulcan should have 0.25 feature density");
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
    public void testVulcanBiomeMapGeneration() {
        Random rng = new Random(42L); // Fixed seed for deterministic testing
        MapGenerator generator = new MapGenerator(20, 15, Biome.VULCAN, rng);
        int[][] tiles = generator.generate();
        
        assertNotNull(tiles, "Generated vulcan tiles should not be null");
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
        
        // Check that vulcan-specific tiles are present
        boolean hasVulcanTiles = false;
        for (int x = 1; x < 19; x++) {
            for (int y = 1; y < 14; y++) {
                if (tiles[x][y] == TileMap.FLOOR_VULCAN_ROCK || 
                    tiles[x][y] == TileMap.FLOOR_VULCAN_LAVA || 
                    tiles[x][y] == TileMap.FLOOR_VULCAN_ASH) {
                    hasVulcanTiles = true;
                    break;
                }
            }
        }
        assertTrue(hasVulcanTiles, "Vulcan biome should generate vulcan-specific tiles");
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
    
    @Test
    public void testVulcanTileMapWithBiome() {
        int[][] tiles = new int[10][10];
        // Fill with vulcan tiles
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x == 0 || y == 0 || x == 9 || y == 9) {
                    tiles[x][y] = TileMap.EDGE_WALL;
                } else {
                    tiles[x][y] = TileMap.FLOOR_VULCAN_ROCK;
                }
            }
        }
        
        Random rng = new Random(42L);
        TileMap tileMap = new TileMap(tiles, Biome.VULCAN, rng);
        
        assertEquals(Biome.VULCAN, tileMap.getBiome(), "TileMap should have correct vulcan biome");
        assertFalse(tileMap.isWall(5, 5), "Center should not be wall");
        assertTrue(tileMap.isWall(0, 0), "Corner should be wall");
        
        // Test vulcan-specific tile hiding
        assertTrue(TileMap.isHidingTileId(TileMap.FLOOR_VULCAN_ASH), "Vulcan ash should be hiding tile");
        assertTrue(TileMap.isHidingTileId(TileMap.WALL_VULCAN_CRYSTAL), "Vulcan crystal should be hiding tile");
    }
}
