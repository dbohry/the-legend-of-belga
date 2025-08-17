package com.lhamacorp.games.tlob.client.perks;

import com.lhamacorp.games.tlob.client.entities.Entity;

import java.util.*;
import java.util.function.Predicate;

/**
 * Flexible, registry-based perk manager with rarity and eligibility.
 * Add new perks via:
 *   register("ID", Rarity.RARE, rng -> new Perk(...));
 *   register("ID", Rarity.UNCOMMON, entity -> entity.hasMana(), rng -> new Perk(...));
 */
public class PerkManager {

    /** Factory that builds a Perk; use rng for randomized values. */
    @FunctionalInterface
    public interface PerkFactory {
        Perk create(Random rng);
    }

    /** Rarity presets mapped to weights (higher weight => more common). */
    public enum Rarity {
        COMMON(15), UNCOMMON(8), RARE(4), EPIC(2), LEGENDARY(1);
        public final int weight;

        Rarity(int w) {
            this.weight = w;
        }
    }

    /** Registry entry with weight (for rarity) and optional entity-based eligibility. */
    public static final class PerkEntry {
        public final int weight;
        public final Predicate<Entity> eligibility;
        public final PerkFactory factory;

        private PerkEntry(int weight, Predicate<Entity> eligibility, PerkFactory factory) {
            if (weight <= 0) throw new IllegalArgumentException("weight must be > 0");
            this.weight = weight;
            this.eligibility = (eligibility != null) ? eligibility : e -> true;
            this.factory = Objects.requireNonNull(factory);
        }

        public static PerkEntry of(int weight, Predicate<Entity> eligibility, PerkFactory factory) {
            return new PerkEntry(weight, eligibility, factory);
        }

        public static PerkEntry of(int weight, PerkFactory factory) {
            return new PerkEntry(weight, null, factory);
        }
    }

    private final Map<String, PerkEntry> registry = new LinkedHashMap<>();
    private final List<Perk> currentChoices = new ArrayList<>(3);
    private final Random rng;

    public PerkManager() {
        this(new Random());
        registerDefaults();
    }

    public PerkManager(Random rng) {
        this.rng = (rng != null) ? rng : new Random();
    }

    /* -------------------- Registration API -------------------- */

    /** Register/replace a perk definition under an id (raw weight version). */
    public void register(String id, PerkEntry entry) {
        registry.put(Objects.requireNonNull(id), Objects.requireNonNull(entry));
    }

    /** Register with rarity (no eligibility). */
    public void register(String id, Rarity rarity, PerkFactory factory) {
        register(id, PerkEntry.of(rarity.weight, factory));
    }

    /** Register with rarity and eligibility. */
    public void register(String id, Rarity rarity, Predicate<Entity> eligibility, PerkFactory factory) {
        register(id, PerkEntry.of(rarity.weight, eligibility, factory));
    }

    /** Remove a perk by id. */
    public void unregister(String id) {
        registry.remove(id);
    }

    /** Change a perk's weight (rarity) at runtime. Returns true if updated. */
    public boolean setWeight(String id, int newWeight) {
        PerkEntry e = registry.get(id);
        if (e == null || newWeight <= 0) return false;
        registry.put(id, PerkEntry.of(newWeight, e.eligibility, e.factory));
        return true;
    }

    /* -------------------- Rolling & Applying -------------------- */

    /** Immutable view of rolled choices. */
    public List<Perk> getChoices() {
        return Collections.unmodifiableList(currentChoices);
    }

    /** Clear rolled choices. */
    public void clearChoices() {
        currentChoices.clear();
    }

    /** Backward-compatible roll (no eligibility). Prefer rollChoicesFor(entity). */
    public void rollChoices() {
        rollChoicesFor(null);
    }

