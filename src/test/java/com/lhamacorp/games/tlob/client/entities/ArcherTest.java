package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcherTest {

    private Archer archer;
    private Sword sword;

    @BeforeEach
    void setUp() {
        sword = new Sword(1, 30, 20, 500, 1000);
        archer = new Archer(100, 100, sword);
    }

    @Test
    void testArcherCreation() {
        assertNotNull(archer);
        assertEquals(100, archer.getX());
        assertEquals(100, archer.getY());
        assertTrue(archer.isAlive());
    }

    @Test
    void testArcherStats() {
        assertEquals(18, archer.getWidth());
        assertEquals(18, archer.getHeight());
        assertEquals(1.0, archer.getSpeed(), 0.01);
        assertEquals(2.0, archer.getMaxHealth(), 0.01);
        assertEquals(1.0, archer.getMaxStamina(), 0.01);
        assertEquals(0.0, archer.getMaxMana(), 0.01);
    }

    @Test
    void testArcherDamage() {
        double initialHealth = archer.getHealth();
        archer.damage(0.5);
        assertEquals(initialHealth - 0.5, archer.getHealth(), 0.01);
    }

    @Test
    void testArcherPosition() {
        assertEquals(100, archer.getX());
        assertEquals(100, archer.getY());
    }

    @Test
    void testArcherWeapon() {
        assertNotNull(archer.getWeapon());
        assertEquals(sword, archer.getWeapon());
    }

    @Test
    void testArcherKnockback() {
        double initialX = archer.getX();
        double initialY = archer.getY();
        
        archer.applyKnockback(0, 0);
        
        // Mock TileMap for collision checking
        TileMap mockMap = new TileMap(new int[10][10]);
        archer.updateKnockbackWithMap(mockMap);
        
        // Should have moved due to knockback
        assertNotEquals(initialX, archer.getX());
        assertNotEquals(initialY, archer.getY());
    }

    @Test
    void testArcherInheritance() {
        assertTrue(archer instanceof Entity);
        assertEquals("Archer", archer.getName());
        assertTrue(archer.isFoe());
        assertFalse(archer.isAlly());
        assertFalse(archer.isNeutral());
    }

    // ===== Perk System Integration Test =====

    @Test
    void testArcherPerkApplication() {
        double initialMaxHealth = archer.getMaxHealth();
        double initialMaxStamina = archer.getMaxStamina();
        double initialSpeed = archer.getSpeed();
        
        // Apply perks to archer
        archer.increaseMaxHealthByPercent(0.2); // +20%
        archer.increaseMaxStaminaByPercent(0.15); // +15%
        archer.increaseMoveSpeedByPercent(0.25); // +25%
        archer.increaseAttackDamageByPercent(0.3); // +30%
        
        // Verify perks were applied
        // Note: Math.ceil(2.0 * 1.2) = Math.ceil(2.4) = 3.0
        assertEquals(3.0, archer.getMaxHealth(), 0.01);
        assertEquals(2.0, archer.getMaxStamina(), 0.01); // Math.ceil(1.0 * 1.15) = Math.ceil(1.15) = 2.0
        assertEquals(1.25, archer.getEffectiveSpeed(), 0.01); // 1.0 * 1.25 = 1.25
        
        // Verify multipliers are set correctly
        assertEquals(1.25, archer.getSpeedMultiplier(), 0.01);
        assertEquals(1.3, archer.getDamageMultiplier(), 0.01);
    }
}
