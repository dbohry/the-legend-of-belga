package com.lhamacorp.games.tlob.client.managers.renderers;

import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class HudRendererTest {

    private HudRenderer hudRenderer;
    private Player player;
    private Graphics2D graphics;

    @BeforeEach
    void setUp() {
        // Create a test font
        Font testFont = new Font("Arial", Font.PLAIN, 12);
        hudRenderer = new HudRenderer(testFont);
        
        // Create a test player with a sword
        Sword sword = new Sword(10, 20, 5, 10, 20);
        player = new Player(100, 100, sword);
        
        // Create a test graphics context
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
    }

    @Test
    void testHudRendererCreation() {
        assertNotNull(hudRenderer, "HUD renderer should be created successfully");
    }

    @Test
    void testPlayerHealthAndStamina() {
        // Test that player has expected initial values
        assertEquals(6.0, player.getMaxHealth(), "Player should have max health of 6.0");
        assertEquals(6.0, player.getMaxStamina(), "Player should have max stamina of 6.0");
        assertEquals(6.0, player.getHealth(), "Player should start with full health");
        assertEquals(6.0, player.getStamina(), "Player should start with full stamina");
    }

    @Test
    void testPlayerShieldLogic() {
        // Test that shield logic is preserved
        assertEquals(0.0, player.getMaxShield(), "Player should start with no shield");
        assertEquals(0.0, player.getShield(), "Player should start with no shield");
        
        // Test shield increase
        player.increaseShield();
        assertEquals(1.0, player.getMaxShield(), "Player shield should increase to 1.0");
        // Note: increaseShield only increases maxShield, not current shield
        assertEquals(0.0, player.getShield(), "Player shield should remain at 0 until restored");
        
        // Test setting shield to max
        player.setShield(player.getMaxShield());
        assertEquals(1.0, player.getShield(), "Player shield should be set to max");
    }

    @Test
    void testHudRendererDrawMethod() {
        // Test that the draw method doesn't throw exceptions
        assertDoesNotThrow(() -> {
            hudRenderer.draw(graphics, player, 10, 10);
        }, "HUD renderer draw method should not throw exceptions");
    }

    @Test
    void testPlayerDamageWithShield() {
        // Give player a shield
        player.increaseShield();
        player.setShield(1.0);
        
        // Test damage absorption by shield
        double initialHealth = player.getHealth();
        double initialShield = player.getShield();
        
        player.damage(0.5);
        
        assertEquals(initialHealth, player.getHealth(), "Health should not decrease when shield absorbs damage");
        assertEquals(initialShield - 0.5, player.getShield(), "Shield should absorb damage");
        
        // Test damage exceeding shield
        player.damage(1.0);
        assertEquals(initialHealth - 0.5, player.getHealth(), "Health should decrease after shield is depleted");
        assertEquals(0.0, player.getShield(), "Shield should be depleted");
    }

    @Test
    void testShieldDisplayLogic() {
        // Test that shield display logic works correctly
        assertEquals(0.0, player.getMaxShield(), "Player should start with no shield");
        assertEquals(0.0, player.getShield(), "Player should start with no shield");
        
        // Add shield and set it
        player.increaseShield();
        player.setShield(2.0);
        
        assertEquals(1.0, player.getMaxShield(), "Player max shield should be 1.0");
        assertEquals(1.0, player.getShield(), "Player shield should be clamped to max value");
        
        // Test shield with health
        player.setHealth(3.0); // Set health to 3.0/6.0
        assertEquals(3.0, player.getHealth(), "Player health should be 3.0");
        assertEquals(1.0, player.getShield(), "Player shield should remain 1.0");
        
        // Verify total protection
        double totalProtection = player.getHealth() + player.getShield();
        assertEquals(4.0, totalProtection, "Total protection should be 4.0 (3.0 health + 1.0 shield)");
    }

    @Test
    void testPlayerStaminaRegeneration() {
        // Test stamina drain and regeneration
        player.setStamina(1.0); // Set low stamina
        
        // Test that stamina can be set and retrieved correctly
        assertEquals(1.0, player.getStamina(), "Player stamina should be set to 1.0");
        
        // Test that stamina can be restored
        player.setStamina(player.getMaxStamina());
        assertEquals(6.0, player.getStamina(), "Player stamina should be restored to max");
        
        // Test that stamina respects max value
        player.setStamina(10.0); // Try to set above max
        assertEquals(6.0, player.getStamina(), "Player stamina should not exceed max value");
    }

    @Test
    void testHudRendererWithShield() {
        // Test that HUD renderer can handle shield display
        player.increaseShield(); // Give player a shield
        player.setShield(1.0);   // Set shield to max
        
        // Verify shield is active
        assertEquals(1.0, player.getMaxShield(), "Player should have max shield of 1.0");
        assertEquals(1.0, player.getShield(), "Player should have current shield of 1.0");
        
        // Test that HUD renderer can draw with shield
        assertDoesNotThrow(() -> {
            hudRenderer.draw(graphics, player, 10, 10);
        }, "HUD renderer should handle shield display without errors");
    }
}
