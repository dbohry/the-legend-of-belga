package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the new behavior system.
 * Demonstrates how behaviors work and can be composed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BehaviorSystemTest {
    
    private Entity testEntity;
    private Player testPlayer;
    private TileMap testMap;
    private BehaviorManager behaviorManager;
    
    @BeforeEach
    void setUp() {
        // Create test entities with proper constructors
        testEntity = new Soldier(100, 100, new Sword(1, 30, 10, 3, 60));
        testPlayer = new Player(200, 200, new Sword(1, 30, 10, 3, 60));
        
        // Create a simple test map (2x2 array)
        int[][] testTiles = new int[2][2];
        testTiles[0][0] = TileMap.FLOOR;
        testTiles[0][1] = TileMap.FLOOR;
        testTiles[1][0] = TileMap.FLOOR;
        testTiles[1][1] = TileMap.FLOOR;
        testMap = new TileMap(testTiles);
        
        // Create behavior manager
        behaviorManager = new BehaviorManager(testEntity);
    }
    
    @Test
    void testBehaviorManagerCreation() {
        assertNotNull(behaviorManager);
        assertEquals(0, behaviorManager.getBehaviorCount());
        assertEquals("None", behaviorManager.getCurrentBehaviorName());
    }
    
    @Test
    void testAddingBehaviors() {
        // Add attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        behaviorManager.addBehavior(attackBehavior);
        
        // Add movement behavior
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.WANDER, 200, 0, 0.5, false, 0, 0
        );
        behaviorManager.addBehavior(movementBehavior);
        
        assertEquals(2, behaviorManager.getBehaviorCount());
        assertNotNull(behaviorManager.getBehavior("Attack"));
        assertNotNull(behaviorManager.getBehavior("Movement"));
    }
    
    @Test
    void testBehaviorPriority() {
        // Add behaviors with different priorities
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3); // Priority 100
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.WANDER, 200, 0, 0.5, false, 0, 0
        ); // Priority 50
        
        behaviorManager.addBehavior(movementBehavior);
        behaviorManager.addBehavior(attackBehavior);
        
        // Position player within attack range so AttackBehavior can execute
        testPlayer.setPosition(110, 100); // 10 units away from entity (within 30 unit range)
        
        // Attack behavior should have higher priority and be executed first
        behaviorManager.update(testPlayer, testMap, null);
        assertEquals("Attack", behaviorManager.getCurrentBehaviorName());
    }
    
    @Test
    void testBehaviorExecution() {
        // Add attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        behaviorManager.addBehavior(attackBehavior);
        
        // Place player within attack range
        testPlayer.setPosition(110, 100); // 10 units away from entity
        
        // Update behavior manager
        behaviorManager.update(testPlayer, testMap, null);
        
        // Attack behavior should be active
        assertEquals("Attack", behaviorManager.getCurrentBehaviorName());
        assertTrue(behaviorManager.isBehaviorActive("Attack"));
    }
    
    @Test
    void testBehaviorSwitching() {
        // Add both behaviors
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.WANDER, 200, 0, 0.5, false, 0, 0
        );
        
        behaviorManager.addBehavior(attackBehavior);
        behaviorManager.addBehavior(movementBehavior);
        
        // Initially, attack behavior should be active (player within range)
        testPlayer.setPosition(110, 100);
        behaviorManager.update(testPlayer, testMap, null);
        assertEquals("Attack", behaviorManager.getCurrentBehaviorName());
        
        // Move player out of attack range
        testPlayer.setPosition(300, 300);
        behaviorManager.update(testPlayer, testMap, null);
        
        // Movement behavior should now be active
        assertEquals("Movement", behaviorManager.getCurrentBehaviorName());
    }
    
    @Test
    void testBehaviorRemoval() {
        // Add behaviors
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.WANDER, 200, 0, 0.5, false, 0, 0
        );
        
        behaviorManager.addBehavior(attackBehavior);
        behaviorManager.addBehavior(movementBehavior);
        
        assertEquals(2, behaviorManager.getBehaviorCount());
        
        // Remove attack behavior
        behaviorManager.removeBehavior("Attack");
        
        assertEquals(1, behaviorManager.getBehaviorCount());
        assertNull(behaviorManager.getBehavior("Attack"));
        assertNotNull(behaviorManager.getBehavior("Movement"));
    }
    
    @Test
    void testBehaviorReset() {
        // Add attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        behaviorManager.addBehavior(attackBehavior);
        
        // Execute attack
        testPlayer.setPosition(110, 100);
        behaviorManager.update(testPlayer, testMap, null);
        
        // Reset behaviors
        behaviorManager.resetBehaviors();
        
        assertEquals("None", behaviorManager.getCurrentBehaviorName());
    }
    
    @Test
    void testCustomBehaviorConfiguration() {
        // Create custom movement behavior
        MovementBehavior customMovement = new MovementBehavior(
            MovementBehavior.MovementType.PATROL, 150, 0, 0.8, true, 0.3, 1.0
        );
        
        behaviorManager.addBehavior(customMovement);
        
        // Test that custom configuration is applied
        MovementBehavior retrieved = (MovementBehavior) behaviorManager.getBehavior("Movement");
        assertEquals(MovementBehavior.MovementType.PATROL, retrieved.getMovementType());
        assertEquals(150, retrieved.getAggressionRadius());
        assertEquals(0.8, retrieved.getSpeedMultiplier());
    }
    
    @Test
    void testDebugInformation() {
        // Add behaviors
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.WANDER, 200, 0, 0.5, false, 0, 0
        );
        
        behaviorManager.addBehavior(attackBehavior);
        behaviorManager.addBehavior(movementBehavior);
        
        String debugInfo = behaviorManager.getDebugInfo();
        
        assertTrue(debugInfo.contains("BehaviorManager Debug Info"));
        assertTrue(debugInfo.contains("Total Behaviors: 2"));
        assertTrue(debugInfo.contains("Attack (Priority: 100)"));
        assertTrue(debugInfo.contains("Movement (Priority: 50)"));
    }
    
    @Test
    void testEnemyFactory() {
        // Test creating different enemy types with behaviors
        Entity basicSoldier = EnemyFactory.createBasicSoldier(100, 100, new Sword(1, 30, 10, 3, 60));
        Entity aggressiveSoldier = EnemyFactory.createAggressiveSoldier(150, 150, new Sword(1, 30, 10, 3, 60));
        Entity tacticalArcher = EnemyFactory.createTacticalArcher(200, 200, new Sword(1, 30, 10, 3, 60));
        
        assertNotNull(basicSoldier);
        assertNotNull(aggressiveSoldier);
        assertNotNull(tacticalArcher);
        
        // All should be instances of their respective classes
        assertTrue(basicSoldier instanceof Soldier);
        assertTrue(aggressiveSoldier instanceof Soldier);
        assertTrue(tacticalArcher instanceof Soldier); // Note: Factory creates Soldier for now
    }
    
    @Test
    void testCustomEnemyCreation() {
        // Test creating a custom enemy with specific behavior
        Entity customEnemy = EnemyFactory.createCustomEnemy(
            100, 100, new Sword(1, 30, 10, 3, 60),
            MovementBehavior.MovementType.GROUP_MOVEMENT,
            180, 0.7
        );
        
        assertNotNull(customEnemy);
        assertTrue(customEnemy instanceof Soldier);
    }
    
    // Helper method to set player position (this would need to be implemented in Player class)
    private void setPlayerPosition(Player player, double x, double y) {
        // This is a placeholder - in the actual implementation, Player would need
        // setter methods for position or the fields would need to be accessible
        try {
            java.lang.reflect.Field xField = Player.class.getDeclaredField("x");
            java.lang.reflect.Field yField = Player.class.getDeclaredField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);
            xField.set(player, x);
            yField.set(player, y);
        } catch (Exception e) {
            // Test will fail if reflection doesn't work
            fail("Could not set player position: " + e.getMessage());
        }
    }
}
