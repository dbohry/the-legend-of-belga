package com.lhamacorp.games.tlob.client.weapons;

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

    /**
     * Sets the weapon's reach.
     */
    public void setReach(int reach) {
        this.reach = reach;
    }

    /**
     * Sets the weapon's damage.
     */
    public void setDamage(int damage) {
        this.damage = damage;
    }

    /**
     * Sets the weapon's width.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the weapon's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the weapon's type.
     */
    public WeaponType getType() {
        return type;
    }

    /**
     * Gets the weapon's reach.
     */
    public int getReach() {
        return reach;
    }

    /**
     * Gets the weapon's width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the weapon's damage.
     */
    public int getDamage() {
        return damage;
    }

    /**
     * Gets the weapon's duration.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the weapon's cooldown.
     */
    public int getCooldown() {
        return cooldown;
    }

}
