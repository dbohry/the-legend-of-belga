package com.lhamacorp.games.tlob.entities;

import com.lhamacorp.games.tlob.managers.GameManager;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;

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

    protected Direction facing = Direction.DOWN;

    // Knockback system
    protected double knockbackX = 0;
    protected double knockbackY = 0;
    protected int knockbackTimer = 0;
    protected static final int KNOCKBACK_DURATION = 8;
    protected static final double KNOCKBACK_FORCE = 4.0;

    public enum Direction {UP, DOWN, LEFT, RIGHT}

    public Entity(double x, double y, int width, int height, double speed, double maxHealth, double maxStamina, double maxMana, double maxShield, Weapon weapon) {
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
    }

    public boolean isAlive() {
        return alive;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getShield() {
        return shield;
    }

    public double getMaxShield() {
        return maxShield;
    }

    public double getMana() {
        return mana;
    }

    public double getMaxMana() {
        return maxMana;
    }

    public double getStamina() {
        return stamina;
    }

    public double getMaxStamina() {
        return maxStamina;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public Rectangle getBoundsAt(double cx, double cy) {
        int halfW = width / 2;
        int halfH = height / 2;
        return new Rectangle((int) Math.round(cx - halfW), (int) Math.round(cy - halfH), width, height);
    }

    public Rectangle getBounds() {
        return getBoundsAt(x, y);
    }

    public void damage(double amount) {
        if (!alive) return;

        double remaining = amount;

        // absorb with shield first
        if (shield > 0) {
            double absorbed = Math.min(shield, remaining);
            shield -= absorbed;
            remaining -= absorbed;
        }

        // then health
        if (remaining > 0) {
            health -= remaining;
        }

        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public void applyKnockback(double fromX, double fromY) {
        // Calculate direction from attacker to this entity
        double dx = x - fromX;
        double dy = y - fromY;
        double distance = Math.hypot(dx, dy);

        if (distance > 0) {
            // Normalize and apply knockback force
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

        // Check if player is trying to go outside map boundaries
        if (bounds.x < 0 || bounds.y < 0 ||
            bounds.x + bounds.width > map.getWidth() * GameManager.TILE_SIZE ||
            bounds.y + bounds.height > map.getHeight() * GameManager.TILE_SIZE) {
            return true; // Collision with map boundaries
        }

        // Check tile-based collisions
        int left = bounds.x / GameManager.TILE_SIZE;
        int right = (bounds.x + bounds.width - 1) / GameManager.TILE_SIZE;
        int top = bounds.y / GameManager.TILE_SIZE;
        int bottom = (bounds.y + bounds.height - 1) / GameManager.TILE_SIZE;

        for (int ty = top; ty <= bottom; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (map.isWall(tx, ty)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Method to be overridden by subclasses for collision checking during knockback
    protected void applyKnockbackMovement() {
        // Default implementation - just move without collision checking
        x += knockbackX;
        y += knockbackY;
    }

    public abstract void update(Object... args);

    public abstract void draw(Graphics2D g2, int camX, int camY);

    protected void drawCenteredRect(Graphics2D g2, int camX, int camY, int w, int h, Color color) {
        g2.setColor(color);
        int drawX = (int) Math.round(x - w / 2.0) - camX;
        int drawY = (int) Math.round(y - h / 2.0) - camY;
        g2.fillRect(drawX, drawY, w, h);
    }
}
