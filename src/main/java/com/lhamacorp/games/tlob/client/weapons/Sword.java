package com.lhamacorp.games.tlob.client.weapons;

public class Sword extends Weapon {

    /**
     * Creates a sword with the specified properties.
     */
    public Sword(int damage, int reach, int width, int duration, int cooldown) {
        super.name = "Sword";
        super.type = WeaponType.SWORD;
        super.damage = damage;
        super.setReach(reach);
        super.setWidth(width);
        super.duration = duration;
        super.cooldown = cooldown;
    }

}
