package com.lhamacorp.games.tlob.weapons;

public class Weapon {

    protected String name;
    protected WeaponType type;
    protected int reach;
    protected int width;
    protected int damage;
    protected int duration;
    protected int cooldown;

    public enum WeaponType {
        SWORD, AXE, BOW, DAGGER, MACE, SPEAR, WAND
    }

    public void setReach(int reach) {
        this.reach = reach;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getName() {
        return name;
    }

    public WeaponType getType() {
        return type;
    }

    public int getReach() {
        return reach;
    }

    public int getWidth() {
        return width;
    }

    public int getDamage() {
        return damage;
    }

    public int getDuration() {
        return duration;
    }

    public int getCooldown() {
        return cooldown;
    }

}
