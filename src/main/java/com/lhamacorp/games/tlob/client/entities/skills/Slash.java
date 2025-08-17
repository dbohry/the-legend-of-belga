package com.lhamacorp.games.tlob.client.entities.skills;

public class Slash extends Skill {

    public Slash(double baseDamage, int cooldownTicks, int durationTicks, double range) {
        super("Slash",
            "A powerful melee attack with the sword",
            baseDamage,
            cooldownTicks,
            durationTicks,
            range,
            false,
            "slash-hit.wav"
        );
    }

}