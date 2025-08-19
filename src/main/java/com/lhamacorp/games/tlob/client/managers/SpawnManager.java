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
    private static final double PERK_CHANCE_BASE = 0.20; // 0% chance at map 0
    private static final double PERK_CHANCE_SCALING = 0.20; // +20% per map completed
    private static final double MAX_PERK_CHANCE = 0.8; // Cap at 80% chance
    private static final int MAX_PERKS_PER_ENEMY = 10; // Maximum perks per enemy (increased from 10 to allow full 10)
    private static final int GOLEN_PERK_COUNT = 5; // Golen get 5 perks by default
    private static final int HIGH_PERK_THRESHOLD = 5; // Enemies with more than this many perks are considered "high-perk"
    private static final int HIGH_PERK_REPLACEMENT_RATIO = 5; // Each high-perk enemy replaces this many low-perk enemies
    private static final int HIGH_PERK_SPAWN_THRESHOLD = 20; // Minimum enemies required to start spawning high-perk enemies

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
     * High-perk enemies (with more than 5 perks) replace multiple low-perk enemies.
     */
    public void spawn(TileMap map, Player player, List<Entity> out, int completedMaps, int tileSize) {
        out.clear();
        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int totalCount = Math.max(1, (int) (base * mult));

        int golenCount = calculateGolenSpawnCount(totalCount);
        int regularEnemyCount = totalCount - (golenCount * getGolenReplacementRatio());

        // Spawn Golen enemies first
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

        // Calculate how many high-perk enemies we can spawn
        // Each high-perk enemy replaces 5 low-perk enemies, so we need to calculate
        // how many we can afford to replace while maintaining a minimum enemy count
        int highPerkEnemyCount = calculateHighPerkEnemyCount(regularEnemyCount, completedMaps);
        
        // Calculate how many low-perk enemies we can spawn after replacements
        // If we spawn 2 high-perk enemies, they replace 10 low-perk enemies
        // So we can only spawn (regularEnemyCount - 10) low-perk enemies
        int lowPerkEnemyCount = Math.max(0, regularEnemyCount - (highPerkEnemyCount * HIGH_PERK_REPLACEMENT_RATIO));
        


        // Spawn high-perk enemies (Soldiers and Archers with 6+ perks)
        for (int i = 0; i < highPerkEnemyCount; i++) {
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

                // Apply high perks (6-10 perks) to make them elite
                applyHighPerkEnemyPerks(enemy, completedMaps);

                out.add(enemy);
            }
        }

        // Spawn low-perk enemies (Soldiers and Archers with 0-5 perks)
        for (int i = 0; i < lowPerkEnemyCount; i++) {
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

                // Apply regular perks (0-5 perks)
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
        for (int i = 0; i < GOLEN_PERK_COUNT; i++) {
            applyRandomEnemyPerk(golen, completedMaps);
        }
    }

    /**
     * Applies perks to a high-perk enemy (Soldiers and Archers with 6+ perks).
     * These enemies replace multiple low-perk enemies.
     */
    protected void applyHighPerkEnemyPerks(Entity enemy, int completedMaps) {
        if (completedMaps <= 0) return; // No perks for first map

        // High-perk enemies get 6-10 perks to make them elite
        // They replace 5 low-perk enemies, so they should be significantly stronger
        int perkCount = HIGH_PERK_THRESHOLD + 1 + rng.nextInt(5); // 6-10 perks
        
        // Apply the perks
        for (int i = 0; i < perkCount; i++) {
            applyRandomEnemyPerk(enemy, completedMaps);
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
     * @return the replacement ratio (10 regular enemies per Golen)
     */
    public static int getGolenReplacementRatio() {
        return 10;
    }

    /**
     * Gets the perk threshold above which enemies are considered "high-perk".
     *
     * @return the high perk threshold (5 perks)
     */
    public static int getHighPerkThreshold() {
        return HIGH_PERK_THRESHOLD;
    }

    /**
     * Gets the number of low-perk enemies each high-perk enemy replaces.
     *
     * @return the replacement ratio (5 low-perk enemies per high-perk enemy)
     */
    public static int getHighPerkReplacementRatio() {
        return HIGH_PERK_REPLACEMENT_RATIO;
    }

    /**
     * Gets the minimum enemy count required to start spawning high-perk enemies.
     *
     * @return the high-perk spawn threshold (20 enemies)
     */
    public static int getHighPerkSpawnThreshold() {
        return HIGH_PERK_SPAWN_THRESHOLD;
    }

    /**
     * Calculates how many Golen enemies can spawn for a given total enemy count.
     * Each Golen replaces 10 regular enemies, so we need to ensure there are enough
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
        // Each Golen replaces 10 enemies, so we need at least 10 enemies per Golen
        int maxGolenCount = Math.min(getMaxGolenPerMap(),
            totalEnemyCount / getGolenReplacementRatio());

        // Ensure we don't go below a minimum enemy count (e.g., at least 10 enemies remaining)
        int minEnemiesToKeep = 10;
        int maxGolenForMinEnemies = (totalEnemyCount - minEnemiesToKeep) / getGolenReplacementRatio();
        maxGolenCount = Math.min(maxGolenCount, Math.max(0, maxGolenForMinEnemies));

        return rng.nextInt(maxGolenCount + 1); // 0 to maxGolenCount
    }

    /**
     * Calculates how many high-perk enemies (Soldiers and Archers with 6+ perks)
     * can spawn for a given total enemy count.
     *
     * @param totalEnemyCount the total number of enemies on the map
     * @param completedMaps the number of maps completed
     * @return the number of high-perk enemies that can spawn
     */
    public int calculateHighPerkEnemyCount(int totalEnemyCount, int completedMaps) {
        if (completedMaps <= 0) return 0; // No high-perk enemies on first map
        
        if (totalEnemyCount <= HIGH_PERK_SPAWN_THRESHOLD) return 0; // Need more than 20 enemies to consider high-perk spawning

        // Calculate how many high-perk enemies can spawn based on available enemies
        // Each high-perk enemy replaces 5 low-perk enemies
        // We need to ensure we keep at least 5 enemies as low-perk
        int minLowPerkEnemies = 5;
        int availableEnemiesForReplacement = totalEnemyCount - minLowPerkEnemies;
        
        // Calculate max high-perk enemies we can afford
        int maxHighPerkCount = availableEnemiesForReplacement / HIGH_PERK_REPLACEMENT_RATIO;
        
        // Cap at a reasonable maximum (e.g., 4 high-perk enemies max)
        maxHighPerkCount = Math.min(maxHighPerkCount, 4);
        
        // Ensure we don't go negative
        maxHighPerkCount = Math.max(0, maxHighPerkCount);
        
        return rng.nextInt(maxHighPerkCount + 1); // 0 to maxHighPerkCount
    }

}
