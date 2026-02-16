package com.lhamacorp.games.tlob.client.inventory;

import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the player's inventory of weapons.
 * Currently supports a single weapon but designed for future multi-weapon support.
 */
public class Inventory {

    private final List<Weapon> weapons = new ArrayList<>();
    private int equippedIndex = 0;

    /**
     * Creates a new Inventory with an initial weapon.
     *
     * @param initialWeapon the player's starting weapon
     */
    public Inventory(Weapon initialWeapon) {
        if (initialWeapon != null) {
            weapons.add(initialWeapon);
        }
    }

    /**
     * Gets the currently equipped weapon.
     *
     * @return the current weapon, or null if no weapons available
     */
    public Weapon getCurrentWeapon() {
        if (weapons.isEmpty()) {
            return null;
        }
        return weapons.get(equippedIndex);
    }

    /**
     * Gets the total number of weapons in the inventory.
     *
     * @return the weapon count
     */
    public int getWeaponCount() {
        return weapons.size();
    }

    /**
     * Gets the index of the currently equipped weapon.
     *
     * @return the equipped weapon index
     */
    public int getEquippedIndex() {
        return equippedIndex;
    }

    /**
     * Adds a weapon to the inventory.
     * Future enhancement: support for multiple weapons.
     *
     * @param weapon the weapon to add
     */
    public void addWeapon(Weapon weapon) {
        if (weapon != null && !weapons.contains(weapon)) {
            weapons.add(weapon);
        }
    }

    /**
     * Removes a weapon from the inventory.
     * If the equipped weapon is removed, switches to the first available weapon.
     *
     * @param weapon the weapon to remove
     */
    public void removeWeapon(Weapon weapon) {
        if (weapons.remove(weapon)) {
            if (equippedIndex >= weapons.size() && !weapons.isEmpty()) {
                equippedIndex = 0;
            }
        }
    }

    /**
     * Switches to a different weapon by index.
     * Future enhancement: support for weapon switching mechanics.
     *
     * @param index the index of the weapon to equip
     * @return true if switch was successful, false otherwise
     */
    public boolean switchWeapon(int index) {
        if (index >= 0 && index < weapons.size()) {
            equippedIndex = index;
            return true;
        }
        return false;
    }

}
