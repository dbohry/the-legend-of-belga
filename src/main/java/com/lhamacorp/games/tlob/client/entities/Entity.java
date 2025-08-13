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
    public Entity(double x, double y, int width, int height, double speed, double maxHealth, double maxStamina, double maxMana, double maxShield, Weapon weapon) {
        this(x, y, width, height, speed, maxHealth, maxStamina, maxMana, maxShield, weapon, "Entity", Alignment.NEUTRAL);
    }

    /**
     * Creates an entity with specified parameters.
     */
    public Entity(double x, double y, int width, int height, double speed, double maxHealth, double maxStamina, double maxMana, double maxShield, Weapon weapon, String name, Alignment alignment) {
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
     * Applies damage to the entity, absorbing with shield first.
     */
    public void damage(double amount) {
        if (!alive) return;

        double remaining = amount;

        if (shield > 0) {
            double absorbed = Math.min(shield, remaining);
            shield -= absorbed;
            remaining -= absorbed;
        }

        if (remaining > 0) {
            health -= remaining;
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
