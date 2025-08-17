package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.entities.skills.Skill;
import com.lhamacorp.games.tlob.client.entities.skills.Skills;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.awt.Graphics2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the entity attributes system.
 * Tests how entity attributes (attack speed, base damage) modify skill performance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityAttributesTest {
    
    private Entity testEntity;
    private Skill testSkill;
    
    @BeforeEach
    void setUp() {
        // Create a test entity with default attributes
        testEntity = new TestEntity(100, 100, 32, 32, 2.0, 100, 50, 25, 20, 5, new Sword(1, 30, 10, 3, 60));
        testSkill = Skills.SLASH; // Use the SLASH skill for testing
    }
    
    @Test
    void testDefaultAttributes() {
        // Test default attribute values
        assertEquals(1.0, testEntity.getAttackSpeed(), "Default attack speed should be 1.0");
        assertEquals(1.0, testEntity.getBaseDamage(), "Default base damage should be 1.0");
    }
    
    @Test
    void testSetAttackSpeed() {
        // Test setting attack speed
        testEntity.setAttackSpeed(2.0);
        assertEquals(2.0, testEntity.getAttackSpeed(), "Attack speed should be set to 2.0");
        
        // Test minimum attack speed limit
        testEntity.setAttackSpeed(0.05); // Below minimum
        assertEquals(0.1, testEntity.getAttackSpeed(), "Attack speed should be clamped to minimum 0.1");
    }
    
    @Test
    void testIncreaseAttackSpeedByPercent() {
        // Test increasing attack speed by percentage
        testEntity.increaseAttackSpeedByPercent(0.5); // 50% increase
        assertEquals(1.5, testEntity.getAttackSpeed(), 0.001, "Attack speed should increase by 50% to 1.5");
        
        testEntity.increaseAttackSpeedByPercent(0.2); // 20% increase
        assertEquals(1.8, testEntity.getAttackSpeed(), 0.001, "Attack speed should increase by 20% to 1.8");
    }
    
    @Test
    void testSetBaseDamage() {
        // Test setting base damage
        testEntity.setBaseDamage(5.0);
        assertEquals(5.0, testEntity.getBaseDamage(), "Base damage should be set to 5.0");
        
        // Test minimum base damage limit
        testEntity.setBaseDamage(-2.0); // Below minimum
        assertEquals(0.0, testEntity.getBaseDamage(), "Base damage should be clamped to minimum 0.0");
    }
    
    @Test
    void testIncreaseBaseDamage() {
        // Test increasing base damage by amount
        testEntity.increaseBaseDamage(3.0);
        assertEquals(4.0, testEntity.getBaseDamage(), "Base damage should increase by 3.0 to 4.0");
        
        testEntity.increaseBaseDamage(1.5);
        assertEquals(5.5, testEntity.getBaseDamage(), "Base damage should increase by 1.5 to 5.5");
    }
    
    @Test
    void testSkillTotalDamage() {
        // Test that entity base damage is added to skill base damage
        testEntity.setBaseDamage(2.0);
        testEntity.increaseAttackDamageByPercent(0.5); // 50% damage multiplier
        
        // SLASH has base damage 1.0, entity base damage 2.0, damage multiplier 1.5
        // Total damage = (1.0 + 2.0) * 1.5 = 4.5
        double expectedDamage = (1.0 + 2.0) * 1.5;
        assertEquals(expectedDamage, testSkill.getTotalDamage(testEntity), 0.001,
                   "Total damage should be (skill base + entity base) * damage multiplier");
    }
    
    @Test
    void testSkillEffectiveCooldown() {
        // Test that attack speed affects cooldown
        testEntity.setAttackSpeed(2.0); // Double attack speed
        
        // SLASH has base cooldown 30 ticks, attack speed 2.0
        // Effective cooldown = 30 / 2.0 = 15 ticks
        int expectedCooldown = 30 / 2;
        assertEquals(expectedCooldown, testSkill.getEffectiveCooldown(testEntity),
                   "Effective cooldown should be reduced by attack speed");
        
        // Test minimum cooldown limit
        testEntity.setAttackSpeed(100.0); // Very high attack speed
        assertEquals(1, testSkill.getEffectiveCooldown(testEntity),
                   "Effective cooldown should be clamped to minimum 1 tick");
    }
    
    @Test
    void testSkillEffectiveDuration() {
        // Test that attack speed affects duration
        testEntity.setAttackSpeed(1.5); // 1.5x attack speed
        
        // SLASH has base duration 3 ticks, attack speed 1.5
        // Effective duration = 3 / 1.5 = 2 ticks
        int expectedDuration = (int) Math.round(3.0 / 1.5);
        assertEquals(expectedDuration, testSkill.getEffectiveDuration(testEntity),
                   "Effective duration should be reduced by attack speed");
        
        // Test minimum duration limit
        testEntity.setAttackSpeed(10.0); // Very high attack speed
        assertEquals(1, testSkill.getEffectiveDuration(testEntity),
                   "Effective duration should be clamped to minimum 1 tick");
    }
    
    @Test
    void testSkillEffectiveRange() {
        // Test that range is not modified by entity attributes (for now)
        double originalRange = testSkill.getRange();
        testEntity.setAttackSpeed(2.0);
        testEntity.setBaseDamage(5.0);
        
        assertEquals(originalRange, testSkill.getEffectiveRange(testEntity),
                   "Effective range should not be modified by entity attributes");
    }
    
    @Test
    void testSkillManagerWithEffectiveValues() {
        // Test that SkillManager uses effective values when entity is provided
        SkillManager skillManager = testEntity.getSkillManager();
        
        // Set entity attributes
        testEntity.setAttackSpeed(2.0);
        testEntity.setBaseDamage(1.0);
        
        // Use skill with entity context
        skillManager.useSkill(testSkill, testEntity);
        
        // Check that effective cooldown and duration are used
        int expectedCooldown = testSkill.getEffectiveCooldown(testEntity);
        int expectedDuration = testSkill.getEffectiveDuration(testEntity);
        
        assertEquals(expectedCooldown, skillManager.getRemainingCooldown(testSkill),
                   "SkillManager should use effective cooldown when entity is provided");
        assertEquals(expectedDuration, skillManager.getRemainingDuration(testSkill),
                   "SkillManager should use effective duration when entity is provided");
    }
    
    @Test
    void testEffectiveProgressCalculations() {
        // Test effective progress calculations in SkillManager
        SkillManager skillManager = testEntity.getSkillManager();
        
        // Set entity attributes
        testEntity.setAttackSpeed(2.0);
        
        // Use skill
        skillManager.useSkill(testSkill, testEntity);
        
        // Check effective progress calculations
        double cooldownProgress = skillManager.getEffectiveCooldownProgress(testSkill, testEntity);
        double durationProgress = skillManager.getEffectiveDurationProgress(testSkill, testEntity);
        
        // Progress should be 1.0 (100%) immediately after using the skill
        assertEquals(1.0, cooldownProgress, 0.01, "Cooldown progress should be 100% immediately after use");
        assertEquals(1.0, durationProgress, 0.01, "Duration progress should be 100% immediately after use");
    }
    
    @Test
    void testBackwardCompatibility() {
        // Test that the old useSkill method still works
        SkillManager skillManager = testEntity.getSkillManager();
        
        // Use skill without entity context (deprecated method)
        skillManager.useSkill(testSkill);
        
        // Should use base values
        assertEquals(testSkill.getCooldownTicks(), skillManager.getRemainingCooldown(testSkill),
                   "Deprecated method should use base cooldown");
        assertEquals(testSkill.getDurationTicks(), skillManager.getRemainingDuration(testSkill),
                   "Deprecated method should use base duration");
    }
    
    /**
     * Test entity class that provides access to skillManager for testing.
     */
    private static class TestEntity extends Entity {
        public TestEntity(double x, double y, int width, int height, double speed, 
                         double maxHealth, double maxStamina, double maxMana, 
                         double maxShield, double maxArmor, Weapon weapon) {
            super(x, y, width, height, speed, maxHealth, maxStamina, maxMana, maxShield, maxArmor, weapon);
        }
        
        @Override
        public void update(Object... args) {
            // Test implementation - just update skill manager
            skillManager.update();
        }
        
        @Override
        public void draw(Graphics2D g, int offsetX, int offsetY) {
            // Test implementation - do nothing for testing
        }
        
        public SkillManager getSkillManager() {
            return skillManager;
        }
    }
}
