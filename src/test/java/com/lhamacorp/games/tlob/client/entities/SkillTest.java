package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.entities.skills.Skill;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillTest {

    @Test
    void testSkillCreation() {
        Skill skill = new Skill("Test Skill", "A test skill", 2.5, 60, 10, 50, false, "test.wav");
        
        assertEquals("Test Skill", skill.getName());
        assertEquals("A test skill", skill.getDescription());
        assertEquals(2.5, skill.getBaseDamage(), 0.01);
        assertEquals(60, skill.getCooldownTicks());
        assertEquals(10, skill.getDurationTicks());
        assertEquals(50, skill.getRange(), 0.01);
        assertFalse(skill.isRanged());
        assertEquals("test.wav", skill.getSoundEffect());
    }
    
    @Test
    void testMeleeSkillConstructor() {
        Skill skill = new Skill("Melee Skill", "A melee attack", 1.0, 30, 5, 40);
        
        assertEquals("Melee Skill", skill.getName());
        assertEquals(1.0, skill.getBaseDamage(), 0.01);
        assertFalse(skill.isRanged());
        assertEquals("slash-hit.wav", skill.getSoundEffect()); // Default sound effect
    }
    
    @Test
    void testRangedSkill() {
        Skill skill = new Skill("Ranged Skill", "A ranged attack", 0.8, 90, 15, 120, true, "arrow.wav");
        
        assertTrue(skill.isRanged());
        assertEquals(120, skill.getRange(), 0.01);
        assertEquals("arrow.wav", skill.getSoundEffect());
    }
    
    @Test
    void testEffectiveDamageCalculation() {
        Skill skill = new Skill("Damage Test", "Test damage calculation", 2.0, 30, 5, 40);
        
        // Create a mock entity with damage multiplier
        Entity entity = new TestEntity(0, 0, 20, 20, 3.0, 10.0, 5.0, 5.0, 0, 0, null, "Test", Alignment.NEUTRAL);
        entity.increaseAttackDamageByPercent(0.5); // +50% damage
        
        // Base damage 2.0 * 1.5 multiplier = 3.0
        assertEquals(3.0, skill.getEffectiveDamage(entity), 0.01);
    }
    
    @Test
    void testToString() {
        Skill skill = new Skill("Test", "Description", 1.5, 45, 3, 35);
        String expected = "Test (1.5 damage, 45 ticks cooldown)";
        assertEquals(expected, skill.toString());
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