    /**
     * Roll up to 3 distinct perk choices, honoring eligibility if an entity is provided.
     * Weighted sampling without replacement based on entry.weight.
     */
    public void rollChoicesFor(Entity entity) {
        currentChoices.clear();
        if (registry.isEmpty()) return;

        // Build a pool filtered by eligibility
        List<Map.Entry<String, PerkEntry>> pool = new ArrayList<>();
        for (Map.Entry<String, PerkEntry> e : registry.entrySet()) {
            if (entity == null || e.getValue().eligibility.test(entity)) {
                pool.add(e);
            }
        }
        if (pool.isEmpty()) return;

        int picks = Math.min(3, pool.size());
        for (int i = 0; i < picks; i++) {
            int totalWeight = 0;
            for (var e : pool) totalWeight += e.getValue().weight;

            int target = rng.nextInt(totalWeight) + 1; // 1..totalWeight
            int cumulative = 0;
            int chosenIdx = -1;

            for (int idx = 0; idx < pool.size(); idx++) {
                cumulative += pool.get(idx).getValue().weight;
                if (cumulative >= target) {
                    chosenIdx = idx;
                    break;
                }
            }
            if (chosenIdx < 0) chosenIdx = pool.size() - 1;

            PerkEntry chosen = pool.get(chosenIdx).getValue();
            currentChoices.add(chosen.factory.create(rng));
            pool.remove(chosenIdx); // avoid duplicates
        }

        Collections.shuffle(currentChoices, rng);
    }

    /** Apply the chosen perk to the entity. Returns the perk applied (or null if bad index). */
    public Perk applyChoice(int index, Entity entity) {
        if (index < 0 || index >= currentChoices.size()) return null;
        Perk chosen = currentChoices.get(index);
        chosen.apply(entity);
        return chosen;
    }

    /* -------------------- Defaults -------------------- */

    private void registerDefaults() {
        register("MAX_HEALTH", Rarity.COMMON, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = "Max Life";
            String desc = String.format("Increases maximum life (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.COMMON, entity -> entity.increaseMaxHealthByPercent(p));
        });

        register("MAX_STAMINA", Rarity.COMMON, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = "Max Stamina";
            String desc = String.format("Increases maximum stamina (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.COMMON, entity -> entity.increaseMaxStaminaByPercent(p));
        });

        register("MAX_MANA", Rarity.COMMON, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = "Max Mana";
            String desc = String.format("Increases maximum mana (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.COMMON, entity -> entity.increaseMaxManaByPercent(p));
        });

        register("MOVE_SPEED", Rarity.UNCOMMON, r -> {
            double p = pct(r, 0.05, 0.10);
            String label = "Movement Speed";
            String desc = String.format("Increases movement speed (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.UNCOMMON, entity -> entity.increaseMoveSpeedByPercent(p));
        });

        register("STAMINA_REGEN", Rarity.UNCOMMON, r -> {
            double p = pct(r, 0.10, 0.20); // 5%..10%
            String label = "Stamina Regen";
            String desc = String.format("Stamina regenerates faster (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.UNCOMMON, entity -> entity.increaseStaminaRegenByPercent(p));
        });

        register("MANA_REGEN", Rarity.UNCOMMON, r -> {
            double p = pct(r, 0.05, 0.10); // 5%..10%
            String label = "Mana Regen";
            String desc = String.format("Mana regenerates faster (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.UNCOMMON, entity -> entity.increaseManaRegenByPercent(p));
        });

        register("ATTACK_DAMAGE", Rarity.UNCOMMON, r -> {
            double p = pct(r, 0.20, 0.30);
            String label = "Attack Damage";
            String desc = String.format("Increases attack damage (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.UNCOMMON, entity -> entity.increaseAttackDamageByPercent(p));
        });

        register("WEAPON_RANGE", Rarity.RARE, r -> {
            double p = pct(r, 0.15, 0.25);
            String label = "Weapon Range";
            String desc = String.format("Increases weapon range (+%d%%).", (int) Math.round(p * 100));
            return new Perk(label, desc, Rarity.RARE, entity -> entity.increaseWeaponRangeByPercent(p));
        });

        register("ARMOR", Rarity.RARE, r -> {
            String label = "Max Armor";
            String desc = "Increases max armor (+1)";
            return new Perk(label, desc, Rarity.RARE, entity -> entity.increaseArmor(1.0));
        });

        register("WEAPON_WIDTH", Rarity.EPIC, r -> {
            String label = "Weapon Width";
            String desc = "Increases weapon width (+1).";
            return new Perk(label, desc, Rarity.EPIC, entity -> entity.increaseWeaponWidth(1));
        });

        register("SHIELD_BOOST", Rarity.EPIC, r -> {
            String label = "Energy Shield";
            String desc = "Adds energy shield (+1.0).";
            return new Perk(label, desc, Rarity.EPIC, entity -> entity.increaseMaxShield(1.0));
        });

    }

    private static double pct(Random r, double min, double max) {
        return min + r.nextDouble() * (max - min);
    }
}
