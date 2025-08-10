package com.lhamacorp.games.tlob;

import java.util.function.Consumer;

public class Perk {
    public final String name;
    public final String description;
    private final Consumer<Player> effect;

    public Perk(String name, String description, Consumer<Player> effect) {
        this.name = name;
        this.description = description;
        this.effect = effect;
    }

    public void apply(Player player) {
        if (player != null && effect != null) {
            effect.accept(player);
        }
    }
}
