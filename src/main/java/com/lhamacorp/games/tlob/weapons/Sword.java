package com.lhamacorp.games.tlob.weapons;

public class Sword extends Weapon {

    public Sword(int damage, int reach, int width, int duration, int cooldown) {
        super.damage = damage;
        super.reach = reach;
        super.width = width;
        super.duration = duration;
        super.cooldown = cooldown;
    }

    public void increaseDamage(int increasedDamage) {
        damage += damage + increasedDamage;
    }

    public void increaseRange(int increasedRange) {
        reach += reach + increasedRange;
    }

}
