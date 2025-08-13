package com.lhamacorp.games.tlob.client.managers.renderers;

import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the StatsRenderer class.
 */
class StatsRendererTest {

    private StatsRenderer statsRenderer;
    private Player player;
    private Graphics2D graphics;

    @BeforeEach
    void setUp() {
        statsRenderer = new StatsRenderer();
        player = new Player(100, 100, new Sword(5, 30, 15, 12, 20));
        
        // Create a test graphics context
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
    }

    @Test
    void testStatsRendererCreation() {
        assertNotNull(statsRenderer);
    }

    @Test
    void testStatsRendererWithCustomFonts() {
        Font titleFont = new Font("Arial", Font.BOLD, 20);
        Font statFont = new Font("Arial", Font.PLAIN, 14);
        Font valueFont = new Font("Arial", Font.BOLD, 14);
        Font sectionFont = new Font("Arial", Font.BOLD, 16);
        
        StatsRenderer customRenderer = new StatsRenderer(titleFont, statFont, valueFont, sectionFont);
        assertNotNull(customRenderer);
    }

    @Test
    void testDrawMethodWithNullPlayer() {
        // Should not throw exception
        assertDoesNotThrow(() -> statsRenderer.draw(graphics, null));
    }

    @Test
    void testDrawMethodWithValidPlayer() {
        // Should not throw exception
        assertDoesNotThrow(() -> statsRenderer.draw(graphics, player));
    }

    @Test
    void testPlayerStatsAreAccessible() {
        // Verify that the player has the required stats for the renderer
        assertNotNull(player.getWeapon());
        assertEquals(5, player.getWeapon().getDamage());
        assertEquals(30, player.getWeapon().getReach());
        assertEquals(15, player.getWeapon().getWidth());
        assertEquals(12, player.getWeapon().getDuration());
        assertEquals(20, player.getWeapon().getCooldown());
    }
}
