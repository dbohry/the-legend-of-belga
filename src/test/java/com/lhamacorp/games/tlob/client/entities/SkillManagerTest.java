package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.entities.skills.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillManagerTest {

    private SkillManager skillManager;
    private Skill testSkill;
    
    @BeforeEach
    void setUp() {
        skillManager = new SkillManager();
        testSkill = new Skill("Test Skill", "A test skill", 1.0, 30, 10, 40);
    }
    
    @Test
    void testInitialState() {
        assertTrue(skillManager.isSkillReady(testSkill));
        assertFalse(skillManager.isSkillActive(testSkill));
        assertEquals(0, skillManager.getRemainingCooldown(testSkill));
        assertEquals(0, skillManager.getRemainingDuration(testSkill));
    }
    
    @Test
    void testUseSkill() {
        skillManager.useSkill(testSkill);
        
        assertFalse(skillManager.isSkillReady(testSkill));
        assertTrue(skillManager.isSkillActive(testSkill));
        assertEquals(30, skillManager.getRemainingCooldown(testSkill));
        assertEquals(10, skillManager.getRemainingDuration(testSkill));
    }
    
    @Test
    void testUpdate() {
        skillManager.useSkill(testSkill);
        
        // Update once
        skillManager.update();
        
        assertEquals(29, skillManager.getRemainingCooldown(testSkill));
        assertEquals(9, skillManager.getRemainingDuration(testSkill));
        
        // Update until cooldown expires
        for (int i = 0; i < 28; i++) {
            skillManager.update();
        }
        
        assertEquals(1, skillManager.getRemainingCooldown(testSkill));
        assertEquals(0, skillManager.getRemainingDuration(testSkill));
        
        // Final update
        skillManager.update();
        
        assertEquals(0, skillManager.getRemainingCooldown(testSkill));
        assertEquals(0, skillManager.getRemainingDuration(testSkill));
        assertTrue(skillManager.isSkillReady(testSkill));
        assertFalse(skillManager.isSkillActive(testSkill));
    }
    
    @Test
    void testCooldownProgress() {
        skillManager.useSkill(testSkill);
        
        assertEquals(1.0, skillManager.getCooldownProgress(testSkill), 0.01);
        
        skillManager.update();
        assertEquals(29.0 / 30.0, skillManager.getCooldownProgress(testSkill), 0.01);
        
        // Update until cooldown is half done
        for (int i = 0; i < 14; i++) {
            skillManager.update();
        }
        
        assertEquals(0.5, skillManager.getCooldownProgress(testSkill), 0.01);
    }
    
    @Test
    void testDurationProgress() {
        skillManager.useSkill(testSkill);
        
        assertEquals(1.0, skillManager.getDurationProgress(testSkill), 0.01);
        
        skillManager.update();
        assertEquals(9.0 / 10.0, skillManager.getDurationProgress(testSkill), 0.01);
        
        // Update until duration is half done
        for (int i = 0; i < 4; i++) {
            skillManager.update();
        }
        
        assertEquals(0.5, skillManager.getDurationProgress(testSkill), 0.01);
    }
    
    @Test
    void testMultipleSkills() {
        Skill skill1 = new Skill("Skill 1", "First skill", 1.0, 20, 5, 30);
        Skill skill2 = new Skill("Skill 2", "Second skill", 2.0, 40, 8, 50);
        
        skillManager.useSkill(skill1);
        skillManager.useSkill(skill2);
        
        assertEquals(2, skillManager.getSkillsOnCooldown());
        assertEquals(2, skillManager.getActiveSkills());
        
        // Update until first skill is ready
        for (int i = 0; i < 20; i++) {
            skillManager.update();
        }
        
        assertEquals(1, skillManager.getSkillsOnCooldown());
        assertEquals(0, skillManager.getActiveSkills());
        assertTrue(skillManager.isSkillReady(skill1));
        assertFalse(skillManager.isSkillReady(skill2));
    }
    
    @Test
    void testResetAll() {
        skillManager.useSkill(testSkill);
        assertFalse(skillManager.isSkillReady(testSkill));
        
        skillManager.resetAll();
        assertTrue(skillManager.isSkillReady(testSkill));
        assertEquals(0, skillManager.getSkillsOnCooldown());
        assertEquals(0, skillManager.getActiveSkills());
    }
    
    @Test
    void testSkillNotUsed() {
        // Test that skills that haven't been used return appropriate values
        assertEquals(0, skillManager.getRemainingCooldown(testSkill));
        assertEquals(0, skillManager.getRemainingDuration(testSkill));
        assertEquals(0.0, skillManager.getCooldownProgress(testSkill), 0.01);
        assertEquals(0.0, skillManager.getDurationProgress(testSkill), 0.01);
    }
}
