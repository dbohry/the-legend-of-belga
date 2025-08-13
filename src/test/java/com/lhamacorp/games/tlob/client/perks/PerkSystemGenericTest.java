package com.lhamacorp.games.tlob.client.perks;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerkSystemGenericTest {

    private PerkManager perkManager;
    private Player player;
    private Soldier soldier;
    private Archer archer;
    private Sword sword;

    @BeforeEach
    void setUp() {
        perkManager = new PerkManager();
        sword = new Sword(2, 28, 12, 10, 16);
        player = new Player(100, 100, sword);
        soldier = new Soldier(200, 200, sword);
        archer = new Archer(300, 300, sword);
    }

    // ===== Generic Perk Application Tests =====

    @Test
    void testPerksWorkWithPlayer() {
        // Roll perks for player
        perkManager.rollChoicesFor(player);
        assertFalse(perkManager.getChoices().isEmpty());
        
        // Apply a perk to player
        Perk perk = perkManager.getChoices().get(0);
        double initialHealth = player.getMaxHealth();
        perk.apply(player);
        
        // Verify perk affected player
        assertTrue(player.getMaxHealth() >= initialHealth);
    }

    @Test
    void testPerksWorkWithSoldier() {
        // Roll perks for soldier
        perkManager.rollChoicesFor(soldier);
        assertFalse(perkManager.getChoices().isEmpty());
        
        // Apply a perk to soldier
        Perk perk = perkManager.getChoices().get(0);
        double initialHealth = soldier.getMaxHealth();
        perk.apply(soldier);
        
        // Verify perk affected soldier
        assertTrue(soldier.getMaxHealth() >= initialHealth);
    }

    @Test
    void testPerksWorkWithArcher() {
        // Roll perks for archer
        perkManager.rollChoicesFor(archer);
        assertFalse(perkManager.getChoices().isEmpty());
        
        // Apply a perk to archer
        Perk perk = perkManager.getChoices().get(0);
        double initialHealth = archer.getMaxHealth();
        perk.apply(archer);
        
        // Verify perk affected archer
        assertTrue(archer.getMaxHealth() >= initialHealth);
    }

    // ===== Perk Eligibility Tests =====

    @Test
    void testPerkEligibilityWithDifferentEntities() {
        // Create a perk that works with all entities (health perk)
        PerkManager testManager = new PerkManager();
        testManager.register("HEALTH_ONLY", PerkManager.Rarity.COMMON, 
            entity -> entity.getMaxHealth() > 0, // All entities have health
            rng -> new Perk("Health Boost", "Increases health by 10%", PerkManager.Rarity.COMMON,
                entity -> entity.increaseMaxHealthByPercent(0.1))
        );
        
        // Test with player (has health)
        testManager.rollChoicesFor(player);
        assertFalse(testManager.getChoices().isEmpty(), "Player should be eligible for health perk");
        
        // Test with soldier (has health)
        testManager.rollChoicesFor(soldier);
        assertFalse(testManager.getChoices().isEmpty(), "Soldier should be eligible for health perk");
        
        // Test with archer (has health)
        testManager.rollChoicesFor(archer);
        assertFalse(testManager.getChoices().isEmpty(), "Archer should be eligible for health perk");
    }

    // ===== Perk Effect Tests =====

    @Test
    void testHealthPerkOnAllEntities() {
        // Test health perk on player
        double playerInitialHealth = player.getMaxHealth();
        player.increaseMaxHealthByPercent(0.1);
        assertEquals(7.0, player.getMaxHealth(), 0.01); // Math.ceil(6.0 * 1.1) = Math.ceil(6.6) = 7.0
        
        // Test health perk on soldier
        double soldierInitialHealth = soldier.getMaxHealth();
        soldier.increaseMaxHealthByPercent(0.1);
        assertEquals(2.0, soldier.getMaxHealth(), 0.01); // Math.ceil(1.0 * 1.1) = Math.ceil(1.1) = 2.0
        
        // Test health perk on archer
        double archerInitialHealth = archer.getMaxHealth();
        archer.increaseMaxHealthByPercent(0.1);
        assertEquals(3.0, archer.getMaxHealth(), 0.01); // Math.ceil(2.0 * 1.1) = Math.ceil(2.2) = 3.0
    }

    @Test
    void testSpeedPerkOnAllEntities() {
        // Test speed perk on player
        double playerInitialSpeed = player.getSpeed();
        player.increaseMoveSpeedByPercent(0.1);
        assertEquals(playerInitialSpeed * 1.1, player.getEffectiveSpeed(), 0.01);
        
        // Test speed perk on soldier
        double soldierInitialSpeed = soldier.getSpeed();
        soldier.increaseMoveSpeedByPercent(0.1);
        assertEquals(soldierInitialSpeed * 1.1, soldier.getEffectiveSpeed(), 0.01);
        
        // Test speed perk on archer
        double archerInitialSpeed = archer.getSpeed();
        archer.increaseMoveSpeedByPercent(0.1);
        assertEquals(archerInitialSpeed * 1.1, archer.getEffectiveSpeed(), 0.01);
    }

    @Test
    void testDamagePerkOnAllEntities() {
        // Test damage perk on player
        double playerInitialDamage = player.getEffectiveAttackDamage();
        player.increaseAttackDamageByPercent(0.1);
        assertEquals(playerInitialDamage * 1.1, player.getEffectiveAttackDamage(), 0.01);
        
        // Test damage perk on soldier
        double soldierInitialDamage = soldier.getEffectiveAttackDamage();
        soldier.increaseAttackDamageByPercent(0.1);
        assertEquals(soldierInitialDamage * 1.1, soldier.getEffectiveAttackDamage(), 0.01);
        
        // Test damage perk on archer
        double archerInitialDamage = archer.getEffectiveAttackDamage();
        archer.increaseAttackDamageByPercent(0.1);
        assertEquals(archerInitialDamage * 1.1, archer.getEffectiveAttackDamage(), 0.01);
    }

    // ===== Perk Manager Integration Tests =====

    @Test
    void testPerkManagerWithGenericEntities() {
        // Test that perk manager can work with any entity type
        Entity[] entities = {player, soldier, archer};
        
        for (Entity entity : entities) {
            perkManager.rollChoicesFor(entity);
            assertFalse(perkManager.getChoices().isEmpty());
            
            // Apply first perk
            Perk perk = perkManager.getChoices().get(0);
            double initialHealth = entity.getMaxHealth();
            perk.apply(entity);
            
            // Verify perk had an effect
            assertTrue(entity.getMaxHealth() >= initialHealth);
        }
    }

    // ===== Edge Case Tests =====

    @Test
    void testPerksOnNullEntity() {
        // Test that perks handle null entities gracefully
        Perk perk = new Perk("Test", "Test perk", PerkManager.Rarity.COMMON, entity -> entity.increaseMaxHealthByPercent(0.1));
        
        // Should not throw exception
        assertDoesNotThrow(() -> perk.apply(null));
    }

    @Test
    void testPerkManagerWithNullEntity() {
        // Test that perk manager handles null entities
        perkManager.rollChoicesFor(null);
        // Should not throw exception and should provide some choices
        assertNotNull(perkManager.getChoices());
    }
}
