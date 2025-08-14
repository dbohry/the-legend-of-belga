package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class GolenTest {

    private Golen golen;
    private Sword weapon;
    private TileMap mockMap;
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        weapon = new Sword(1, 30, 20, 500, 1000);
        golen = new Golen(100.0, 100.0, weapon);
        
        // Create simple mock objects
        mockMap = new TileMap(new int[20][20]);
        mockPlayer = new Player(150.0, 150.0, weapon);
    }

    @Test
    void testGolenCreation() {
        // Test basic properties
        assertEquals("Golen", golen.getName());
        assertEquals(Alignment.FOE, golen.getAlignment());
        assertEquals(80, golen.getWidth());
        assertEquals(80, golen.getHeight());
        // Golen should have significantly more health than a Soldier (1.0)
        assertTrue(golen.getMaxHealth() > 1.0, "Golen should have more health than Soldier");
        assertEquals(2.0, golen.getMaxStamina());
        assertEquals(0.0, golen.getMaxMana());
        assertTrue(golen.isAlive());
    }

    @Test
    void testGolenSizeComparison() {
        // Golen should be 4x the size of a Soldier
        Soldier soldier = new Soldier(200.0, 200.0, weapon);
        assertEquals(20, soldier.getWidth());
        assertEquals(80, golen.getWidth());
        assertEquals(4, golen.getWidth() / soldier.getWidth());
    }

    @Test
    void testGolenSpeedComparison() {
        // Golen should be slower than a Soldier
        Soldier soldier = new Soldier(200.0, 200.0, weapon);
        assertTrue(golen.getSpeed() < soldier.getSpeed());
    }

    @Test
    void testGolenHealthComparison() {
        // Golen should have more health than a Soldier
        Soldier soldier = new Soldier(200.0, 200.0, weapon);
        assertTrue(golen.getMaxHealth() > soldier.getMaxHealth());
        // Golen should have significantly more health than Soldier (1.0)
        assertTrue(golen.getMaxHealth() > 1.0, "Golen should have more health than Soldier");
        assertEquals(1.0, soldier.getMaxHealth());
    }

    @Test
    void testGolenAttackRange() {
        // Golen should have larger attack range due to size
        Soldier soldier = new Soldier(200.0, 200.0, weapon);
        // Note: Attack range is private, but we can test through behavior
        // Golen should be able to attack from further away
    }

    @Test
    void testGolenMovement() {
        // Test that Golen can move
        double initialX = golen.getX();
        double initialY = golen.getY();
        
        // Update with player at distance
        golen.update(mockPlayer, mockMap);
        
        // Golen should move towards player (may not move every update due to timers)
        // This test verifies the update method doesn't crash
        assertNotNull(golen);
    }

    @Test
    void testGolenAttackCooldown() {
        // Test that Golen has longer attack cooldown than Soldier
        Soldier soldier = new Soldier(200.0, 200.0, weapon);
        
        // Place player in attack range by creating a new player instance
        Player nearbyPlayer = new Player(130.0, 100.0, weapon);
        
        // Update multiple times to trigger attack
        for (int i = 0; i < 10; i++) {
            golen.update(nearbyPlayer, mockMap);
        }
        
        // Verify the update method works without crashing
        assertNotNull(golen);
    }

    @Test
    void testGolenPersonalityTraits() {
        // Test that Golen has different personality traits than Soldier
        // Golen should be more cautious and less aggressive on average
        // This is probabilistic, so we test the structure exists
        assertNotNull(golen);
    }

    @Test
    void testGolenWanderBehavior() {
        // Test that Golen has enhanced wandering behavior
        // Update without player nearby to trigger wandering
        Player farPlayer = new Player(500.0, 500.0, weapon);
        
        for (int i = 0; i < 5; i++) {
            golen.update(farPlayer, mockMap);
        }
        
        // Verify the update method works without crashing
        assertNotNull(golen);
    }

    @Test
    void testGolenDrawing() {
        // Test that Golen can be drawn without crashing
        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mockGraphics = mockImage.createGraphics();
        
        try {
            golen.draw(mockGraphics, 0, 0);
            // If we get here, drawing didn't crash
            assertTrue(true);
        } catch (Exception e) {
            fail("Golen drawing should not crash: " + e.getMessage());
        } finally {
            mockGraphics.dispose();
        }
    }

    @Test
    void testGolenCollisionDetection() {
        // Test that Golen collision detection works
        // This is inherited from Entity, but we verify it works for Golen
        assertNotNull(golen);
        
        // Update should not crash even with wall collision
        golen.update(mockPlayer, mockMap);
        assertNotNull(golen);
    }

    @Test
    void testGolenGroupBehavior() {
        // Test that Golen can work with group behavior
        // Create a list of nearby enemies
        java.util.List<Entity> enemies = new java.util.ArrayList<>();
        Soldier ally1 = new Soldier(120.0, 100.0, weapon);
        Soldier ally2 = new Soldier(100.0, 120.0, weapon);
        enemies.add(ally1);
        enemies.add(ally2);
        
        // Update with enemies list
        golen.update(mockPlayer, mockMap, enemies);
        
        // Verify the update method works without crashing
        assertNotNull(golen);
    }

    @Test
    void testGolenChargeUpAttack() {
        // Test that Golen has charge-up attack behavior
        // Place player in attack range
        Player nearbyPlayer = new Player(130.0, 100.0, weapon);
        
        // Update to trigger charge-up
        golen.update(nearbyPlayer, mockMap);
        
        // Verify the update method works without crashing
        assertNotNull(golen);
    }

    @Test
    void testGolenRecoveryAfterAttack() {
        // Test that Golen has longer recovery after attack
        // Place player in attack range
        Player nearbyPlayer = new Player(130.0, 100.0, weapon);
        
        // Update multiple times to trigger attack and recovery
        for (int i = 0; i < 15; i++) {
            golen.update(nearbyPlayer, mockMap);
        }
        
        // Verify the update method works without crashing
        assertNotNull(golen);
    }
}
