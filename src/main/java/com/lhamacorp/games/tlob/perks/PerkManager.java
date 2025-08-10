package com.lhamacorp.games.tlob.perks;

import com.lhamacorp.games.tlob.entities.Player;

import java.util.*;
import java.util.function.Predicate;

/**
 * Flexible, registry-based perk manager.
 * Add new perks via register("ID", PerkEntry.of(weight, eligibility, rng -> new Perk(...))).
 */
public class PerkManager {

    /**
     * Factory that builds a Perk; use rng for randomized values.
     */
    @FunctionalInterface
    public interface PerkFactory {
        Perk create(Random rng);
    }

    /**
     * Registry entry with weight (for rarity) and optional player-based eligibility.
     */
    public static final class PerkEntry {
        public final int weight;
        public final Predicate<Player> eligibility;
        public final PerkFactory factory;

        private PerkEntry(int weight, Predicate<Player> eligibility, PerkFactory factory) {
            if (weight <= 0) throw new IllegalArgumentException("weight must be > 0");
            this.weight = weight;
            this.eligibility = (eligibility != null) ? eligibility : p -> true;
            this.factory = Objects.requireNonNull(factory);
        }

        public static PerkEntry of(int weight, Predicate<Player> eligibility, PerkFactory factory) {
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

    /* -------------------- Public API -------------------- */

    /**
     * Register/replace a perk definition under an id.
     */
    public void register(String id, PerkEntry entry) {
        registry.put(Objects.requireNonNull(id), Objects.requireNonNull(entry));
    }

    /**
     * Remove a perk by id.
     */
    public void unregister(String id) {
        registry.remove(id);
    }

    /**
     * Return an immutable view of rolled choices.
     */
    public List<Perk> getChoices() {
        return Collections.unmodifiableList(currentChoices);
    }

    /**
     * Clear rolled choices.
     */
    public void clearChoices() {
        currentChoices.clear();
    }

    /**
     * Backward compatible roll (no eligibility).
     */
    public void rollChoices() {
        rollChoicesFor(null);
    }

    /**
     * Roll 3 distinct perk choices, honoring eligibility if a player is provided.
     * Weighted without replacement.
     */
    public void rollChoicesFor(Player player) {
        currentChoices.clear();
        if (registry.isEmpty()) return;

        // Build a pool filtered by eligibility
        List<Map.Entry<String, PerkEntry>> pool = new ArrayList<>();
        for (Map.Entry<String, PerkEntry> e : registry.entrySet()) {
            if (player == null || e.getValue().eligibility.test(player)) {
                pool.add(e);
            }
        }
        if (pool.isEmpty()) return;

        // Weighted picks without replacement (up to 3)
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

            // remove to avoid duplicates
            pool.remove(chosenIdx);
        }

        // Optional: shuffle final order for variety
        Collections.shuffle(currentChoices, rng);
    }

    /**
     * Apply the chosen perk to the player. Returns the perk applied (or null if bad index).
     */
    public Perk applyChoice(int index, Player player) {
        if (index < 0 || index >= currentChoices.size()) return null;
        Perk chosen = currentChoices.get(index);
        chosen.apply(player);
        return chosen;
    }

    private void registerDefaults() {
        // You can tweak weights to reflect rarity (higher = more common)
        register("MAX_HEALTH", PerkEntry.of(10, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = String.format("Max Life +%d%%", (int) Math.round(p * 100));
            String desc = "Increases maximum life permanently.";
            return new Perk(label, desc, pl -> pl.increaseMaxHealthByPercent(p));
        }));

        register("MAX_STAMINA", PerkEntry.of(10, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = String.format("Max Stamina +%d%%", (int) Math.round(p * 100));
            String desc = "Increases maximum stamina permanently.";
            return new Perk(label, desc, pl -> pl.increaseMaxStaminaByPercent(p));
        }));

        register("MAX_MANA", PerkEntry.of(8, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = String.format("Max Mana +%d%%", (int) Math.round(p * 100));
            String desc = "Increases maximum mana permanently.";
            return new Perk(label, desc, pl -> pl.increaseMaxManaByPercent(p));
        }));

        register("MOVE_SPEED", PerkEntry.of(8, r -> {
            double p = pct(r, 0.05, 0.10);
            String label = String.format("Move Speed +%d%%", (int) Math.round(p * 100));
            String desc = "Increases movement speed permanently.";
            return new Perk(label, desc, pl -> pl.increaseMoveSpeedByPercent(p));
        }));

        register("WEAPON_DAMAGE", PerkEntry.of(9, r -> {
            double p = pct(r, 0.10, 0.20);
            String label = String.format("Damage +%d%%", (int) Math.round(p * 100));
            String desc = "Increases melee damage permanently.";
            return new Perk(label, desc, pl -> pl.increaseAttackDamageByPercent(p));
        }));

        register("WEAPON_RANGE", PerkEntry.of(6, r -> {
            double p = pct(r, 0.05, 0.10);
            String label = String.format("Weapon Range +%d%%", (int) Math.round(p * 100));
            String desc = "Increased weapon range permanently.";
            return new Perk(label, desc, pl -> pl.increaseWeaponRangeByPercent(p));
        }));

        register("WEAPON_WIDTH", PerkEntry.of(6, r -> {
            String label = "+1 Weapon Width";
            String desc = "Enlarge your weapon by 1";
            return new Perk(label, desc, Player::increaseWeaponWidth);
        }));

        register("SHIELD", PerkEntry.of(7, r ->
            new Perk("+1 Shield", "Adds +1 to HP shield", Player::increaseShield)
        ));
    }

    private static double pct(Random r, double min, double max) {
        return min + r.nextDouble() * (max - min);
    }
}
