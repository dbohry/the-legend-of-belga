package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.BaseGameManager;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.awt.*;

public abstract class Entity {

    protected double x;
    protected double y;
    protected int width;
    protected int height;
    protected double speed;

    protected double maxShield;
    protected double shield;

    protected double maxMana;
    protected double mana;

    protected double maxStamina;
    protected double stamina;

    protected double maxHealth;
    protected double health;
    protected boolean alive = true;
    protected Weapon weapon;
    protected String name;
    protected Alignment alignment;

    // Perk system fields
    protected double damageMultiplier = 1.0;
    protected double speedMultiplier = 1.0;
    protected double staminaRegenRateMult = 1.0;
    protected double manaRegenRateMult = 1.0;

    // Armor system
    protected double armor;

    // Perk tracking for visual indicators
    protected int perkCount = 0;
    protected boolean hasHealthPerk = false;
    protected boolean hasSpeedPerk = false;
    protected boolean hasDamagePerk = false;
    protected boolean hasStaminaPerk = false;
    protected boolean hasRangePerk = false;
    protected boolean hasWidthPerk = false;
    protected boolean hasArmorPerk = false;

    protected Direction facing = Direction.DOWN;

    // Knockback system
    protected double knockbackX = 0;
    protected double knockbackY = 0;
    protected int knockbackTimer = 0;
    protected static final int KNOCKBACK_DURATION = 8;
    protected static final double KNOCKBACK_FORCE = 8.0;

    public enum Direction {UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT}

    /**
     * Creates an entity with default name and neutral alignment.
     */
    public Entity(double x, double y, int width, int height, double speed, double maxHealth, double maxStamina, double maxMana, double maxShield, double maxArmor, Weapon weapon) {
        this(x, y, width, height, speed, maxHealth, maxStamina, maxMana, maxShield, maxArmor, weapon, "Entity", Alignment.NEUTRAL);
    }

