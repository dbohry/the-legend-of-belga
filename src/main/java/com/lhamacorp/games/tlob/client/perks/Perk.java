package com.lhamacorp.games.tlob.client.perks;

import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.perks.PerkManager.Rarity;

import java.util.function.Consumer;

public class Perk {
    public final String name;
    public final String description;
    public final Rarity rarity;
    private final Consumer<Player> effect;

    public Perk(String name, String description, Rarity rarity, Consumer<Player> effect) {
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.effect = effect;
    }

    public void apply(Player player) {
        if (player != null && effect != null) {
            effect.accept(player);
        }
    }
}
