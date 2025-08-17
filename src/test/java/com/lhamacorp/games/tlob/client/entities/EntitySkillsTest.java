package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.entities.skills.Skill;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntitySkillsTest {

    private TestEntity entity;
    private Skill testSkill;
    
    @BeforeEach
    void setUp() {
        entity = new TestEntity(0, 0, 20, 20, 3.0, 10.0, 5.0, 5.0, 0, 0, null, "Test", Alignment.NEUTRAL);
        testSkill = new Skill("Test Skill", "A test skill", 2.0, 30, 10, 40);
    }
    
    @Test
    void testInitialSkillState() {
        assertNull(entity.getPrimarySkill());
        assertNotNull(entity.getSkillManager());
        assertFalse(entity.isPrimarySkillReady());
        assertEquals(0.0, entity.getPrimarySkillDamage(), 0.01);
    }
    
    @Test
    void testSetPrimarySkill() {
        entity.setPrimarySkill(testSkill);
        
        assertEquals(testSkill, entity.getPrimarySkill());
        assertTrue(entity.isPrimarySkillReady());
        assertEquals(2.0, entity.getPrimarySkillDamage(), 0.01); // 2.0 * 1.0 (base damage * default damage multiplier)
    }
    
    @Test
    void testUsePrimarySkill() {
        entity.setPrimarySkill(testSkill);
        
        // First use should succeed
        assertTrue(entity.usePrimarySkill());
        assertFalse(entity.isPrimarySkillReady());
        
        // Second use should fail (on cooldown)
        assertFalse(entity.usePrimarySkill());
    }
    
    @Test
    void testSkillCooldownRecovery() {
        entity.setPrimarySkill(testSkill);
        entity.usePrimarySkill();
        
        // Update skills until cooldown expires
        for (int i = 0; i < 30; i++) {
            entity.updateSkills();
        }
        
        assertTrue(entity.isPrimarySkillReady());
        assertEquals(2.0, entity.getPrimarySkillDamage(), 0.01); // Skill ready, returns base damage
    }
    
    @Test
    void testSkillDamageWithPerks() {
        entity.setPrimarySkill(testSkill);
        
        // Apply damage perk
        entity.increaseAttackDamageByPercent(0.5); // +50% damage
        
        assertEquals(3.0, entity.getPrimarySkillDamage(), 0.01); // 2.0 * 1.5 (base damage * 1.5 multiplier)
    }
    
    @Test
    void testMultipleSkillUses() {
        entity.setPrimarySkill(testSkill);
        
        // Use skill multiple times
        assertTrue(entity.usePrimarySkill());
        assertFalse(entity.usePrimarySkill());
        
        // Wait for cooldown
        for (int i = 0; i < 30; i++) {
            entity.updateSkills();
        }
        
        assertTrue(entity.usePrimarySkill());
    }
    
    @Test
    void testSkillManagerIntegration() {
        entity.setPrimarySkill(testSkill);
        SkillManager skillManager = entity.getSkillManager();
        
        assertNotNull(skillManager);
        assertTrue(skillManager.isSkillReady(testSkill));
        
        entity.usePrimarySkill();
        assertFalse(skillManager.isSkillReady(testSkill));
        assertTrue(skillManager.isSkillActive(testSkill));
    }
    
    @Test
    void testNoPrimarySkill() {
        // Entity without primary skill
        assertFalse(entity.isPrimarySkillReady());
        assertFalse(entity.usePrimarySkill());
        assertEquals(0.0, entity.getPrimarySkillDamage(), 0.01);
    }
    
    @Test
    void testSkillDamageCalculation() {
        entity.setPrimarySkill(testSkill);
        
        // Test base damage
        assertEquals(2.0, entity.getPrimarySkillDamage(), 0.01); // 2.0 * 1.0
        
        // Apply multiple damage perks
        entity.increaseAttackDamageByPercent(0.25); // +25%
        entity.increaseAttackDamageByPercent(0.25); // +25% more
        
        // Total multiplier: 1.0 * 1.25 * 1.25 = 1.5625
        assertEquals(3.125, entity.getPrimarySkillDamage(), 0.01); // 2.0 * 1.5625
    }
    
    // Helper test entity class
    private static class TestEntity extends Entity {
        public TestEntity(double x, double y, int width, int height, double speed, 
                         double maxHealth, double maxStamina, double maxMana, 
                         double maxShield, double maxArmor, Weapon weapon, 
                         String name, Alignment alignment) {
            super(x, y, width, height, speed, maxHealth, maxStamina, maxMana, maxShield, maxArmor, weapon, name, alignment);
        }
        
        @Override
        public void update(Object... args) {}
        
        @Override
        public void draw(java.awt.Graphics2D g2, int camX, int camY) {}
    }
}
