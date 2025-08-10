package com.lhamacorp.games.tlob.perks;

import com.lhamacorp.games.tlob.entities.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PerkManager {

    public enum PerkType {SHIELD, MAX_HEALTH, MAX_STAMINA, MAX_MANA, MOVE_SPEED, DAMAGE, RANGE}

    private final List<Perk> currentChoices = new ArrayList<>(3);

    /**
     * Roll 3 distinct perk choices and store them.
     */
    public void rollChoices() {
        currentChoices.clear();

        List<PerkType> types = new ArrayList<>(Arrays.asList(PerkType.values()));
        Collections.shuffle(types);

        for (int i = 0; i < 3; i++) {
            currentChoices.add(generate(types.get(i)));
        }
        Collections.shuffle(currentChoices);
        getChoices();
    }

    /**
     * Read-only view of current choices.
     */
    public List<Perk> getChoices() {
        return Collections.unmodifiableList(currentChoices);
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

    public void clearChoices() {
        currentChoices.clear();
    }

    private Perk generate(PerkType type) {
        switch (type) {
            case MAX_HEALTH -> {
                double p = 0.10 + Math.random() * 0.10; // +10..20%
                String label = String.format("Max Life +%d%%", (int) Math.round(p * 100));
                String desc = "Increases maximum life permanently.";
                return new Perk(label, desc, pl -> pl.increaseMaxHealthByPercent(p));
            }
            case MAX_STAMINA -> {
                double p = 0.10 + Math.random() * 0.10;
                String label = String.format("Max Stamina +%d%%", (int) Math.round(p * 100));
                String desc = "Increases maximum stamina permanently.";
                return new Perk(label, desc, pl -> pl.increaseMaxStaminaByPercent(p));
            }
            case MAX_MANA -> {
                double p = 0.10 + Math.random() * 0.10;
                String label = String.format("Max Mana +%d%%", (int) Math.round(p * 100));
                String desc = "Increases maximum mana permanently.";
                return new Perk(label, desc, pl -> pl.increaseMaxManaByPercent(p));
            }
            case MOVE_SPEED -> {
                double p = 0.05 + Math.random() * 0.05; // +5..10%
                String label = String.format("Move Speed +%d%%", (int) Math.round(p * 100));
                String desc = "Increases movement speed permanently.";
                return new Perk(label, desc, pl -> pl.increaseMoveSpeedByPercent(p));
            }
            case DAMAGE -> {
                double p = 0.10 + Math.random() * 0.10;
                String label = String.format("Damage +%d%%", (int) Math.round(p * 100));
                String desc = "Increases melee damage permanently.";
                return new Perk(label, desc, pl -> pl.increaseAttackDamageByPercent(p));
            }
            case RANGE -> {
                double p = 0.05 + Math.random() * 0.05;
                String label = String.format("Weapon range +%d%%", (int) Math.round(p * 100));
                String desc = "Increased weapon range permanently.";
                return new Perk(label, desc, pl -> pl.increaseWeaponRangeByPercent(p));
            }
            case SHIELD -> {
                String label = "+1 Shield";
                String desc = "Adds +1 to HP shield";
                return new Perk(label, desc, Player::increaseShield);
            }
            default -> throw new IllegalStateException("Unhandled perk type: " + type);
        }
    }
}
