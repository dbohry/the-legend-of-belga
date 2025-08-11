package com.lhamacorp.games.tlob.entities;

import com.lhamacorp.games.tlob.managers.AudioManager;
import com.lhamacorp.games.tlob.managers.GameManager;
import com.lhamacorp.games.tlob.managers.KeyManager;
import com.lhamacorp.games.tlob.managers.TextureManager;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;
import com.lhamacorp.games.tlob.world.InputState;
import com.lhamacorp.games.tlob.world.PlayerInputView;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class Player extends Entity {

    private static final int PLAYER_SIZE = 22;
    private static final double PLAYER_SPEED = 3;
    private static final double PLAYER_MAX_HP = 6.0;
    private static final double PLAYER_MAX_STAMINA = 6.0;
    private static final double PLAYER_MAX_MANA = 0;
    private static final double PLAYER_MAX_SHIELD = 0;

    private static final int TICKS_PER_SECOND = 30;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int STAMINA_DRAIN_INTERVAL = TICKS_PER_SECOND;
    private static final int STAMINA_REGEN_INTERVAL = TICKS_PER_SECOND * 3;

    private int staminaDrainCounter = 0;
    private int staminaRegenCounter = 0;
    private boolean wasSprinting = false;

    private int attackTimer = 0;
    private int attackCooldown = 0;
    private double speedMultiplier = 1.0;
    private double damageMultiplier = 1.0;

    // Animation state
    private long animTimeMs = 0L;
    private boolean movingThisTick = false;

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
        this.maxHealth = Math.ceil(getMaxHealth() * (1.0 + pct));
    }

    public void increaseMaxStaminaByPercent(double pct) {
        this.maxStamina = Math.ceil(getMaxStamina() * (1.0 + pct));
    }

    public void increaseMaxManaByPercent(double pct) {
        if (getMaxMana() == 0) this.maxMana = 1.0;
        this.maxMana = Math.ceil(getMaxMana() * (1.0 + pct));
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
        weapon.setDamage((int) Math.ceil(weapon.getDamage() * (1.0 + pct)));
    }

    public void increaseWeaponRangeByPercent(double pct) {
        weapon.setReach((int) Math.ceil(weapon.getReach() * (1.0 + pct)));
    }

    public void increaseShield() {
        this.maxShield += 1.0;
    }

    public void increaseWeaponWidth() {
        weapon.setWidth(weapon.getWidth() + 1);
    }

    @Override
    public void update(Object... args) {
        final PlayerInputView input;
        Object k0 = (args.length > 0) ? args[0] : null;

        if (k0 instanceof PlayerInputView piv) {
            input = piv;
        } else if (k0 instanceof InputState is) {
            input = new PlayerInputView() {
                public boolean left() {
                    return is.left;
                }

                public boolean right() {
                    return is.right;
                }

                public boolean up() {
                    return is.up;
                }

                public boolean down() {
                    return is.down;
                }

                public boolean sprint() {
                    return is.shift;
                }

                public boolean attack() {
                    return is.attack;
                }
            };
        } else if (k0 instanceof KeyManager km) {
            input = new PlayerInputView() {
                public boolean left() {
                    return km.left;
                }

                public boolean right() {
                    return km.right;
                }

                public boolean up() {
                    return km.up;
                }

                public boolean down() {
                    return km.down;
                }

                public boolean sprint() {
                    return km.shift;
                }

                public boolean attack() {
                    return km.attack;
                }
            };
        } else {
            input = new PlayerInputView() {
                public boolean left() {
                    return false;
                }

                public boolean right() {
                    return false;
                }

                public boolean up() {
                    return false;
                }

                public boolean down() {
                    return false;
                }

                public boolean sprint() {
                    return false;
                }

                public boolean attack() {
                    return false;
                }
            };
        }

        TileMap map = (TileMap) args[1];
        @SuppressWarnings("unchecked") List<Enemy> enemies = (List<Enemy>) args[2];

        // Optional mouse-aim point in WORLD coords
        Point aimPoint = null;
        if (args.length >= 4 && args[3] instanceof Point p) aimPoint = p;

        // --- Input -> movement intent (WASD) ---
        int dxRaw = 0, dyRaw = 0;
        if (input.left()) dxRaw -= 1;
        if (input.right()) dxRaw += 1;
        if (input.up()) dyRaw -= 1;
        if (input.down()) dyRaw += 1;

        // --- Facing ---
        if (aimPoint != null) {
            double ax = aimPoint.x - x;
            double ay = aimPoint.y - y;
            if (Math.abs(ax) > 1e-6 || Math.abs(ay) > 1e-6) {
                double angle = Math.atan2(ay, ax);            // -PI..PI
                int oct = (int) Math.round(angle / (Math.PI / 4.0)); // nearest 45°
                switch (oct) {
                    case 0 -> facing = Direction.RIGHT;
                    case 1 -> facing = Direction.DOWN_RIGHT;
                    case 2 -> facing = Direction.DOWN;
                    case 3 -> facing = Direction.DOWN_LEFT;
                    case -1 -> facing = Direction.UP_RIGHT;
                    case -2 -> facing = Direction.UP;
                    case -3 -> facing = Direction.UP_LEFT;
                    default -> facing = Direction.LEFT; // 4 or -4
                }
            }
        } else if (dxRaw != 0 || dyRaw != 0) {
            if (dyRaw < 0) facing = (dxRaw < 0) ? Direction.UP_LEFT : (dxRaw > 0) ? Direction.UP_RIGHT : Direction.UP;
            else if (dyRaw > 0)
                facing = (dxRaw < 0) ? Direction.DOWN_LEFT : (dxRaw > 0) ? Direction.DOWN_RIGHT : Direction.DOWN;
            else facing = (dxRaw < 0) ? Direction.LEFT : Direction.RIGHT;
        }

        // --- Movement (normalize diagonals) ---
        double dx = dxRaw, dy = dyRaw;
        if (dx != 0 && dy != 0) {
            double inv = Math.sqrt(0.5);
            dx *= inv;
            dy *= inv;
        }

        movingThisTick = (dxRaw != 0 || dyRaw != 0) && knockbackTimer == 0;

        // --- Sprint / stamina ---
        boolean sprinting = input.sprint() && stamina >= 1.0;
        double speedBase = effectiveBaseSpeed();
        if (sprinting) {
            speed = speedBase * 2.0;
            if (++staminaDrainCounter >= STAMINA_DRAIN_INTERVAL) {
                stamina = Math.max(0, stamina - 1.0);
                staminaDrainCounter = 0;
            }
            staminaRegenCounter = 0;
        } else {
            speed = speedBase;
            if (!wasSprinting) {
                if (++staminaRegenCounter >= STAMINA_REGEN_INTERVAL) {
                    boolean did = false;
                    if (stamina < maxStamina) {
                        stamina = Math.min(maxStamina, stamina + 1.0);
                        did = true;
                    }
                    if (shield < maxShield) {
                        shield = Math.min(maxShield, shield + 1.0);
                        did = true;
                    }
                    if (did) staminaRegenCounter = 0;
                }
            } else {
                staminaRegenCounter = 0;
            }
            staminaDrainCounter = 0;
        }
        wasSprinting = sprinting;

        // --- Knockback & movement ---
        updateKnockbackWithMap(map);
        if (knockbackTimer == 0) moveWithCollision(dx * speed, dy * speed, map, enemies);

        // --- Attack ---
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        if (input.attack() && attackCooldown == 0 && attackTimer == 0 && stamina > 0) {
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

        // --- Advance local animation clock (30 Hz sim) ---
        animTimeMs += TICK_MS;
    }

    private void moveWithCollision(double dx, double dy, TileMap map, List<Enemy> enemies) {
        double newX = x + dx, newY = y + dy;

        if (!collidesWithMap(newX, y, map) && !collidesWithEnemies(newX, y, enemies)) x = newX;
        else {
            int step = (int) Math.signum(dx);
            while (step != 0 && !collidesWithMap(x + step, y, map) && !collidesWithEnemies(x + step, y, enemies))
                x += step;
        }

        if (!collidesWithMap(x, newY, map) && !collidesWithEnemies(x, newY, enemies)) y = newY;
        else {
            int step = (int) Math.signum(dy);
            while (step != 0 && !collidesWithMap(x, y + step, map) && !collidesWithEnemies(x, y + step, enemies))
                y += step;
        }
    }

    private boolean collidesWithEnemies(double cx, double cy, List<Enemy> enemies) {
        Rectangle playerBounds = getBoundsAt(cx, cy);
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && playerBounds.intersects(enemy.getBounds())) return true;
        }
        return false;
    }

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
            case UP -> {
                theta = -Math.PI / 2;
                ax = cx;
                ay = top;
            }
            case DOWN -> {
                theta = Math.PI / 2;
                ax = cx;
                ay = bottom;
            }
            case LEFT -> {
                theta = Math.PI;
                ax = left;
                ay = cy;
            }
            case RIGHT -> {
                theta = 0.0;
                ax = right;
                ay = cy;
            }
            case UP_RIGHT -> {
                theta = -Math.PI / 4;
                ax = right;
                ay = top;
            }
            case UP_LEFT -> {
                theta = -3 * Math.PI / 4;
                ax = left;
                ay = top;
            }
            case DOWN_RIGHT -> {
                theta = Math.PI / 4;
                ax = right;
                ay = bottom;
            }
            case DOWN_LEFT -> {
                theta = 3 * Math.PI / 4;
                ax = left;
                ay = bottom;
            }
            default -> {
                theta = 0.0;
                ax = right;
                ay = cy;
            }
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
        TextureManager.Direction dir = toCardinal(facing);
        TextureManager.Motion motion = movingThisTick ? TextureManager.Motion.WALK : TextureManager.Motion.IDLE;

        TextureManager.SpriteAnimation anim = TextureManager.getPlayerAnimation(dir, motion);
        BufferedImage tex = (anim != null && anim.length() > 0) ? anim.frameAt(animTimeMs) : TextureManager.getPlayerTexture();

        if (tex != null) {
            int pxImg = (int) Math.round(x - width / 2.0) - camX;
            int pyImg = (int) Math.round(y - height / 2.0) - camY;
            g2.drawImage(tex, pxImg, pyImg, width, height, null);
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

        // Facing indicator (kept)
        g2.setColor(new Color(10, 40, 15));
        int px = (int) Math.round(x) - camX;
        int py = (int) Math.round(y) - camY;
        switch (facing) {
            case UP -> g2.fillRect(px - 2, py - height / 2 - 4, 4, 4);
            case DOWN -> g2.fillRect(px - 2, py + height / 2, 4, 4);
            case LEFT -> g2.fillRect(px - width / 2 - 4, py - 2, 4, 4);
            case RIGHT -> g2.fillRect(px + width / 2, py - 2, 4, 4);
            case UP_RIGHT -> g2.fillRect(px + width / 2, py - height / 2 - 4, 4, 4);
            case DOWN_RIGHT -> g2.fillRect(px + width / 2, py + height / 2, 4, 4);
            case UP_LEFT -> g2.fillRect(px - width / 2 - 4, py - height / 2 - 4, 4, 4);
            case DOWN_LEFT -> g2.fillRect(px - width / 2 - 4, py + height / 2, 4, 4);
        }
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

    private static TextureManager.Direction toCardinal(Direction f) {
        return switch (f) {
            case UP, UP_LEFT, UP_RIGHT -> TextureManager.Direction.UP;
            case DOWN, DOWN_LEFT, DOWN_RIGHT -> TextureManager.Direction.DOWN;
            case LEFT -> TextureManager.Direction.LEFT;
            default -> TextureManager.Direction.RIGHT;
        };
    }
}
