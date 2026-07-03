package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.maps.Biome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Data-driven registry of spawnable mob (enemy) types, mirroring the pattern used by
 * {@link com.lhamacorp.games.tlob.client.perks.PerkManager} for perks.
 * <p>
 * Adding a new mob to the game is a single {@link #register} call: no enum values,
 * no parallel weight arrays, and no if/else chains to edit. Each {@link MobEntry} owns
 * its own per-biome spawn weight, how many "regular" spawn slots it consumes
 * (its {@code slotCost} - e.g. an elite unit that should be rarer and replace several
 * weaker enemies), a factory that builds the {@link Entity} (including its weapon), and
 * an optional custom perk-application routine for mobs that should be buffed differently
 * than the default (e.g. always-elite mobs).
 */
public final class MobRegistry {

    /**
     * Builds an {@link Entity} instance for a mob at the given position. The factory closure
     * owns its own weapon construction (e.g. a Soldier registration builds its own Sword) so
     * each mob's stats live in exactly one place instead of being re-created ad hoc by callers.
     */
    @FunctionalInterface
    public interface MobFactory {
        Entity create(double x, double y);
    }

    /** A single registered mob definition. */
    public static final class MobEntry {
        public final String id;
        public final MobFactory factory;
        public final int slotCost;
        public final BiConsumer<Entity, Integer> perkApplier; // may be null (use caller's default)

        private final Map<Biome, Double> biomeWeights = new EnumMap<>(Biome.class);

        private MobEntry(String id, MobFactory factory, int slotCost, BiConsumer<Entity, Integer> perkApplier) {
            this.id = Objects.requireNonNull(id);
            this.factory = Objects.requireNonNull(factory);
            this.slotCost = Math.max(1, slotCost);
            this.perkApplier = perkApplier;
        }

        /** Weight for this mob in the given biome (0 = never spawns there). */
        public double getWeight(Biome biome) {
            return biomeWeights.getOrDefault(biome, 0.0);
        }

        public MobEntry setWeight(Biome biome, double weight) {
            biomeWeights.put(biome, Math.max(0.0, weight));
            return this;
        }

        /** Convenience: set the same weight across every biome, then override individual ones. */
        public MobEntry setDefaultWeight(double weight) {
            for (Biome b : Biome.values()) setWeight(b, weight);
            return this;
        }

        public boolean isElite() {
            return slotCost > 1;
        }
    }

    private final Map<String, MobEntry> mobs = new LinkedHashMap<>();
    private final Map<Biome, Double> biomeMultipliers = new EnumMap<>(Biome.class);

    public MobRegistry() {
        for (Biome b : Biome.values()) biomeMultipliers.put(b, 1.0);
    }

    /* -------------------- Registration API -------------------- */

    /** Register a regular (slotCost = 1) mob with no custom perk handling. */
    public MobEntry register(String id, MobFactory factory) {
        return register(id, 1, null, factory);
    }

    /** Register a mob that consumes {@code slotCost} regular spawn slots (elite units use > 1). */
    public MobEntry register(String id, int slotCost, MobFactory factory) {
        return register(id, slotCost, null, factory);
    }

    /** Full registration: custom slot cost and optional custom perk-application routine. */
    public MobEntry register(String id, int slotCost, BiConsumer<Entity, Integer> perkApplier, MobFactory factory) {
        MobEntry entry = new MobEntry(id, factory, slotCost, perkApplier);
        mobs.put(id, entry);
        return entry;
    }

    public void unregister(String id) {
        mobs.remove(id);
    }

    public MobEntry get(String id) {
        return mobs.get(id);
    }

    public Collection<MobEntry> all() {
        return mobs.values();
    }

    /* -------------------- Biome-wide multipliers -------------------- */

    public void setBiomeMultiplier(Biome biome, double multiplier) {
        biomeMultipliers.put(biome, Math.max(0.1, multiplier));
    }

    public double getBiomeMultiplier(Biome biome) {
        return biomeMultipliers.getOrDefault(biome, 1.0);
    }

    /* -------------------- Selection -------------------- */

    /** All registered mobs with a positive weight in the given biome. */
    public List<MobEntry> poolFor(Biome biome) {
        List<MobEntry> pool = new ArrayList<>();
        for (MobEntry e : mobs.values()) {
            if (e.getWeight(biome) > 0) pool.add(e);
        }
        return pool;
    }

    /**
     * Weighted-random pick from an arbitrary pool of entries (use {@link #poolFor} to build one,
     * optionally filtered further, e.g. by {@link MobEntry#isElite()}).
     * Returns {@code null} if the pool is empty or all weights are zero for this biome.
     */
    public MobEntry pickWeighted(Biome biome, Random rng, Collection<MobEntry> pool) {
        double total = 0.0;
        for (MobEntry e : pool) total += e.getWeight(biome);
        if (total <= 0.0) return null;

        double target = rng.nextDouble() * total;
        double cumulative = 0.0;
        for (MobEntry e : pool) {
            cumulative += e.getWeight(biome);
            if (target < cumulative) return e;
        }
        return null; // unreachable in practice
    }
}
