package com.lhamacorp.games.tlob;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Player extends Entity {
    private static final int PLAYER_SIZE = 22;
    private static final double PLAYER_SPEED = 1.8;
    private static final double PLAYER_MAX_HP = 6.0;
    private static final double PLAYER_MAX_STAMINA = 6.0;
    private static final double PLAYER_MAX_MANA = 1;

    private static final int SWORD_REACH = 28;
    private static final int SWORD_WIDTH = 12;
    private static final int ATTACK_DURATION_FRAMES = 10;
    private static final int ATTACK_COOLDOWN_FRAMES = 16;
    private static final double ATTACK_DAMAGE = 2.0;

    private static final int FPS = 60;
    private static final int STAMINA_DRAIN_INTERVAL = FPS;
    private static final int STAMINA_REGEN_INTERVAL = FPS * 2;

    private int staminaDrainCounter = 0;
    private int staminaRegenCounter = 0;
    private boolean wasSprinting = false;

    private int attackTimer = 0;
    private int attackCooldown = 0;

    public Player(double x, double y) {
        super(x, y, PLAYER_SIZE, PLAYER_SIZE, PLAYER_SPEED, PLAYER_MAX_HP, PLAYER_MAX_STAMINA, PLAYER_MAX_MANA);
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void heal() {
        this.health = maxHealth;
        this.alive = true;
    }

    public void restoreAll() {
        this.health = this.maxHealth;
        this.mana = this.maxMana;
        this.stamina = this.maxStamina;
    }

    @Override
    public void update(Object... args) {
        KeyManager keys = (KeyManager) args[0];
        TileMap map = (TileMap) args[1];
        @SuppressWarnings("unchecked")
        List<Enemy> enemies = (List<Enemy>) args[2];

        double dx = 0, dy = 0;
        if (keys.up) {
            dy -= 1;
            facing = Direction.UP;
        }
        if (keys.down) {
            dy += 1;
            facing = Direction.DOWN;
        }
        if (keys.left) {
            dx -= 1;
            facing = Direction.LEFT;
        }
        if (keys.right) {
            dx += 1;
            facing = Direction.RIGHT;
        }

        boolean sprinting = keys.shift && stamina >= 1.0;

        if (sprinting) {
            speed = PLAYER_SPEED * 2;
            staminaDrainCounter++;

            if (staminaDrainCounter >= STAMINA_DRAIN_INTERVAL) {
                stamina -= 1.0;
                if (stamina < 0) stamina = 0;
                staminaDrainCounter = 0;
            }

            staminaRegenCounter = 0;
        } else {
            speed = PLAYER_SPEED;
            if (!wasSprinting) {
                staminaRegenCounter++;
                if (staminaRegenCounter >= STAMINA_REGEN_INTERVAL && stamina < maxStamina) {
                    stamina += 1.0;
                    if (stamina > maxStamina) stamina = maxStamina;
                    staminaRegenCounter = 0;
                }
            } else {
                staminaRegenCounter = 0;
            }

            staminaDrainCounter = 0;
        }

        wasSprinting = sprinting;

        // Normalize diagonal
        if (dx != 0 && dy != 0) {
            dx *= Math.sqrt(0.5);
            dy *= Math.sqrt(0.5);
        }

        // Update knockback first
        updateKnockbackWithMap(map);

        // Only allow movement if not being knocked back
        if (knockbackTimer == 0) {
            moveWithCollision(dx * speed, dy * speed, map, enemies);
        }

        // Handle attack
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        if (keys.attack && attackCooldown == 0 && attackTimer == 0 && stamina > 0) {
            stamina -= 0.5;
            attackTimer = ATTACK_DURATION_FRAMES;
            attackCooldown = ATTACK_COOLDOWN_FRAMES;

            Rectangle hit = getSwordHitbox();
            boolean hitSomething = false;

            for (Enemy e : enemies) {
                if (e.isAlive() && hit.intersects(e.getBounds())) {
                    e.damage(ATTACK_DAMAGE);
                    e.applyKnockback(x, y);
                    hitSomething = true;
                }
            }

            // Damage walls in sword hitbox
            boolean hitWall = damageWallsInHitbox(hit, map);
            if (hitWall) {
                hitSomething = true;
            }

            // Play sound based on result
            if (hitSomething) {
                AudioManager.playSound("slash-hit.wav", -10.0f);
            } else {
                AudioManager.playSound("slash-clean.wav");
            }
        }
    }

    private void moveWithCollision(double dx, double dy, TileMap map, List<Enemy> enemies) {
        double newX = x + dx;
        double newY = y + dy;

        // Horizontal collision
        if (!collidesWithMap(newX, y, map) && !collidesWithEnemies(newX, y, enemies)) {
            x = newX;
        } else {
            // Try to slide along wall horizontally in small steps
            int step = (int) Math.signum(dx);
            while (step != 0 && !collidesWithMap(x + step, y, map) && !collidesWithEnemies(x + step, y, enemies)) {
                x += step;
            }
        }

        // Vertical collision
        if (!collidesWithMap(x, newY, map) && !collidesWithEnemies(x, newY, enemies)) {
            y = newY;
        } else {
            int step = (int) Math.signum(dy);
            while (step != 0 && !collidesWithMap(x, y + step, map) && !collidesWithEnemies(x, y + step, enemies)) {
                y += step;
            }
        }
    }

    private boolean collidesWithEnemies(double cx, double cy, List<Enemy> enemies) {
        Rectangle playerBounds = getBoundsAt(cx, cy);
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && playerBounds.intersects(enemy.getBounds())) {
                return true;
            }
        }
        return false;
    }

    private Rectangle getSwordHitbox() {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);
        switch (facing) {
            case UP:
                return new Rectangle(cx - SWORD_WIDTH / 2, cy - height / 2 - SWORD_REACH, SWORD_WIDTH, SWORD_REACH);
            case DOWN:
                return new Rectangle(cx - SWORD_WIDTH / 2, cy + height / 2, SWORD_WIDTH, SWORD_REACH);
            case LEFT:
                return new Rectangle(cx - width / 2 - SWORD_REACH, cy - SWORD_WIDTH / 2, SWORD_REACH, SWORD_WIDTH);
            case RIGHT:
            default:
                return new Rectangle(cx + width / 2, cy - SWORD_WIDTH / 2, SWORD_REACH, SWORD_WIDTH);
        }
    }

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        // Player body
        BufferedImage tex = TextureManager.getPlayerTexture();
        if (tex != null) {
            int px = (int) Math.round(x - width / 2.0) - camX;
            int py = (int) Math.round(y - height / 2.0) - camY;
            g2.drawImage(tex, px, py, width, height, null);
        } else {
            drawCenteredRect(g2, camX, camY, width, height, new Color(40, 160, 70));
        }

        // Draw sword swing indicator
        if (attackTimer > 0) {
            Rectangle hit = getSwordHitbox();
            g2.setColor(new Color(255, 255, 180, 180));
            g2.fillRect(hit.x - camX, hit.y - camY, hit.width, hit.height);
            g2.setColor(new Color(255, 255, 255, 220));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(hit.x - camX, hit.y - camY, hit.width, hit.height);
        }

        // Optional: small facing indicator
        g2.setColor(new Color(10, 40, 15));
        int px = (int) Math.round(x) - camX;
        int py = (int) Math.round(y) - camY;
        switch (facing) {
            case UP:
                g2.fillRect(px - 2, py - height / 2 - 4, 4, 4);
                break;
            case DOWN:
                g2.fillRect(px - 2, py + height / 2, 4, 4);
                break;
            case LEFT:
                g2.fillRect(px - width / 2 - 4, py - 2, 4, 4);
                break;
            case RIGHT:
                g2.fillRect(px + width / 2, py - 2, 4, 4);
                break;
        }
    }

    private boolean damageWallsInHitbox(Rectangle hitbox, TileMap map) {
        int tileSize = GamePanel.TILE_SIZE;
        boolean damaged = false;

        int startTileX = Math.max(0, hitbox.x / tileSize);
        int endTileX = Math.min(map.getWidth() - 1, (hitbox.x + hitbox.width - 1) / tileSize);
        int startTileY = Math.max(0, hitbox.y / tileSize);
        int endTileY = Math.min(map.getHeight() - 1, (hitbox.y + hitbox.height - 1) / tileSize);

        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                if (map.isWall(tileX, tileY)) {
                    if (map.damageWall(tileX, tileY, ATTACK_DAMAGE)) {
                        damaged = true;
                    }
                }
            }
        }

        return damaged;
    }

    @Override
    public void damage(double amount) {
        super.damage(amount);

        if (isAlive()) {
            AudioManager.playSound("hero-hurt.wav", -5);
        } else {
            AudioManager.playSound("hero-death.wav");
        }
    }

    @Override
    protected void applyKnockbackMovement() {
        double newX = x + knockbackX;
        double newY = y + knockbackY;

        // Check map collision for knockback movement
        if (!collidesWithMap(newX, newY, null)) {
            x = newX;
            y = newY;
        } else {
            // Stop knockback if we hit a wall
            knockbackTimer = 0;
            knockbackX = 0;
            knockbackY = 0;
        }
    }

}
