package com.lhamacorp.games.tlob.entities;

import com.lhamacorp.games.tlob.managers.AudioManager;
import com.lhamacorp.games.tlob.managers.GameManager;
import com.lhamacorp.games.tlob.managers.KeyManager;
import com.lhamacorp.games.tlob.managers.TextureManager;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class Player extends Entity {

    private static final int PLAYER_SIZE = 22;
    private static final double PLAYER_SPEED = 1.8;
    private static final double PLAYER_MAX_HP = 6.0;
    private static final double PLAYER_MAX_STAMINA = 6.0;
    private static final double PLAYER_MAX_MANA = 0;
    private static final double PLAYER_MAX_SHIELD = 0;

    private static final int FPS = 60;
    private static final int STAMINA_DRAIN_INTERVAL = FPS;
    private static final int STAMINA_REGEN_INTERVAL = FPS * 3;

    private int staminaDrainCounter = 0;
    private int staminaRegenCounter = 0;
    private boolean wasSprinting = false;

    private int attackTimer = 0;
    private int attackCooldown = 0;
    private double speedMultiplier = 1.0;
    private double damageMultiplier = 1.0;

    public Player(double x, double y, Weapon weapon) {
        super(x, y, PLAYER_SIZE, PLAYER_SIZE, PLAYER_SPEED, PLAYER_MAX_HP, PLAYER_MAX_STAMINA, PLAYER_MAX_MANA, PLAYER_MAX_SHIELD, weapon);
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
        this.shield = this.maxShield;
    }

    public void increaseMaxHealthByPercent(double pct) {
        double oldMax = getMaxHealth();
        this.maxHealth = Math.ceil(oldMax * (1.0 + pct));
    }

    public void increaseMaxStaminaByPercent(double pct) {
        double oldMax = getMaxStamina();
        this.maxStamina = Math.ceil(oldMax * (1.0 + pct));
    }

    public void increaseMaxManaByPercent(double pct) {
        if (getMaxMana() == 0) this.maxMana = 1.0;
        double oldMax = getMaxMana();
        this.maxMana = Math.ceil(oldMax * (1.0 + pct));
    }

    private double effectiveBaseSpeed() {
        return PLAYER_SPEED * speedMultiplier;
    }

    private double effectiveAttackDamage() {
        return weapon.getDamage() * damageMultiplier;
    }

    public void increaseMoveSpeedByPercent(double pct) {
        speedMultiplier *= (1.0 + pct);
    }

    public void increaseAttackDamageByPercent(double pct) {
        int currentDamage = weapon.getDamage();
        int newDamage = (int) Math.ceil(currentDamage * (1.0 + pct));
        weapon.setDamage(newDamage);
    }

    public void increaseWeaponRangeByPercent(double pct) {
        int currentReach = weapon.getReach();
        int newReach = (int) Math.ceil(currentReach * (1.0 + pct));
        weapon.setReach(newReach);
    }

    public void increaseShield() {
        this.maxShield += 1.0;
    }

    @Override
    public void update(Object... args) {
        KeyManager keys = (KeyManager) args[0];
        TileMap map = (TileMap) args[1];
        @SuppressWarnings("unchecked")
        List<Enemy> enemies = (List<Enemy>) args[2];

        // Input + 8-way facing
        int dxRaw = 0, dyRaw = 0;
        if (keys.left) dxRaw -= 1;
        if (keys.right) dxRaw += 1;
        if (keys.up) dyRaw -= 1;
        if (keys.down) dyRaw += 1;

        if (dxRaw != 0 || dyRaw != 0) {
            if (dyRaw < 0) {
                if (dxRaw < 0) facing = Direction.UP_LEFT;
                else if (dxRaw > 0) facing = Direction.UP_RIGHT;
                else facing = Direction.UP;
            } else if (dyRaw > 0) {
                if (dxRaw < 0) facing = Direction.DOWN_LEFT;
                else if (dxRaw > 0) facing = Direction.DOWN_RIGHT;
                else facing = Direction.DOWN;
            } else {
                facing = (dxRaw < 0) ? Direction.LEFT : Direction.RIGHT;
            }
        }

        // Movement (normalize diagonals)
        double dx = dxRaw, dy = dyRaw;
        if (dx != 0 && dy != 0) {
            double inv = Math.sqrt(0.5);
            dx *= inv;
            dy *= inv;
        }

        boolean sprinting = keys.shift && stamina >= 1.0;
        double speedBase = effectiveBaseSpeed();
        if (sprinting) {
            speed = speedBase * 2.0;
            staminaDrainCounter++;
            if (staminaDrainCounter >= STAMINA_DRAIN_INTERVAL) {
                stamina = Math.max(0, stamina - 1.0);
                staminaDrainCounter = 0;
            }
            staminaRegenCounter = 0;
        } else {
            speed = speedBase;
            if (!wasSprinting) {
                staminaRegenCounter++;
                if (staminaRegenCounter >= STAMINA_REGEN_INTERVAL) {
                    boolean didRegen = false;
                    if (stamina < maxStamina) {
                        stamina = Math.min(maxStamina, stamina + 1.0);
                        didRegen = true;
                    }
                    if (shield < maxShield) {
                        shield = Math.min(maxShield, shield + 1.0);
                        didRegen = true;
                    }
                    if (didRegen) staminaRegenCounter = 0;
                }
            } else {
                staminaRegenCounter = 0;
            }
            staminaDrainCounter = 0;
        }
        wasSprinting = sprinting;

        // Knockback & movement
        updateKnockbackWithMap(map);
        if (knockbackTimer == 0) moveWithCollision(dx * speed, dy * speed, map, enemies);

        // Attack
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        if (keys.attack && attackCooldown == 0 && attackTimer == 0 && stamina > 0) {
            stamina -= 0.5;
            attackTimer = weapon.getDuration();
            attackCooldown = weapon.getCooldown();

            Shape swing = getSwordSwingShape();
            boolean hitSomething = false;

            double dmg = effectiveAttackDamage();
            for (Enemy e : enemies) {
                if (e.isAlive() && swing.intersects(e.getBounds())) {
                    e.damage(dmg);
                    e.applyKnockback(x, y);
                    hitSomething = true;
                }
            }

            if (damageWallsInShape(swing, map, dmg)) hitSomething = true;

            if (hitSomething) AudioManager.playSound("slash-hit.wav", -15.0f);
            else AudioManager.playSound("slash-clean.wav");
        }
    }

    private void moveWithCollision(double dx, double dy, TileMap map, List<Enemy> enemies) {
        double newX = x + dx;
        double newY = y + dy;

        if (!collidesWithMap(newX, y, map) && !collidesWithEnemies(newX, y, enemies)) {
            x = newX;
        } else {
            int step = (int) Math.signum(dx);
            while (step != 0 && !collidesWithMap(x + step, y, map) && !collidesWithEnemies(x + step, y, enemies)) {
                x += step;
            }
        }

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

    // Rotated blade shape honoring reach & width for all 8 directions
    private Shape getSwordSwingShape() {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);
        int r = weapon.getReach();
        int w = weapon.getWidth();

        int left = (int) Math.round(x - width / 2.0);
        int right = (int) Math.round(x + width / 2.0);
        int top = (int) Math.round(y - height / 2.0);
        int bottom = (int) Math.round(y + height / 2.0);

        double theta, ax, ay;
        switch (facing) {
            case UP:
                theta = -Math.PI / 2;
                ax = cx;
                ay = top;
                break;
            case DOWN:
                theta = Math.PI / 2;
                ax = cx;
                ay = bottom;
                break;
            case LEFT:
                theta = Math.PI;
                ax = left;
                ay = cy;
                break;
            case RIGHT:
                theta = 0.0;
                ax = right;
                ay = cy;
                break;
            case UP_RIGHT:
                theta = -Math.PI / 4;
                ax = right;
                ay = top;
                break;
            case UP_LEFT:
                theta = -3 * Math.PI / 4;
                ax = left;
                ay = top;
                break;
            case DOWN_RIGHT:
                theta = Math.PI / 4;
                ax = right;
                ay = bottom;
                break;
            case DOWN_LEFT:
                theta = 3 * Math.PI / 4;
                ax = left;
                ay = bottom;
                break;
            default:
                theta = 0.0;
                ax = right;
                ay = cy;
                break;
        }

        double cxBlade = ax + Math.cos(theta) * (r / 2.0);
        double cyBlade = ay + Math.sin(theta) * (r / 2.0);

        Rectangle2D.Double blade = new Rectangle2D.Double(-r / 2.0, -w / 2.0, r, w);

        AffineTransform at = new AffineTransform();
        at.translate(cxBlade, cyBlade);
        at.rotate(theta);
        return at.createTransformedShape(blade);
    }

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        BufferedImage tex = TextureManager.getPlayerTexture();
        if (tex != null) {
            int px = (int) Math.round(x - width / 2.0) - camX;
            int py = (int) Math.round(y - height / 2.0) - camY;
            g2.drawImage(tex, px, py, width, height, null);
        } else {
            drawCenteredRect(g2, camX, camY, width, height, new Color(40, 160, 70));
        }

        if (attackTimer > 0) {
            Shape swingWorld = getSwordSwingShape();
            Shape swing = AffineTransform.getTranslateInstance(-camX, -camY).createTransformedShape(swingWorld);

            g2.setColor(new Color(255, 255, 180, 180));
            g2.fill(swing);
            g2.setColor(new Color(255, 255, 255, 220));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(swing);
        }

        // Facing indicator (includes diagonals)
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
            case UP_RIGHT:
                g2.fillRect(px + width / 2, py - height / 2 - 4, 4, 4);
                break;
            case DOWN_RIGHT:
                g2.fillRect(px + width / 2, py + height / 2, 4, 4);
                break;
            case UP_LEFT:
                g2.fillRect(px - width / 2 - 4, py - height / 2 - 4, 4, 4);
                break;
            case DOWN_LEFT:
                g2.fillRect(px - width / 2 - 4, py + height / 2, 4, 4);
                break;
        }
    }

    private boolean damageWallsInShape(Shape swing, TileMap map, double damage) {
        int tileSize = GameManager.TILE_SIZE;
        boolean damaged = false;

        Rectangle2D b = swing.getBounds2D();
        int startTileX = Math.max(0, (int) Math.floor(b.getX() / tileSize));
        int endTileX = Math.min(map.getWidth() - 1, (int) Math.floor((b.getX() + b.getWidth() - 1) / tileSize));
        int startTileY = Math.max(0, (int) Math.floor(b.getY() / tileSize));
        int endTileY = Math.min(map.getHeight() - 1, (int) Math.floor((b.getY() + b.getHeight() - 1) / tileSize));

        for (int ty = startTileY; ty <= endTileY; ty++) {
            for (int tx = startTileX; tx <= endTileX; tx++) {
                if (!map.isWall(tx, ty)) continue;
                Rectangle2D tileRect = new Rectangle2D.Double(tx * tileSize, ty * tileSize, tileSize, tileSize);
                if (swing.intersects(tileRect)) {
                    if (map.damageWall(tx, ty, damage)) damaged = true;
                }
            }
        }
        return damaged;
    }

    @Override
    public void damage(double amount) {
        super.damage(amount);
        if (isAlive()) AudioManager.playSound("hero-hurt.wav", -10);
        else AudioManager.playSound("hero-death.wav");
    }

    @Override
    protected void applyKnockbackMovement() {
        double newX = x + knockbackX;
        double newY = y + knockbackY;
        if (!collidesWithMap(newX, newY, null)) {
            x = newX;
            y = newY;
        } else {
            knockbackTimer = 0;
            knockbackX = 0;
            knockbackY = 0;
        }
    }
}
