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
        assertEquals(2.5, soldier.getSpeed(), 0.01);
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
}
