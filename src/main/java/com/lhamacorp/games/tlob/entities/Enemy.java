package com.lhamacorp.games.tlob.entities;

import com.lhamacorp.games.tlob.managers.TextureManager;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Enemy extends Entity {

    private static final int ENEMY_SIZE = 20;
    private static final double ENEMY_SPEED = 3.5;

    private static final double ENEMY_MAX_HP = 1.0;
    private static final double ENEMY_MAX_STAMINA = 1.0;
    private static final double ENEMY_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 30;

    // --- 30 Hz timing ---
    private static final int TICKS_PER_SECOND = 30;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int ATTACK_COOLDOWN_TICKS = 60; // 2.0s @30Hz
    private static final int ATTACK_DURATION_TICKS = 3;  // 0.1s  @30Hz
    private static final int HURT_FLASH_TICKS = 6;  // ~0.2s

    private int hurtTimer = 0;
    private int attackCooldown = 0;
    private int attackTimer = 0;

    private long animTimeMs = 0L;
    private boolean movedThisTick = false;

    private int wanderTimer = 0;
    private int lcg;
    private double wanderDx = 0, wanderDy = 0;

    public Enemy(double x, double y, Weapon weapon) {
        super(x, y, ENEMY_SIZE, ENEMY_SIZE, ENEMY_SPEED, ENEMY_MAX_HP, ENEMY_MAX_STAMINA, ENEMY_MAX_MANA, 0, weapon, "Zombie");
        int seed = (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) ^ 0x9E3779B9);
        lcg = (seed == 0) ? 1 : seed;
        pickNewWanderDir(); // init
    }

    @Override
    public void update(Object... args) {
        if (!isAlive()) return;
        Player player = (Player) args[0];
        TileMap map = (TileMap) args[1];

        if (hurtTimer > 0) hurtTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
        if (distToPlayer <= ATTACK_RANGE && attackCooldown == 0 && attackTimer == 0) {
            attackTimer = ATTACK_DURATION_TICKS;
            attackCooldown = ATTACK_COOLDOWN_TICKS;
            player.damage(1.0);
            player.applyKnockback(x, y);
        }

        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double dist = distToPlayer;
        if (dist > 1e-4) {
            dx /= dist;
            dy /= dist;
        } else {
            dx = 0;
            dy = 0;
        }

        updateKnockbackWithMap(map);

        double vx = 0, vy = 0;
        if (knockbackTimer == 0) {
            double desiredSpeed = speed;

            boolean wander = (distToPlayer > 300) || (attackTimer > 0);
            if (wander) {
                if (--wanderTimer <= 0) pickNewWanderDir();
                dx = wanderDx;
                dy = wanderDy;
                desiredSpeed = 0.6;
            }

            vx = dx * desiredSpeed;
            vy = dy * desiredSpeed;
            moveWithCollision(vx, vy, map, player);
        }

        updateFacing(vx, vy);
        movedThisTick = (Math.abs(vx) + Math.abs(vy)) > 1e-3 && knockbackTimer == 0;

        animTimeMs += TICK_MS;
    }

    private int nextRand() {
        lcg = lcg * 1664525 + 1013904223;
        return lcg;
    }

    private void pickNewWanderDir() {
        // new direction every 0.5–1.2s @30Hz
        int span = 15 + Math.abs(nextRand()) % 21; // 15..35 ticks
        wanderTimer = span;

        // angle from RNG
        double u = (nextRand() >>> 8) * (1.0 / (1 << 24)); // [0,1)
        double ang = u * Math.PI * 2.0;
        wanderDx = Math.cos(ang);
        wanderDy = Math.sin(ang);
    }

    private int hash() {
        return (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) & 0x7fffffff);
    }

    private void moveWithCollision(double dx, double dy, TileMap map, Player player) {
        double newX = x + dx;
        double newY = y + dy;

        if (!collidesWithMap(newX, y, map) && !collidesWithPlayer(newX, y, player)) x = newX;
        if (!collidesWithMap(x, newY, map) && !collidesWithPlayer(x, newY, player)) y = newY;
    }

    private boolean collidesWithPlayer(double cx, double cy, Player player) {
        Rectangle enemyBounds = getBoundsAt(cx, cy);
        return enemyBounds.intersects(player.getBounds());
    }

    @Override
    public void damage(double amount) {
        super.damage(amount);
        hurtTimer = HURT_FLASH_TICKS;
    }

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!isAlive()) return;

        TextureManager.Direction dir = toCardinal(facing);
        TextureManager.Motion motion = movedThisTick ? TextureManager.Motion.WALK : TextureManager.Motion.IDLE;

        BufferedImage tex = TextureManager.getEnemyFrame(dir, motion, animTimeMs);
        int px = (int) Math.round(x - width / 2.0) - camX;
        int py = (int) Math.round(y - height / 2.0) - camY;

        if (tex != null) {
            g2.drawImage(tex, px, py, width, height, null);

            // State overlays
            if (hurtTimer > 0 || attackTimer > 0) {
                Color overlay = (hurtTimer > 0) ? new Color(255, 120, 120, 100)
                    : new Color(255, 200, 60, 100);
                g2.setColor(overlay);
                g2.fillRect(px, py, width, height);
            }
        } else {
            // Fallback rectangle if texture missing
            Color c = (hurtTimer > 0) ? new Color(255, 120, 120)
                : (attackTimer > 0) ? new Color(255, 200, 60)
                : new Color(200, 60, 60);
            drawCenteredRect(g2, camX, camY, width, height, c);
        }
    }

    private void updateFacing(double vx, double vy) {
        if (Math.abs(vx) < 1e-3 && Math.abs(vy) < 1e-3) return; // keep current
        if (Math.abs(vx) > Math.abs(vy)) {
            facing = (vx < 0) ? Direction.LEFT : Direction.RIGHT;
        } else {
            facing = (vy < 0) ? Direction.UP : Direction.DOWN;
        }
    }

    // Map 8-way Entity.Direction to 4 cardinal rows
    private static TextureManager.Direction toCardinal(Direction f) {
        return switch (f) {
            case UP, UP_LEFT, UP_RIGHT -> TextureManager.Direction.UP;
            case DOWN, DOWN_LEFT, DOWN_RIGHT -> TextureManager.Direction.DOWN;
            case LEFT -> TextureManager.Direction.LEFT;
            default -> TextureManager.Direction.RIGHT;
        };
    }
}
