package com.lhamacorp.games.tlob.client.managers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

/**
 * Tests for the TextureManager, specifically focusing on meadow biome textures.
 */
public class TextureManagerTest {
    
    @Test
    public void testMeadowTexturesGenerated() {
        // Test that meadow textures are generated and not null
        BufferedImage meadowGrass = TextureManager.getMeadowGrassTexture();
        BufferedImage meadowDirt = TextureManager.getMeadowDirtTexture();
        BufferedImage meadowPlants = TextureManager.getMeadowPlantsTexture();
        BufferedImage meadowFlowers = TextureManager.getMeadowFlowersTexture();
        
        assertNotNull(meadowGrass, "Meadow grass texture should not be null");
        assertNotNull(meadowDirt, "Meadow dirt texture should not be null");
        assertNotNull(meadowPlants, "Meadow plants texture should not be null");
        assertNotNull(meadowFlowers, "Meadow flowers texture should not be null");
        
        // Test that textures have expected dimensions
        assertEquals(32, meadowGrass.getWidth(), "Meadow grass should have width 32");
        assertEquals(32, meadowGrass.getHeight(), "Meadow grass should have height 32");
        assertEquals(32, meadowDirt.getWidth(), "Meadow dirt should have width 32");
        assertEquals(32, meadowDirt.getHeight(), "Meadow dirt should have height 32");
        assertEquals(32, meadowPlants.getWidth(), "Meadow plants should have width 32");
        assertEquals(32, meadowPlants.getHeight(), "Meadow plants should have height 32");
        assertEquals(32, meadowFlowers.getWidth(), "Meadow flowers should have width 32");
        assertEquals(32, meadowFlowers.getHeight(), "Meadow flowers should have height 32");
    }
    
    @Test
    public void testMeadowTexturesDifferentFromGeneric() {
        // Test that meadow textures are different from generic textures
        BufferedImage meadowGrass = TextureManager.getMeadowGrassTexture();
        BufferedImage meadowDirt = TextureManager.getMeadowDirtTexture();
        BufferedImage meadowPlants = TextureManager.getMeadowPlantsTexture();
        BufferedImage meadowFlowers = TextureManager.getMeadowFlowersTexture();
        
        BufferedImage genericGrass = TextureManager.getGrassTexture();
        BufferedImage genericDirt = TextureManager.getDirtTexture();
        BufferedImage genericPlants = TextureManager.getPlantsTexture();
        
        // Meadow grass reuses generic grass texture (correct for caching)
        assertEquals(meadowGrass, genericGrass, "Meadow grass should reuse generic grass texture");
        
        // Meadow dirt, plants, and flowers should be different from generic ones
        assertNotEquals(meadowDirt, genericDirt, "Meadow dirt should be different from generic dirt");
        assertNotEquals(meadowPlants, genericPlants, "Meadow plants should be different from generic plants");
        
        // Meadow flowers should be unique
        assertNotEquals(meadowFlowers, meadowGrass, "Meadow flowers should be different from meadow grass");
        assertNotEquals(meadowFlowers, meadowPlants, "Meadow flowers should be different from meadow plants");
    }
}
