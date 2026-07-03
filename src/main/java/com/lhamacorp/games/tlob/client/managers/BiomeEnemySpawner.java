package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Archer;
import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Golen;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.entities.Soldier;
import com.lhamacorp.games.tlob.client.maps.Biome;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Bow;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles spawning biome-specific enemies with different characteristics and spawn patterns
 * based on the current biome.
 * <p>
 * Which mobs exist, what they cost to spawn, and how likely they are per biome is entirely
 * data-driven via {@link #registry}. To add a new mob type, register it once (see
 * {@link #registerDefaultMobs()} for examples) - no other code in this class needs to change.
 */
public class BiomeEnemySpawner extends SpawnManager {

    /**
     * Biome-specific enemy count multipliers. These affect the total number of enemies
     * spawned in each biome.
     */
    private static final double[] DEFAULT_BIOME_ENEMY_MULTIPLIERS = {
        1.0,   // MEADOWS: standard count
        1.2,   // FOREST: more enemies
        0.8,   // CAVE: fewer enemies
        1.1,   // DESERT: slightly more
        1.3    // VULCAN: more enemies (challenging)
    };

    private final MobRegistry registry = new MobRegistry();

    public BiomeEnemySpawner(Weapon enemyWeapon) {
        super(enemyWeapon);
        registerDefaultMobs();
    }

    public BiomeEnemySpawner(Weapon enemyWeapon, Random rng) {
        super(enemyWeapon, rng);
        registerDefaultMobs();
    }

    /** The registry backing this spawner. Register additional mobs on it to extend the roster. */
    public MobRegistry getRegistry() {
        return registry;
    }

    @Override
    public void spawn(TileMap map, Player player, List<Entity> out, int completedMaps, int tileSize) {
        Biome biome = map.getBiome();

        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int totalCount = Math.max(1, (int) (base * mult));
        totalCount = applyBiomeAdjustments(totalCount, biome);

        out.clear();

        List<MobRegistry.MobEntry> elitePool = new ArrayList<>();
        List<MobRegistry.MobEntry> regularPool = new ArrayList<>();
        for (MobRegistry.MobEntry entry : registry.poolFor(biome)) {
            (entry.isElite() ? elitePool : regularPool).add(entry);
        }

        int remainingSlots = totalCount;

        // Elite mobs (slotCost > 1, e.g. Golen) spawn first and each one eats several regular slots.
        if (!elitePool.isEmpty()) {
            int representativeSlotCost = elitePool.stream().mapToInt(e -> e.slotCost).max().orElse(1);
            int eliteBudget = calculateEliteSpawnCount(totalCount, representativeSlotCost);

            for (int i = 0; i < eliteBudget && remainingSlots > 0; i++) {
                MobRegistry.MobEntry chosen = registry.pickWeighted(biome, rng, elitePool);
                if (chosen == null) break;
                if (spawnOne(chosen, map, player, out, tileSize, completedMaps)) {
                    remainingSlots -= chosen.slotCost;
                }
            }
        }

        // Fill remaining slots with regular mobs.
        for (int i = 0; i < remainingSlots; i++) {
            MobRegistry.MobEntry chosen = registry.pickWeighted(biome, rng, regularPool);
            if (chosen == null) break;
            spawnOne(chosen, map, player, out, tileSize, completedMaps);
        }
    }

    /**
     * Finds a spawn tile, instantiates the mob, applies perks, and adds it to {@code out}.
     * Returns false (without adding anything) if no valid tile could be found.
     */
    private boolean spawnOne(MobRegistry.MobEntry entry, TileMap map, Player player, List<Entity> out,
                              int tileSize, int completedMaps) {
        int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
        if (pos == null) pos = map.getRandomFloorTile();
        if (pos == null || map.isWall(pos[0], pos[1])) return false;

        double x = pos[0] * tileSize + tileSize / 2.0;
        double y = pos[1] * tileSize + tileSize / 2.0;

        Entity enemy = entry.factory.create(x, y);
        if (entry.perkApplier != null) {
            entry.perkApplier.accept(enemy, completedMaps);
        } else {
            applyEnemyPerks(enemy, completedMaps);
        }
        out.add(enemy);
        return true;
    }

    /**
     * Applies biome-specific adjustments to enemy count.
     */
    private int applyBiomeAdjustments(int baseCount, Biome biome) {
        double multiplier = registry.getBiomeMultiplier(biome);
        return Math.max(1, (int) (baseCount * multiplier));
    }

    /**
     * Generalized version of the original "Golen replaces 10 regular enemies" rule: works for
     * any elite mob (slotCost > 1), sized by that mob's own slot cost. Reuses the same
     * thresholds Golen always used ({@link #getGolenSpawnThreshold()}, {@link #getMaxGolenPerMap()}),
     * so Golen spawn rates are unchanged by this generalization.
     */
    private int calculateEliteSpawnCount(int totalEnemyCount, int slotCost) {
        if (totalEnemyCount <= getGolenSpawnThreshold()) return 0;

        int maxByCount = Math.min(getMaxGolenPerMap(), totalEnemyCount / slotCost);

        int minEnemiesToKeep = 10;
        int maxForMinEnemies = (totalEnemyCount - minEnemiesToKeep) / slotCost;
        int maxCount = Math.min(maxByCount, Math.max(0, maxForMinEnemies));

        return rng.nextInt(maxCount + 1); // 0..maxCount
    }

    /**
     * Registers the game's built-in mobs and their default per-biome weights. This is the
     * template to copy when adding a new mob: pick an id, a slot cost (1 for a normal enemy,
     * >1 for an elite that should replace several regular spawns), a factory that builds the
     * entity (with its own weapon), and per-biome weights.
     */
    private void registerDefaultMobs() {
        registry.register("soldier", (x, y) -> new Soldier(x, y, new Sword(2, 28, 12, 10, 16)))
            .setWeight(Biome.MEADOWS, 8.0)
            .setWeight(Biome.FOREST, 4.0)
            .setWeight(Biome.CAVE, 6.0)
            .setWeight(Biome.DESERT, 5.0)
            .setWeight(Biome.VULCAN, 7.0);

        registry.register("archer", (x, y) -> new Archer(x, y, new Bow(1, 120, 5, 8, 90)))
            .setWeight(Biome.MEADOWS, 2.0)
            .setWeight(Biome.FOREST, 6.0)
            .setWeight(Biome.CAVE, 4.0)
            .setWeight(Biome.DESERT, 5.0)
            .setWeight(Biome.VULCAN, 3.0);

        // Golen is an elite: it replaces 10 regular spawn slots and always gets full perks,
        // regardless of map completion, so it applies its own perk routine instead of the
        // default scaling one.
        registry.register("golen", getGolenReplacementRatio(), this::applyGolenPerks,
                (x, y) -> new Golen(x, y, new Sword(2, 28, 12, 10, 16)));
        // Disabled by default in every biome (weight 0.0), matching the original behavior;
        // enable it per biome with registry.get("golen").setWeight(biome, weight).

        for (Biome biome : Biome.values()) {
            registry.setBiomeMultiplier(biome, DEFAULT_BIOME_ENEMY_MULTIPLIERS[biome.ordinal()]);
        }
    }
}
