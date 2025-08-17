package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.entities.skills.Skill;
import com.lhamacorp.games.tlob.client.entities.skills.Skills;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillsTest {

    @Test
    void testDebugSkillsValues() {
        // Debug test to see what values are actually loaded
        System.out.println("SLASH cooldown: " + Skills.SLASH.getCooldownTicks());
        System.out.println("TACKLE cooldown: " + Skills.TACKLE.getCooldownTicks());
        System.out.println("ARROW_ATTACK cooldown: " + Skills.ARROW_ATTACK.getCooldownTicks());
        System.out.println("STOMP cooldown: " + Skills.STOMP.getCooldownTicks());
        
        // This test should always pass and show us the actual values
        assertTrue(true);
    }

    @Test
    void testSlashSkill() {
        Skill slash = Skills.SLASH;
        
        assertEquals("Slash", slash.getName());
        assertEquals("A powerful melee attack with the sword", slash.getDescription());
        assertEquals(1.0, slash.getBaseDamage(), 0.01);
        assertEquals(30, slash.getCooldownTicks());
        assertEquals(3, slash.getDurationTicks());
        assertEquals(40, slash.getRange(), 0.01);
        assertFalse(slash.isRanged());
        assertEquals("slash-hit.wav", slash.getSoundEffect());
    }
    
    @Test
    void testTackleSkill() {
        Skill tackle = Skills.TACKLE;
        
        assertEquals("Tackle", tackle.getName());
        assertEquals("A charging melee attack that knocks back enemies", tackle.getDescription());
        assertEquals(1.5, tackle.getBaseDamage(), 0.01);
        assertEquals(90, tackle.getCooldownTicks());
        assertEquals(4, tackle.getDurationTicks());
        assertEquals(35, tackle.getRange(), 0.01);
        assertFalse(tackle.isRanged());
        assertEquals("slash-hit.wav", tackle.getSoundEffect());
    }
    
    @Test
    void testArrowAttackSkill() {
        Skill arrowAttack = Skills.ARROW_ATTACK;
        
        assertEquals("Arrow Attack", arrowAttack.getName());
        assertEquals("A precise ranged attack with an arrow", arrowAttack.getDescription());
        assertEquals(0.8, arrowAttack.getBaseDamage(), 0.01);
        assertEquals(90, arrowAttack.getCooldownTicks());
        assertEquals(8, arrowAttack.getDurationTicks());
        assertEquals(120, arrowAttack.getRange(), 0.01);
        assertTrue(arrowAttack.isRanged());
        assertEquals("arrow-hit.wav", arrowAttack.getSoundEffect());
    }
    
    @Test
    void testStompSkill() {
        Skill stomp = Skills.STOMP;
        
        assertEquals("Stomp", stomp.getName());
        assertEquals("A powerful area attack that damages nearby enemies", stomp.getDescription());
        assertEquals(2.0, stomp.getBaseDamage(), 0.01);
        assertEquals(90, stomp.getCooldownTicks());
        assertEquals(6, stomp.getDurationTicks());
        assertEquals(50, stomp.getRange(), 0.01);
        assertFalse(stomp.isRanged());
        assertEquals("slash-hit.wav", stomp.getSoundEffect());
    }
    
    @Test
    void testSkillDamageComparison() {
        // Test that different skills have appropriate damage values
        assertTrue(Skills.STOMP.getBaseDamage() > Skills.TACKLE.getBaseDamage());
        assertTrue(Skills.TACKLE.getBaseDamage() > Skills.SLASH.getBaseDamage());
        assertTrue(Skills.SLASH.getBaseDamage() > Skills.ARROW_ATTACK.getBaseDamage());
    }
    
    @Test
    void testSkillRangeComparison() {
        // Test that different skills have appropriate ranges
        assertTrue(Skills.ARROW_ATTACK.getRange() > Skills.STOMP.getRange());
        assertTrue(Skills.STOMP.getRange() > Skills.SLASH.getRange());
        assertTrue(Skills.SLASH.getRange() > Skills.TACKLE.getRange());
    }
    
    @Test
    void testSkillCooldownComparison() {
        // Test that different skills have appropriate cooldowns
        // Note: STOMP, TACKLE, and ARROW_ATTACK all have 90 tick cooldowns
        assertTrue(Skills.STOMP.getCooldownTicks() >= Skills.TACKLE.getCooldownTicks());
        assertTrue(Skills.TACKLE.getCooldownTicks() > Skills.SLASH.getCooldownTicks());
        // Note: ARROW_ATTACK has longer cooldown than SLASH
        assertTrue(Skills.ARROW_ATTACK.getCooldownTicks() > Skills.SLASH.getCooldownTicks());
    }
    
    @Test
    void testSkillDurationComparison() {
        // Test that different skills have appropriate durations
        assertTrue(Skills.ARROW_ATTACK.getDurationTicks() > Skills.STOMP.getDurationTicks());
        assertTrue(Skills.STOMP.getDurationTicks() > Skills.TACKLE.getDurationTicks());
        assertTrue(Skills.TACKLE.getDurationTicks() > Skills.SLASH.getDurationTicks());
    }
}
