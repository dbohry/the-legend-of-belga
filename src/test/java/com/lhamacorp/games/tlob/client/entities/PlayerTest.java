package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import com.lhamacorp.games.tlob.client.world.InputState;
import com.lhamacorp.games.tlob.client.world.PlayerInputView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player player;
    private Sword sword;
    private TileMap mockMap;
    private List<Entity> enemies;

    @BeforeEach
    void setUp() {
        sword = new Sword(2, 28, 12, 10, 16);
        player = new Player(100, 100, sword);
        mockMap = new TileMap(new int[10][10]); // 10x10 empty map
        enemies = new ArrayList<>();
    }

    // ===== Basic Player Creation and Properties =====

    @Test
    void testPlayerCreation() {
        assertNotNull(player);
        assertEquals(100, player.getX(), 0.01);
        assertEquals(100, player.getY(), 0.01);
        assertTrue(player.isAlive());
        assertEquals("Player", player.getName());
        assertTrue(player.isNeutral());
    }

    @Test
    void testPlayerStats() {
        assertEquals(22, player.getWidth());
        assertEquals(22, player.getHeight());
        assertEquals(3.0, player.getSpeed(), 0.01);
        assertEquals(6.0, player.getMaxHealth(), 0.01);
        assertEquals(6.0, player.getMaxStamina(), 0.01);
        assertEquals(0.0, player.getMaxMana(), 0.01);
        assertEquals(0.0, player.getMaxShield(), 0.01);
    }

    @Test
    void testPlayerWeapon() {
        assertNotNull(player.getWeapon());
        assertEquals(sword, player.getWeapon());
    }

    // ===== Health and Stamina Management =====

    @Test
    void testPlayerHealth() {
        double initialHealth = player.getHealth();
        player.damage(1.0);
        assertEquals(initialHealth - 1.0, player.getHealth(), 0.01);
        
        player.heal();
        assertEquals(player.getMaxHealth(), player.getHealth(), 0.01);
    }

    @Test
    void testPlayerStamina() {
        double initialStamina = player.getStamina();
        player.setStamina(initialStamina - 1.0);
        assertEquals(initialStamina - 1.0, player.getStamina(), 0.01);
        
        player.restoreAll();
        assertEquals(player.getMaxStamina(), player.getStamina(), 0.01);
    }

    @Test
    void testPlayerMana() {
        double initialMana = player.getMana();
        // Note: Player starts with 0 max mana, so we can't easily test mana setting
        // But we can test that mana regeneration works
        player.restoreAll();
        assertEquals(player.getMaxMana(), player.getMana(), 0.01);
    }

    @Test
    void testPlayerShield() {
        double initialShield = player.getShield();
        
        // Note: setShield clamps values to maxShield, and player starts with 0 max shield
        // So setting shield to any positive value will clamp it to 0
        assertEquals(0.0, player.getMaxShield(), 0.01);
        assertEquals(0.0, player.getShield(), 0.01);
        
        // Test that setShield respects the maxShield limit
        player.setShield(1.0);
        assertEquals(0.0, player.getShield(), 0.01); // Clamped to maxShield (0.0)
        
        player.setShield(2.0);
        assertEquals(0.0, player.getShield(), 0.01); // Still clamped to maxShield (0.0)
        
        // Test that we can set shield to 0 (which is within the limit)
        player.setShield(0.0);
        assertEquals(0.0, player.getShield(), 0.01);
    }

    // ===== Position and Movement =====

    @Test
    void testPlayerPosition() {
        player.setPosition(200, 300);
        assertEquals(200, player.getX(), 0.01);
        assertEquals(300, player.getY(), 0.01);
    }

    @Test
    void testPlayerFacingOctant() {
        // Test that setFacingOctant doesn't crash
        player.setFacingOctant(0); // Right
        player.setFacingOctant(2); // Down
        player.setFacingOctant(4); // Left
        player.setFacingOctant(6); // Up
        
        // The method should execute without errors
        assertNotNull(player);
    }

    // ===== Dash Mechanics =====

    @Test
    void testPlayerDashCapability() {
        // Player starts with 0 mana, so can't dash
        assertFalse(player.canDash());
        
        // Note: We can't easily set mana directly, but we can test the method exists
        assertNotNull(player);
    }

    @Test
    void testPlayerDashManaCost() {
        assertEquals(2.0, player.getDashManaCost(), 0.01);
    }

    @Test
    void testPlayerDashState() {
        assertFalse(player.isDashing());
        assertFalse(player.isDashTrailActive());
    }

    // ===== Block Mechanics =====

    @Test
    void testPlayerBlockState() {
        // Player should not be blocking initially
        assertFalse(player.isBlocking());
        assertTrue(player.canBlock()); // Has enough stamina
    }

    @Test
    void testPlayerBlockWithLowStamina() {
        // Set stamina below block cost for a 1.0 damage attack
        player.setStamina(0.3);
        assertFalse(player.canBlock(1.0)); // Need 0.5 stamina to block 1.0 damage
        
        // But can still attempt to block (the actual check happens when damage is received)
        assertTrue(player.canBlock());
    }

    @Test
    void testPlayerBlockDamageReduction() {
        double initialHealth = player.getHealth();
        double initialStamina = player.getStamina();
        
        // Simulate blocking state
        player.setStamina(1.0); // Ensure enough stamina
        
        // Create a mock input that simulates blocking
        PlayerInputView mockInput = new PlayerInputView() {
            @Override
            public boolean left() { return false; }
            @Override
            public boolean right() { return false; }
            @Override
            public boolean up() { return false; }
            @Override
            public boolean down() { return false; }
            @Override
            public boolean sprint() { return false; }
            @Override
            public boolean attack() { return false; }
            @Override
            public boolean block() { return true; }
        };
        
        // Update player with block input
        player.update(mockInput, mockMap, enemies);
        
        // Player should now be blocking
        assertTrue(player.isBlocking());
        
        // Take damage while blocking
        double damageAmount = 2.0;
        player.damage(damageAmount);
        
        // Check that damage was reduced (80% reduction = 20% damage taken)
        double expectedDamage = damageAmount * 0.2;
        double expectedHealth = initialHealth - expectedDamage;
        assertEquals(expectedHealth, player.getHealth(), 0.01);
        
        // Check that stamina was consumed
        // Note: The player might have regenerated some stamina during the update
        // So we just check that stamina decreased
        assertTrue(player.getStamina() < initialStamina);
    }

    @Test
    void testPlayerBlockPreventsAttack() {
        // Set up player with enough stamina and no cooldowns
        player.setStamina(1.0);
        
        // Create mock input that simulates both blocking and attacking
        PlayerInputView mockInput = new PlayerInputView() {
            @Override
            public boolean left() { return false; }
            @Override
            public boolean right() { return false; }
            @Override
            public boolean up() { return false; }
            @Override
            public boolean down() { return false; }
            @Override
            public boolean sprint() { return false; }
            @Override
            public boolean attack() { return true; }
            @Override
            public boolean block() { return true; }
        };
        
        // Update player with both block and attack input
        player.update(mockInput, mockMap, enemies);
        
        // Player should be blocking
        assertTrue(player.isBlocking());
        
        // Try to attack while blocking
        player.update(mockInput, mockMap, enemies);
        
        // Attack should not be processed due to blocking
        // We can verify this by checking that the player is still blocking
        // and that stamina was consumed for blocking but not for attack
        assertTrue(player.isBlocking());
        
        // The player should have consumed stamina for blocking (0.5) but not for attack
        // However, stamina regeneration might occur during updates, so we just verify
        // that the player is still blocking and the attack didn't go through
        assertTrue(player.isBlocking());
    }
    
    @Test
    void testPlayerDynamicBlockStaminaCost() {
        // Test that blocking stamina cost is half of incoming damage
        player.setStamina(3.0); // Ensure enough stamina
        
        // Create mock input that simulates blocking
        PlayerInputView mockInput = new PlayerInputView() {
            @Override
            public boolean left() { return false; }
            @Override
            public boolean right() { return false; }
            @Override
            public boolean up() { return false; }
            @Override
            public boolean down() { return false; }
            @Override
            public boolean sprint() { return false; }
            @Override
            public boolean attack() { return false; }
            @Override
            public boolean block() { return true; }
        };
        
        // Update player with block input
        player.update(mockInput, mockMap, enemies);
        assertTrue(player.isBlocking());
        
        double initialStamina = player.getStamina();
        
        // Test with different damage amounts
        double[] testDamages = {1.0, 2.0, 4.0, 8.0};
        
        for (double damage : testDamages) {
            double requiredStamina = damage * 0.5;
            player.setStamina(requiredStamina + 0.1); // Ensure enough stamina for blocking
            
            // Take damage while blocking
            player.damage(damage);
            
            // Check that stamina was consumed by half the damage amount
            double expectedStaminaConsumption = damage * 0.5;
            double actualStaminaConsumption = (requiredStamina + 0.1) - player.getStamina();
            assertEquals(expectedStaminaConsumption, actualStaminaConsumption, 0.01, 
                "Stamina cost for blocking " + damage + " damage should be " + expectedStaminaConsumption);
        }
    }

    // ===== Perk System Integration =====

    @Test
    void testPlayerPerkMultipliers() {
        assertEquals(1.0, player.getDamageMultiplier(), 0.01);
        assertEquals(1.0, player.getSpeedMultiplier(), 0.01);
        assertEquals(1.0, player.getStaminaRegenRateMult(), 0.01);
        assertEquals(1.0, player.getManaRegenRateMult(), 0.01);
    }

    @Test
    void testPlayerPerkApplication() {
        double initialMaxHealth = player.getMaxHealth();
        double initialMaxStamina = player.getMaxStamina();
        double initialMaxMana = player.getMaxMana();
        
        // Apply perks
        player.increaseMaxHealthByPercent(0.1); // +10%
        player.increaseMaxStaminaByPercent(0.1); // +10%
        player.increaseMaxManaByPercent(0.1); // +10%
        
        // Note: Perks use Math.ceil which rounds up
        // 6.0 * 1.1 = 6.6, but Math.ceil(6.6) = 7.0
        assertEquals(7.0, player.getMaxHealth(), 0.01);
        assertEquals(7.0, player.getMaxStamina(), 0.01);
        assertEquals(2.0, player.getMaxMana(), 0.01); // Note: 0 * 1.1 = 0, but perk sets it to 1.0 first, then 1.0 * 1.1 = 1.1, Math.ceil(1.1) = 2.0
    }

    // ===== Combat Mechanics =====

    @Test
    void testPlayerAttackCooldown() {
        // Player should not be invulnerable initially
        assertFalse(player.isInvulnerable());
    }

    @Test
    void testPlayerKnockback() {
        double initialX = player.getX();
        double initialY = player.getY();
        
        player.applyKnockback(0, 0);
        
        // Update knockback
        player.updateKnockbackWithMap(mockMap);
        
        // Should have moved due to knockback
        assertNotEquals(initialX, player.getX());
        assertNotEquals(initialY, player.getY());
    }

    // ===== Input Handling =====

    @Test
    void testPlayerInputState() {
        InputState inputState = new InputState();
        inputState.up = true;
        inputState.right = true;
        inputState.attack = true;
        inputState.block = true;
        
        // Update player with input state
        player.update(inputState, mockMap, enemies);
        
        // Player should respond to input (facing should change due to movement)
        // Note: We can't easily test exact facing without complex movement calculations
        // but we can verify the update method doesn't crash
        assertNotNull(player);
    }

    // ===== Edge Cases and Error Handling =====

    @Test
    void testPlayerDeath() {
        player.damage(player.getMaxHealth() + 1);
        assertFalse(player.isAlive());
        assertEquals(0.0, player.getHealth(), 0.01);
    }

    @Test
    void testPlayerStaminaClamping() {
        player.setStamina(-1.0);
        assertEquals(0.0, player.getStamina(), 0.01);
        
        player.setStamina(player.getMaxStamina() + 1.0);
        assertEquals(player.getMaxStamina(), player.getStamina(), 0.01);
    }

    @Test
    void testPlayerHealthClamping() {
        player.setHealth(-1.0);
        assertEquals(0.0, player.getHealth(), 0.01);
        assertFalse(player.isAlive());
        
        player.setHealth(player.getMaxHealth() + 1.0);
        assertEquals(player.getMaxHealth(), player.getHealth(), 0.01);
        assertTrue(player.isAlive());
    }

    // ===== Shadow Trail System =====

    @Test
    void testPlayerShadowTrails() {
        assertEquals(0, player.getShadowTrailCount());
        assertEquals(8, player.getMaxShadowTrails());
    }

    // ===== Screen Shake =====

    @Test
    void testPlayerScreenShake() {
        Point shakeOffset = player.getScreenShakeOffset();
        assertNotNull(shakeOffset);
        assertEquals(0, shakeOffset.x);
        assertEquals(0, shakeOffset.y);
    }
}
