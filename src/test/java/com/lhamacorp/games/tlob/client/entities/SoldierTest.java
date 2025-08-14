package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoldierTest {

    private Soldier soldier;
    private Sword sword;

    @BeforeEach
    void setUp() {
        sword = new Sword(1, 30, 20, 500, 1000);
        soldier = new Soldier(100, 100, sword);
    }

    @Test
    void testSoldierCreation() {
        assertNotNull(soldier);
        assertEquals(100, soldier.getX());
        assertEquals(100, soldier.getY());
        assertTrue(soldier.isAlive());
    }

    @Test
    void testSoldierStats() {
        assertEquals(20, soldier.getWidth());
        assertEquals(20, soldier.getHeight());
        assertEquals(3.0, soldier.getSpeed(), 0.01);
        assertEquals(1.0, soldier.getMaxHealth(), 0.01);
        assertEquals(1.0, soldier.getMaxStamina(), 0.01);
        assertEquals(0.0, soldier.getMaxMana(), 0.01);
    }

    @Test
    void testSoldierDamage() {
        double initialHealth = soldier.getHealth();
        soldier.damage(0.5);
        assertEquals(initialHealth - 0.5, soldier.getHealth(), 0.01);
    }

    @Test
    void testSoldierPosition() {
        assertEquals(100, soldier.getX());
        assertEquals(100, soldier.getY());
    }

    @Test
    void testSoldierWeapon() {
        assertNotNull(soldier.getWeapon());
        assertEquals(sword, soldier.getWeapon());
    }

    @Test
    void testSoldierKnockback() {
        double initialX = soldier.getX();
        double initialY = soldier.getY();
        
        soldier.applyKnockback(0, 0);
        
        // Mock TileMap for collision checking
        TileMap mockMap = new TileMap(new int[10][10]);
        soldier.updateKnockbackWithMap(mockMap);
        
        // Should have moved due to knockback
        assertNotEquals(initialX, soldier.getX());
        assertNotEquals(initialY, soldier.getY());
    }

    @Test
    void testSoldierInheritance() {
        assertTrue(soldier instanceof Entity);
        assertEquals("Soldier", soldier.getName());
        assertTrue(soldier.isFoe());
        assertFalse(soldier.isAlly());
        assertFalse(soldier.isNeutral());
    }

    // ===== Perk System Integration Test =====

    @Test
    void testSoldierPerkApplication() {
        double initialMaxHealth = soldier.getMaxHealth();
        double initialMaxStamina = soldier.getMaxStamina();
        double initialSpeed = soldier.getSpeed();
        
        // Apply perks to soldier
        soldier.increaseMaxHealthByPercent(0.1); // +10%
        soldier.increaseMaxStaminaByPercent(0.1); // +10%
        soldier.increaseMoveSpeedByPercent(0.1); // +10%
        
        // Verify perks were applied
        // Note: Math.ceil(1.0 * 1.1) = Math.ceil(1.1) = 2.0
        assertEquals(2.0, soldier.getMaxHealth(), 0.01);
        assertEquals(2.0, soldier.getMaxStamina(), 0.01);
        assertEquals(3.3, soldier.getEffectiveSpeed(), 0.01); // 3.0 * 1.1 = 3.3
        
        // Verify multipliers are set correctly
        assertEquals(1.1, soldier.getSpeedMultiplier(), 0.01);
    }

    @Test
    void testSoldierPerkCountIndicator() {
        // Initially no perks
        assertEquals(0, soldier.getPerkCount());
        
        // Apply some perks
        soldier.increaseMaxHealthByPercent(0.1);
        soldier.increaseMoveSpeedByPercent(0.1);
        
        // Should now have 2 perks
        assertEquals(2, soldier.getPerkCount());
        
        // Apply more perks
        soldier.increaseMaxStaminaByPercent(0.1);
        soldier.increaseAttackDamageByPercent(0.1);
        
        // Should now have 4 perks
        assertEquals(4, soldier.getPerkCount());
    }
}
