package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.util.List;
import java.util.Random;

public class SpawnManager {

    private final Weapon enemyWeapon;
    private Random rng;

    // Enemy perk system constants
    private static final double PERK_CHANCE_BASE = 0.0; // 0% chance at map 0
    private static final double PERK_CHANCE_SCALING = 0.15; // +15% per map completed
    private static final double MAX_PERK_CHANCE = 0.8; // Cap at 80% chance
    private static final int MAX_PERKS_PER_ENEMY = 3; // Maximum perks per enemy

    /**
     * Creates a spawn manager with non-deterministic random number generation.
     */
    public SpawnManager(Weapon enemyWeapon) {
        this(enemyWeapon, new Random());
    }

    /**
     * Creates a spawn manager with the specified random number generator.
     */
    public SpawnManager(Weapon enemyWeapon, Random rng) {
        this.enemyWeapon = enemyWeapon;
        this.rng = (rng != null) ? rng : new Random();
    }

    /** Reseed this spawner's RNG (used on restart / next-level for deterministic runs). */
    public void reseed(Random rng) {
        this.rng = (rng != null) ? rng : new Random();
    }

    /**
     * Spawns enemies on the map based on completion level.
     */
    public void spawn(TileMap map, Player player, List<Entity> out, int completedMaps, int tileSize) {
        out.clear();
        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int count = Math.max(1, (int) (base * mult));

        for (int i = 0; i < count; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                double x = pos[0] * tileSize + tileSize / 2.0;
                double y = pos[1] * tileSize + tileSize / 2.0;
                
                Entity enemy;
                // 20% chance to spawn an Archer, 80% chance for Soldier
                if (rng.nextDouble() < 0.20) {
                    enemy = new Archer(x, y, enemyWeapon);
                } else {
                    enemy = new Soldier(x, y, enemyWeapon);
                }
                
                // Apply perks based on map completion
                applyEnemyPerks(enemy, completedMaps);
                
                out.add(enemy);
            }
        }
    }

    /**
     * Applies perks to an enemy based on map completion level.
     * Higher map completion = more perks and stronger perks.
     */
    private void applyEnemyPerks(Entity enemy, int completedMaps) {
        if (completedMaps <= 0) return; // No perks for first map
        
        // Calculate perk chance based on map completion
        double perkChance = Math.min(MAX_PERK_CHANCE, 
            PERK_CHANCE_BASE + (PERK_CHANCE_SCALING * completedMaps));
        
        // Determine how many perks this enemy gets
        int perkCount = 0;
        if (rng.nextDouble() < perkChance) {
            // Base perk count increases with map completion
            perkCount = 1 + (completedMaps / 3); // +1 perk every 3 maps
            perkCount = Math.min(perkCount, MAX_PERKS_PER_ENEMY);
        }
        
        // Apply the perks
        for (int i = 0; i < perkCount; i++) {
            applyRandomEnemyPerk(enemy, completedMaps);
        }
    }

    /**
     * Applies a random perk to an enemy, with perk strength scaling with map completion.
     */
    private void applyRandomEnemyPerk(Entity enemy, int completedMaps) {
        // Perk strength increases with map completion
        double perkStrength = 0.05 + (0.02 * completedMaps); // 5% base + 2% per map
        perkStrength = Math.min(perkStrength, 0.5); // Cap at 50%
        
        // Choose perk type based on enemy type and random chance
        int perkType = rng.nextInt(6); // 6 different perk types
        
        switch (perkType) {
            case 0: // Health boost
                enemy.increaseMaxHealthByPercent(perkStrength);
                break;
            case 1: // Speed boost
                enemy.increaseMoveSpeedByPercent(perkStrength);
                break;
            case 2: // Damage boost
                enemy.increaseAttackDamageByPercent(perkStrength);
                break;
            case 3: // Stamina boost (if enemy has stamina)
                if (enemy.getMaxStamina() > 0) {
                    enemy.increaseMaxStaminaByPercent(perkStrength);
                }
                break;
            case 4: // Weapon range boost
                enemy.increaseWeaponRangeByPercent(perkStrength);
                break;
            case 5: // Weapon width boost
                enemy.increaseWeaponWidth(1);
                break;
        }
    }

    /**
     * Gets the current perk chance for enemies based on map completion.
     * Useful for UI display or debugging.
     */
    public double getCurrentPerkChance(int completedMaps) {
        return Math.min(MAX_PERK_CHANCE, 
            PERK_CHANCE_BASE + (PERK_CHANCE_SCALING * completedMaps));
    }

    /**
     * Gets the maximum number of perks an enemy can have at the given map completion.
     */
    public int getMaxPerksPerEnemy(int completedMaps) {
        if (completedMaps <= 0) return 0;
        return Math.min(MAX_PERKS_PER_ENEMY, 1 + (completedMaps / 3));
    }
}