    /**
     * Creates an entity with specified parameters.
     */
    public Entity(double x, double y, int width, int height, double speed, double maxHealth, double maxStamina, double maxMana, double maxShield, double maxArmor, Weapon weapon, String name, Alignment alignment) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
        this.maxMana = maxMana;
        this.mana = maxMana;
        this.weapon = weapon;
        this.maxShield = maxShield;
        this.shield = maxShield;
        this.armor = maxArmor;
        this.name = name;
        this.alignment = alignment;
    }

    /**
     * Sets the entity's name.
     */
    public void setName(String n) {
        this.name = n;
    }

    /**
     * Gets the entity's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if the entity is alive.
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Gets the entity's X coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the entity's Y coordinate.
     */
    public double getY() {
        return y;
    }

    // ===== Perk System Methods =====

    /**
     * Increases maximum health by the specified percentage.
     */
    public void increaseMaxHealthByPercent(double pct) {
        this.maxHealth = Math.ceil(getMaxHealth() * (1.0 + pct));
        // Ensure current health doesn't exceed new max
        if (this.health > this.maxHealth) {
            this.health = this.maxHealth;
        }
        // Track perk application
        if (!hasHealthPerk) {
            hasHealthPerk = true;
            perkCount++;
        }
    }

    /**
     * Increases maximum stamina by the specified percentage.
     */
    public void increaseMaxStaminaByPercent(double pct) {
        this.maxStamina = Math.ceil(getMaxStamina() * (1.0 + pct));
        if (this.stamina > this.maxStamina) {
            this.stamina = this.maxStamina;
        }
        if (!hasStaminaPerk) {
            hasStaminaPerk = true;
            perkCount++;
        }
    }

    /**
     * Increases maximum mana by the specified percentage.
     */
    public void increaseMaxManaByPercent(double pct) {
        if (getMaxMana() == 0) this.maxMana = 1.0;
        this.maxMana = Math.ceil(getMaxMana() * (1.0 + pct));
        // Ensure current mana doesn't exceed new max
        if (this.mana > this.maxMana) {
            this.mana = this.maxMana;
        }
        perkCount++;
    }

    /**
     * Increases maximum shield by the specified amount.
     */
    public void increaseMaxShield(double amount) {
        this.maxShield += amount;
        if (this.shield > this.maxShield) {
            this.shield = this.maxShield;
        }
        perkCount++;
    }

    /**
     * Increases stamina regeneration rate by the specified percentage.
     */
    public void increaseStaminaRegenByPercent(double pct) {
        staminaRegenRateMult *= (1.0 + pct);
        if (!hasStaminaPerk) {
            hasStaminaPerk = true;
            perkCount++;
        }
    }

    /**
     * Increases mana regeneration rate by the specified percentage.
     */
    public void increaseManaRegenByPercent(double pct) {
        manaRegenRateMult *= (1.0 + pct);
        perkCount++;
    }

    /**
     * Increases movement speed by the specified percentage.
     */
    public void increaseMoveSpeedByPercent(double pct) {
        speedMultiplier *= (1.0 + pct);
        // Track perk application
        if (!hasSpeedPerk) {
            hasSpeedPerk = true;
            perkCount++;
        }
    }

    /**
     * Increases attack damage by the specified percentage.
     */
    public void increaseAttackDamageByPercent(double pct) {
        damageMultiplier *= (1.0 + pct);
        if (!hasDamagePerk) {
            hasDamagePerk = true;
            perkCount++;
        }
    }

    /**
     * Increases weapon range by the specified percentage.
     */
    public void increaseWeaponRangeByPercent(double pct) {
        if (weapon != null) {
            weapon.setReach((int) Math.ceil(weapon.getReach() * (1.0 + pct)));
            if (!hasRangePerk) {
                hasRangePerk = true;
                perkCount++;
            }
        }
    }

    /**
     * Increases weapon width by the specified amount.
     */
    public void increaseWeaponWidth(int amount) {
        if (weapon != null) {
            weapon.setWidth(weapon.getWidth() + amount);
            if (!hasWidthPerk) {
                hasWidthPerk = true;
                perkCount++;
            }
        }
    }

    /**
     * Increases armor by the specified amount.
     */
    public void increaseArmor(double amount) {
        armor += amount;
        if (!hasArmorPerk) {
            hasArmorPerk = true;
            perkCount++;
        }
    }

    /**
     * Gets the current damage multiplier from perks.
     */
    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Gets the current speed multiplier from perks.
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Gets the current stamina regeneration rate multiplier from perks.
     */
    public double getStaminaRegenRateMult() {
        return staminaRegenRateMult;
    }

    /**
     * Gets the current mana regeneration rate multiplier from perks.
     */
    public double getManaRegenRateMult() {
        return manaRegenRateMult;
    }

    /**
     * Gets the current armor value.
     */
    public double getArmor() {
        return armor * 10;
    }

    /**
     * Gets the effective armor value (capped at 10).
     */
    public double getEffectiveArmorValue() {
        return Math.min(armor, 10.0);
    }

    /**
     * Gets the effective speed considering perks.
     */
    public double getEffectiveSpeed() {
        return speed * speedMultiplier;
    }

    /**
     * Gets the effective attack damage considering perks.
     */
    public double getEffectiveAttackDamage() {
        if (weapon != null) {
            return weapon.getDamage() * damageMultiplier;
        }
        return 0.0;
    }

    // ===== Perk Information Getters =====

    /**
     * Gets a summary of all perks applied to this entity.
     */
    public String getPerkSummary() {
        if (perkCount == 0) return "No perks";

        StringBuilder summary = new StringBuilder();
        if (hasHealthPerk) summary.append("HP+ ");
        if (hasSpeedPerk) summary.append("SPD+ ");
        if (hasDamagePerk) summary.append("DMG+ ");
        if (hasStaminaPerk) summary.append("STM+ ");
        if (hasRangePerk) summary.append("RNG+ ");
        if (hasWidthPerk) summary.append("WID+ ");
        if (hasArmorPerk) summary.append("ARM+ ");

        return summary.toString().trim();
    }

    /**
     * Gets the color for perk indicators based on perk count.
     * More perks = more dangerous color.
     */
    public java.awt.Color getPerkIndicatorColor() {
        if (perkCount == 0) return java.awt.Color.WHITE;
        if (perkCount == 1) return java.awt.Color.YELLOW;
        if (perkCount == 2) return java.awt.Color.ORANGE;
        return java.awt.Color.RED; // 3+ perks = very dangerous
    }

    /**
     * Gets the size multiplier for perk indicators.
     * More perks = larger indicator.
     */
    public float getPerkIndicatorSize() {
        if (perkCount == 0) return 1.0f;
        if (perkCount == 1) return 1.2f;
        if (perkCount == 2) return 1.4f;
        return 1.6f; // 3+ perks = very large indicator
    }

    /**
     * Gets the entity's current shield value.
     */
    public double getShield() {
        return shield;
    }

    /**
     * Gets the entity's maximum shield value.
     */
    public double getMaxShield() {
        return maxShield;
    }

    /**
     * Gets the entity's current mana value.
     */
    public double getMana() {
        return mana;
    }

    /**
     * Gets the entity's maximum mana value.
     */
    public double getMaxMana() {
        return maxMana;
    }

    /**
     * Gets the entity's current stamina value.
     */
    public double getStamina() {
        return stamina;
    }

    /**
     * Gets the entity's maximum stamina value.
     */
    public double getMaxStamina() {
        return maxStamina;
    }

    /**
     * Gets the entity's current health value.
     */
    public double getHealth() {
        return health;
    }

    /**
     * Gets the entity's maximum health value.
     */
    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Gets the entity's width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the entity's height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the entity's movement speed.
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the entity's weapon.
     */
    public Weapon getWeapon() {
        return weapon;
    }

    /**
     * Gets the entity's bounds at the specified coordinates.
     */
    public Rectangle getBoundsAt(double cx, double cy) {
        int halfW = width / 2;
        int halfH = height / 2;
        return new Rectangle((int) Math.round(cx - halfW), (int) Math.round(cy - halfH), width, height);
    }

    /**
     * Gets the entity's bounds at its current position.
     */
    public Rectangle getBounds() {
        return getBoundsAt(x, y);
    }

    /**
     * Applies damage to the entity, applying armor reduction first, then absorbing with shield.
     */
    public void damage(double amount) {
        if (!alive) return;

        if (armor > 0) {
            double cappedArmor = Math.min(armor, 10); // Cap at 10 for 100%
            double chance = cappedArmor * 0.1;  // Each point = 10%
            if (Math.random() < chance) {
                amount *= 0.2; // 80% damage reduction
            }
        }

        if (shield > 0) {
            double absorbed = Math.min(shield, amount);
            shield -= absorbed;
            amount -= absorbed;
        }

        if (amount > 0) {
            health -= amount;
        }

        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    /**
     * Applies knockback force from the specified source position.
     */
    public void applyKnockback(double fromX, double fromY) {
        double dx = x - fromX;
        double dy = y - fromY;
        double distance = Math.hypot(dx, dy);

        if (distance > 0) {
            knockbackX = (dx / distance) * KNOCKBACK_FORCE;
            knockbackY = (dy / distance) * KNOCKBACK_FORCE;
            knockbackTimer = KNOCKBACK_DURATION;
        }
    }

    protected void updateKnockbackWithMap(TileMap map) {
        if (knockbackTimer > 0) {
            knockbackTimer--;

            double newX = x + knockbackX;
            double newY = y + knockbackY;

            if (!collidesWithMap(newX, newY, map)) {
                x = newX;
                y = newY;
            } else {
                knockbackTimer = 0;
                knockbackX = 0;
                knockbackY = 0;
            }

            knockbackX *= 0.9;
            knockbackY *= 0.9;

            if (knockbackTimer == 0) {
                knockbackX = 0;
                knockbackY = 0;
            }
        }
    }

    protected boolean collidesWithMap(double cx, double cy, TileMap map) {
        Rectangle bounds = getBoundsAt(cx, cy);

        if (bounds.x < 0 || bounds.y < 0 ||
            bounds.x + bounds.width > map.getWidth() * BaseGameManager.TILE_SIZE ||
            bounds.y + bounds.height > map.getHeight() * BaseGameManager.TILE_SIZE) {
            return true;
        }

        int left = bounds.x / BaseGameManager.TILE_SIZE;
        int right = (bounds.x + bounds.width - 1) / BaseGameManager.TILE_SIZE;
        int top = bounds.y / BaseGameManager.TILE_SIZE;
        int bottom = (bounds.y + bounds.height - 1) / BaseGameManager.TILE_SIZE;

        for (int ty = top; ty <= bottom; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (map.isWall(tx, ty)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean collidesWithPlayer(double cx, double cy, Player player) {
        if (player == null || !player.isAlive()) return false;

        Rectangle entityBounds = getBoundsAt(cx, cy);
        Rectangle playerBounds = player.getBounds();

        return entityBounds.intersects(playerBounds);
    }

    protected void applyKnockbackMovement() {
        x += knockbackX;
        y += knockbackY;
    }

    /**
     * Updates the entity's state.
     */
    public abstract void update(Object... args);

    /**
     * Draws the entity on the graphics context.
     */
    public abstract void draw(Graphics2D g2, int camX, int camY);

    protected void drawCenteredRect(Graphics2D g2, int camX, int camY, int w, int h, Color color) {
        g2.setColor(color);
        int drawX = (int) Math.round(x - w / 2.0) - camX;
        int drawY = (int) Math.round(y - h / 2.0) - camY;
        g2.fillRect(drawX, drawY, w, h);
    }

    /**
     * Checks if the entity is a foe.
     */
    public boolean isFoe() {
        return alignment == Alignment.FOE;
    }

    /**
     * Checks if the entity is an ally.
     */
    public boolean isAlly() {
        return alignment == Alignment.ALLY;
    }

    /**
     * Checks if the entity is neutral.
     */
    public boolean isNeutral() {
        return alignment == Alignment.NEUTRAL;
    }

    /**
     * Gets the entity's alignment.
     */
    public Alignment getAlignment() {
        return alignment;
    }
}
