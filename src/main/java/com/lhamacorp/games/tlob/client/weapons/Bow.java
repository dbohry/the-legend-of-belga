package com.lhamacorp.games.tlob.client.weapons;

public class Bow extends Weapon {

    /**
     * Creates a bow with the specified properties.
     */
    public Bow(int damage, int reach, int width, int duration, int cooldown) {
        super.name = "Bow";
        super.type = WeaponType.BOW;
        super.damage = damage;
        super.setReach(reach);
        super.setWidth(width);
        super.duration = duration;
        super.cooldown = cooldown;
    }

}
