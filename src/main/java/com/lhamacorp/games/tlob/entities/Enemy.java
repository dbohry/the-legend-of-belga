package com.lhamacorp.games.tlob.entities;

import com.lhamacorp.games.tlob.managers.TextureManager;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Enemy extends Entity {

    private static final int ENEMY_SIZE = 20;
    private static final double ENEMY_SPEED = 2;
    private static final double ENEMY_MAX_HP = 1.0;
    private static final double ENEMY_MAX_STAMINA = 1.0;
    private static final double ENEMY_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 30;
    private static final int ATTACK_COOLDOWN_FRAMES = 120;
    private static final int ATTACK_DURATION_FRAMES = 8;
    private static final double ATTACK_DAMAGE = 1.0;

    private int hurtTimer = 0;
    private int attackCooldown = 0;
    private int attackTimer = 0;

    public Enemy(double x, double y, Weapon weapon) {
        super(x, y, ENEMY_SIZE, ENEMY_SIZE, ENEMY_SPEED, ENEMY_MAX_HP, ENEMY_MAX_STAMINA, ENEMY_MAX_MANA, 0, weapon, "Zombie");
    }

    @Override
    public void update(Object... args) {
        if (!isAlive()) return;
        Player player = (Player) args[0];
        TileMap map = (TileMap) args[1];

        if (hurtTimer > 0) hurtTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        // Check if we can attack the player
        double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
        if (distToPlayer <= ATTACK_RANGE && attackCooldown == 0 && attackTimer == 0) {
            attackTimer = ATTACK_DURATION_FRAMES;
            attackCooldown = ATTACK_COOLDOWN_FRAMES;
            player.damage(ATTACK_DAMAGE);
            player.applyKnockback(x, y); // Apply knockback from enemy position
        }

        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double dist = distToPlayer;
        if (dist > 0.0001) {
            dx /= dist;
            dy /= dist;
        }

        // Update knockback first
        updateKnockbackWithMap(map);

        // Only allow movement if not being knocked back
        if (knockbackTimer == 0) {
            double desiredSpeed = speed;
            if (dist > 300 || attackTimer > 0) {
                // Far away: wander slowly
                double angle = (System.nanoTime() / 1_000_000_000.0 + hash()) % (2 * Math.PI);
                dx = Math.cos(angle);
                dy = Math.sin(angle);
                desiredSpeed = 0.6;
            }

            moveWithCollision(dx * desiredSpeed, dy * desiredSpeed, map, player);
        }
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
        hurtTimer = 10;
    }

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!isAlive()) return;

        BufferedImage tex = TextureManager.getEnemyTexture();
        if (tex != null) {
            int px = (int) Math.round(x - width / 2.0) - camX;
            int py = (int) Math.round(y - height / 2.0) - camY;
            g2.drawImage(tex, px, py, width, height, null);

            // Overlay color based on state
            if (hurtTimer > 0 || attackTimer > 0) {
                Color overlay = hurtTimer > 0 ? new Color(255, 120, 120, 100) : new Color(255, 200, 60, 100);
                g2.setColor(overlay);
                g2.fillRect(px, py, width, height);
            }
        } else {
            Color c;
            if (hurtTimer > 0) {
                c = new Color(255, 120, 120);
            } else if (attackTimer > 0) {
                c = new Color(255, 200, 60); // Attack color
            } else {
                c = new Color(200, 60, 60);
            }
            drawCenteredRect(g2, camX, camY, width, height, c);
        }
    }

}
