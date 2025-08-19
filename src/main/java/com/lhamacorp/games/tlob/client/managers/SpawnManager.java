package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.*;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.util.List;
import java.util.Random;

public class SpawnManager {

    private final Weapon enemyWeapon;
    protected Random rng;

    // Enemy perk system constants
    private static final double PERK_CHANCE_BASE = 0.0; // 0% chance at map 0
    private static final double PERK_CHANCE_SCALING = 0.20; // +20% per map completed
    private static final double MAX_PERK_CHANCE = 0.8; // Cap at 80% chance
    private static final int MAX_PERKS_PER_ENEMY = 10; // Maximum perks per enemy
    private static final int GOLEN_PERK_COUNT = 5; // Golen get 5 perks by default

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

    /**
     * Reseed this spawner's RNG (used on restart / next-level for deterministic runs).
     */
    public void reseed(Random rng) {
        this.rng = (rng != null) ? rng : new Random();
    }

    /**
     * Spawns enemies on the map based on completion level.
     * Golen enemies only spawn on maps with more than 60 foes and replace some regular enemies.
     */
    public void spawn(TileMap map, Player player, List<Entity> out, int completedMaps, int tileSize) {
        out.clear();
        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int totalCount = Math.max(1, (int) (base * mult));

        // Determine Golen spawning logic
        int golenCount = calculateGolenSpawnCount(totalCount);
        // Each Golen replaces 5 regular enemies
        int regularEnemyCount = totalCount - (golenCount * 5);

        // Spawn Golen enemies first (if any)
        for (int i = 0; i < golenCount; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                double x = pos[0] * tileSize + tileSize / 2.0;
                double y = pos[1] * tileSize + tileSize / 2.0;

                Entity golen = new Golen(x, y, enemyWeapon);
                // Golen get 5 perks by default to make them elite enemies
                applyGolenPerks(golen, completedMaps);
                out.add(golen);
            }
        }

        // Spawn regular enemies (Soldiers and Archers)
        for (int i = 0; i < regularEnemyCount; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                double x = pos[0] * tileSize + tileSize / 2.0;
                double y = pos[1] * tileSize + tileSize / 2.0;

                Entity enemy;
                // 20% chance to spawn an Archer, 80% chance for Soldier (no Golen in regular spawns)
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
    protected void applyEnemyPerks(Entity enemy, int completedMaps) {
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
        int perkType = rng.nextInt(7); // 7 different perk types

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
            case 6: // Armor boost
                enemy.increaseArmor(perkStrength);
                break;
        }
    }

    /**
     * Applies perks to a Golen enemy.
     * Golen enemies get 5 perks by default.
     */
    protected void applyGolenPerks(Entity golen, int completedMaps) {
        // Golen get 5 perks by default
        for (int i = 0; i < GOLEN_PERK_COUNT; i++) {
            applyRandomEnemyPerk(golen, completedMaps);
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

    /**
     * Gets the current Golen spawning threshold.
     *
     * @return the minimum enemy count required for Golen to spawn
     */
    public static int getGolenSpawnThreshold() {
        return 60;
    }

    /**
     * Gets the maximum number of Golen that can spawn on a single map.
     *
     * @return the maximum Golen count
     */
    public static int getMaxGolenPerMap() {
        return 10;
    }

    /**
     * Gets the number of regular enemies each Golen replaces.
     *
     * @return the replacement ratio (5 regular enemies per Golen)
     */
    public static int getGolenReplacementRatio() {
        return 5;
    }

    /**
     * Calculates how many Golen enemies can spawn for a given total enemy count.
     * Each Golen replaces 5 regular enemies, so we need to ensure there are enough
     * enemies to replace without going below a minimum threshold.
     *
     * @param totalEnemyCount the total number of enemies on the map
     * @return the number of Golen that can spawn (0 if below threshold or not enough enemies to replace)
     */
    public int calculateGolenSpawnCount(int totalEnemyCount) {
        if (totalEnemyCount <= getGolenSpawnThreshold()) {
            return 0;
        }

        // Calculate how many Golen can spawn based on available enemies to replace
        // Each Golen replaces 5 enemies, so we need at least 5 enemies per Golen
        int maxGolenCount = Math.min(getMaxGolenPerMap(),
            totalEnemyCount / getGolenReplacementRatio());

        // Ensure we don't go below a minimum enemy count (e.g., at least 10 enemies remaining)
        int minEnemiesToKeep = 10;
        int maxGolenForMinEnemies = (totalEnemyCount - minEnemiesToKeep) / getGolenReplacementRatio();
        maxGolenCount = Math.min(maxGolenCount, Math.max(0, maxGolenForMinEnemies));

        return rng.nextInt(maxGolenCount + 1); // 0 to maxGolenCount
    }

    /**
     * Gets spawning statistics for debugging or UI display.
     *
     * @param completedMaps the number of completed maps
     * @return a string describing the current spawning configuration
     */
    public String getSpawningStats(int completedMaps) {
        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int totalCount = Math.max(1, (int) (base * mult));

        StringBuilder stats = new StringBuilder();
        stats.append("Map ").append(completedMaps + 1).append(": ");
        stats.append(totalCount).append(" total enemies");

        if (totalCount > getGolenSpawnThreshold()) {
            int maxGolenCount = Math.min(getMaxGolenPerMap(),
                totalCount / getGolenReplacementRatio());
            // Ensure we don't go below minimum enemy count
            int minEnemiesToKeep = 10;
            int maxGolenForMinEnemies = (totalCount - minEnemiesToKeep) / getGolenReplacementRatio();
            maxGolenCount = Math.min(maxGolenCount, Math.max(0, maxGolenForMinEnemies));

            if (maxGolenCount > 0) {
                stats.append(", up to ").append(maxGolenCount).append(" Golen possible");
                stats.append(" (each replaces ").append(getGolenReplacementRatio()).append(" enemies)");
            } else {
                stats.append(", no Golen (not enough enemies to replace)");
            }
        } else {
            stats.append(", no Golen (below ").append(getGolenSpawnThreshold()).append(" threshold)");
        }

        return stats.toString();
    }
}
