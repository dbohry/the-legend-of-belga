package com.lhamacorp.games.tlob;

import java.util.function.Consumer;

public class Perk {

    public final String name;
    public final String description;
    private final Consumer<Player> applier;

    public Perk(String name, String description, Consumer<Player> applier) {
        this.name = name;
        this.description = description;
        this.applier = applier;
    }

    public void apply(Player p) {
        applier.accept(p);
    }
}
