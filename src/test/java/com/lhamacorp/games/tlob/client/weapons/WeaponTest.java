package com.lhamacorp.games.tlob.client.weapons;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Alignment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WeaponTest {

    @Test
    public void testWeaponRangeCap() {
        Weapon weapon = new Weapon() {
            // Create anonymous subclass for testing
        };
        
        // Test that range can be set normally below the cap
        weapon.setReach(30);
        assertEquals(30, weapon.getReach());
        
        // Test that range is capped at maximum
        weapon.setReach(100);
        assertEquals(Weapon.MAX_WEAPON_RANGE, weapon.getReach());
        
        // Test that range can be set to exactly the cap
        weapon.setReach(Weapon.MAX_WEAPON_RANGE);
        assertEquals(Weapon.MAX_WEAPON_RANGE, weapon.getReach());
        
        // Test that range can be set below the cap after being at cap
        weapon.setReach(25);
        assertEquals(25, weapon.getReach());
    }
    
    @Test
    public void testMaxWeaponRangeConstant() {
        assertEquals(60, Weapon.MAX_WEAPON_RANGE);
    }
    
    @Test
    public void testWeaponWidthCap() {
        Weapon weapon = new Weapon() {
            // Create anonymous subclass for testing
        };
        
        // Test that width can be set normally below the cap
        weapon.setWidth(5);
        assertEquals(5, weapon.getWidth());
        
        // Test that width is capped at maximum
        weapon.setWidth(15);
        assertEquals(Weapon.MAX_WEAPON_WIDTH, weapon.getWidth());
        
        // Test that width can be set to exactly the cap
        weapon.setWidth(Weapon.MAX_WEAPON_WIDTH);
        assertEquals(Weapon.MAX_WEAPON_WIDTH, weapon.getWidth());
        
        // Test that width can be set below the cap after being at cap
        weapon.setWidth(3);
        assertEquals(3, weapon.getWidth());
    }
    
    @Test
    public void testMaxWeaponWidthConstant() {
        assertEquals(10, Weapon.MAX_WEAPON_WIDTH);
    }
    
    @Test
    public void testWeaponWidthCapThroughEntity() {
        // Create a test entity with a weapon
        Entity entity = new Entity(0, 0, 20, 20, 1.0, 1.0, 1.0, 1.0, 0, 0, new Weapon() {}, "TestEntity", Alignment.NEUTRAL) {
            // Anonymous subclass for testing
            @Override
            public void update(Object... args) {
                // No-op for testing
            }
            
            @Override
            public void draw(java.awt.Graphics2D g, int screenX, int screenY) {
                // No-op for testing
            }
        };
        
        Weapon weapon = entity.getWeapon();
        
        // Test that width can be increased normally below the cap
        entity.increaseWeaponWidth(3);
        assertEquals(3, weapon.getWidth());
        
        // Test that width is capped when trying to exceed maximum
        entity.increaseWeaponWidth(10);
        assertEquals(Weapon.MAX_WEAPON_WIDTH, weapon.getWidth());
        
        // Test that width can still be increased within the cap
        entity.increaseWeaponWidth(2);
        assertEquals(Weapon.MAX_WEAPON_WIDTH, weapon.getWidth()); // Should still be capped
    }
}
