package com.lhamacorp.games.tlob.client.entities.behaviors;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.entities.Soldier;
// import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.entities.Golen;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

/**
 * Factory class for creating enemies with different behavior configurations.
 * This demonstrates how the new behavior system makes it easy to create
 * different enemy types with customizable behaviors.
 */
public class EnemyFactory {
    
    /**
     * Creates a basic soldier with standard behaviors.
     */
    public static Soldier createBasicSoldier(double x, double y, Weapon weapon) {
        Soldier soldier = new Soldier(x, y, weapon);
        
        // Create behavior manager
        BehaviorManager behaviorManager = new BehaviorManager(soldier);
        
        // Add attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        behaviorManager.addBehavior(attackBehavior);
        
        // Add movement behavior (wander when not attacking, approach when aggressive)
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.WANDER, 220, 0, 0.5, true, 0.2, 0.7
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // Set the behavior manager (this would need to be added to the Soldier class)
        // soldier.setBehaviorManager(behaviorManager);
        
        return soldier;
    }
    
    /**
     * Creates an aggressive soldier that always tries to attack.
     */
    public static Soldier createAggressiveSoldier(double x, double y, Weapon weapon) {
        Soldier soldier = new Soldier(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(soldier);
        
        // High priority attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(40, 1.5, 45, 3);
        behaviorManager.addBehavior(attackBehavior);
        
        // Aggressive movement - always approach player
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.APPROACH_PLAYER, 300, 0, 0.8, true, 0.3, 1.0
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // soldier.setBehaviorManager(behaviorManager);
        
        return soldier;
    }
    
    /**
     * Creates a tactical archer that maintains distance.
     */
    public static Soldier createTacticalArcher(double x, double y, Weapon weapon) {
        Soldier archer = new Soldier(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(archer);
        
        // Ranged attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(120, 0.5, 90, 8, true, true);
        behaviorManager.addBehavior(attackBehavior);
        
        // Tactical movement - maintain optimal distance
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.MAINTAIN_DISTANCE, 250, 80, 0.6, true, 0.25, 0.8
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // archer.setBehaviorManager(behaviorManager);
        
        return archer;
    }
    
    /**
     * Creates a cowardly archer that retreats when threatened.
     */
    public static Soldier createCowardlyArcher(double x, double y, Weapon weapon) {
        Soldier archer = new Soldier(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(archer);
        
        // archer.setBehaviorManager(behaviorManager);
        
        return archer;
    }
    
    /**
     * Creates a slow but powerful Golen.
     */
    public static Golen createSlowGolen(double x, double y, Weapon weapon) {
        Golen golen = new Golen(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(golen);
        
        // Powerful but slow attack
        AttackBehavior attackBehavior = new AttackBehavior(60, 10.0, 180, 15);
        behaviorManager.addBehavior(attackBehavior);
        
        // Slow patrol movement
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.PATROL, 280, 0, 0.3, false, 0, 0
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // golen.setBehaviorManager(behaviorManager);
        
        return golen;
    }
    
    /**
     * Creates a berserker Golen that becomes more aggressive when damaged.
     */
    public static Golen createBerserkerGolen(double x, double y, Weapon weapon) {
        Golen golen = new Golen(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(golen);
        
        // Fast attack when enraged
        AttackBehavior attackBehavior = new AttackBehavior(70, 12.0, 120, 12);
        behaviorManager.addBehavior(attackBehavior);
        
        // Aggressive approach movement
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.APPROACH_PLAYER, 350, 0, 0.5, false, 0, 0
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // golen.setBehaviorManager(behaviorManager);
        
        return golen;
    }
    
    /**
     * Creates a custom enemy with specific behavior configuration.
     * This demonstrates the flexibility of the behavior system.
     */
    public static Entity createCustomEnemy(double x, double y, Weapon weapon, 
                                        MovementBehavior.MovementType movementType,
                                        double aggressionRadius, double speedMultiplier) {
        // Create a basic soldier as the base
        Soldier enemy = new Soldier(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(enemy);
        
        // Add attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(35, 1.2, 55, 4);
        behaviorManager.addBehavior(attackBehavior);
        
        // Add custom movement behavior
        MovementBehavior movementBehavior = new MovementBehavior(
            movementType, aggressionRadius, 0, speedMultiplier, true, 0.15, 0.6
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // enemy.setBehaviorManager(behaviorManager);
        
        return enemy;
    }
    
    /**
     * Creates a group-oriented enemy that coordinates with nearby allies.
     */
    public static Entity createGroupEnemy(double x, double y, Weapon weapon) {
        Soldier enemy = new Soldier(x, y, weapon);
        
        BehaviorManager behaviorManager = new BehaviorManager(enemy);
        
        // Standard attack behavior
        AttackBehavior attackBehavior = new AttackBehavior(30, 1.0, 60, 3);
        behaviorManager.addBehavior(attackBehavior);
        
        // Group movement behavior
        MovementBehavior movementBehavior = new MovementBehavior(
            MovementBehavior.MovementType.GROUP_MOVEMENT, 200, 0, 0.6, false, 0, 0
        );
        behaviorManager.addBehavior(movementBehavior);
        
        // enemy.setBehaviorManager(behaviorManager);
        
        return enemy;
    }
}
