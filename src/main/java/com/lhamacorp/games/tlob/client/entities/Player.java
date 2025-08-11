package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.AudioManager;
import com.lhamacorp.games.tlob.client.managers.BaseGameManager;
import com.lhamacorp.games.tlob.client.managers.KeyManager;
import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import com.lhamacorp.games.tlob.client.world.InputState;
import com.lhamacorp.games.tlob.client.world.PlayerInputView;

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
    private static final double PLAYER_SPEED_PPS = 90.0;

    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int STAMINA_DRAIN_INTERVAL = TICKS_PER_SECOND;
    private static final int STAMINA_REGEN_INTERVAL = TICKS_PER_SECOND * 3;

    private int staminaDrainCounter = 0;
    private int staminaRegenCounter = 0;
    private double staminaRegenRateMult = 1.0;
    private boolean wasSprinting = false;
    private double facingAngle = 0.0;

    private int attackTimer = 0;
    private int attackCooldown = 0;
    private double speedMultiplier = 1.0;
    private double damageMultiplier = 1.0;

    private long animTimeMs = 0L;
    private boolean movingThisTick = false;

    public Player(double x, double y, Weapon weapon) {
        super(x, y, PLAYER_SIZE, PLAYER_SIZE, PLAYER_SPEED, PLAYER_MAX_HP, PLAYER_MAX_STAMINA, PLAYER_MAX_MANA, PLAYER_MAX_SHIELD, weapon);
    }

    // --- Authoritative snapshot helpers (silent setters; no sounds/effects) ---
    public void setPosition(double x, double y) { this.x = x; this.y = y; }

    public void setHealth(double h) {
        this.health = Math.max(0.0, Math.min(h, maxHealth));
        this.alive = (this.health > 0.0);
    }

    public void setStamina(double s) {
        this.stamina = Math.max(0.0, Math.min(s, maxStamina));
    }

    public void setShield(double sh) {
        this.shield = Math.max(0.0, Math.min(sh, maxShield));
    }

    /** Maps server octant (0..7) to local facing enum + angle. 0=→, 1=↘, 2=↓, 3=↙, 4=←, 5=↖, 6=↑, 7=↗ */
    public void setFacingOctant(int octant) {
        int o = ((octant % 8) + 8) % 8;      // wrap to [0..7]
        int signed = (o <= 4) ? o : o - 8;   // [-4..4] for the same switch map we use below
        this.facingAngle = signed * (Math.PI / 4.0);
        switch (signed) {
            case 0  -> this.facing = Direction.RIGHT;
            case 1  -> this.facing = Direction.DOWN_RIGHT;
            case 2  -> this.facing = Direction.DOWN;
            case 3  -> this.facing = Direction.DOWN_LEFT;
            case -1 -> this.facing = Direction.UP_RIGHT;
            case -2 -> this.facing = Direction.UP;
            case -3 -> this.facing = Direction.UP_LEFT;
            default -> this.facing = Direction.LEFT; // ±4
        }
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

    public void increaseStaminaRegenByPercent(double pct) {
        staminaRegenRateMult *= (1.0 + pct);
    }

    private int effectiveStaminaRegenInterval() {
        double interval = STAMINA_REGEN_INTERVAL / Math.max(0.0001, staminaRegenRateMult);
        return Math.max(1, (int) Math.round(interval));
    }

    private double effectiveBaseSpeed() {
        return (PLAYER_SPEED_PPS / TICKS_PER_SECOND) * speedMultiplier;
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
                public boolean left()   { return is.left; }
                public boolean right()  { return is.right; }
                public boolean up()     { return is.up; }
                public boolean down()   { return is.down; }
                public boolean sprint() { return is.shift; }
                public boolean attack() { return is.attack; }
            };
        } else if (k0 instanceof KeyManager km) {
            input = new PlayerInputView() {
                public boolean left()   { return km.left; }
                public boolean right()  { return km.right; }
                public boolean up()     { return km.up; }
                public boolean down()   { return km.down; }
                public boolean sprint() { return km.shift; }
                public boolean attack() { return km.attack; }
            };
        } else {
            input = new PlayerInputView() {
                public boolean left()   { return false; }
                public boolean right()  { return false; }
                public boolean up()     { return false; }
                public boolean down()   { return false; }
                public boolean sprint() { return false; }
                public boolean attack() { return false; }
            };
        }

        TileMap map = (TileMap) args[1];
        @SuppressWarnings("unchecked") List<Enemy> enemies = (List<Enemy>) args[2];

        // Optional mouse-aim point in WORLD coords
        Point aimPoint = null;
        if (args.length >= 4 && args[3] instanceof Point p) aimPoint = p;

        // --- Input -> movement intent (WASD) ---
        int dxRaw = 0, dyRaw = 0;
        if (input.left())  dxRaw -= 1;
        if (input.right()) dxRaw += 1;
        if (input.up())    dyRaw -= 1;
        if (input.down())  dyRaw += 1;

        // --- Facing ---
        if (aimPoint != null) {
            double ax = aimPoint.x - x;
            double ay = aimPoint.y - y;
            if (Math.abs(ax) > 1e-6 || Math.abs(ay) > 1e-6) {
                double angle = Math.atan2(ay, ax); // -PI..PI
                facingAngle = angle;
                int oct = (int) Math.round(angle / (Math.PI / 4.0));
                switch (oct) {
                    case 0  -> facing = Direction.RIGHT;
                    case 1  -> facing = Direction.DOWN_RIGHT;
                    case 2  -> facing = Direction.DOWN;
                    case 3  -> facing = Direction.DOWN_LEFT;
                    case -1 -> facing = Direction.UP_RIGHT;
                    case -2 -> facing = Direction.UP;
                    case -3 -> facing = Direction.UP_LEFT;
                    default -> facing = Direction.LEFT; // 4 or -4
                }
            }
        } else if (dxRaw != 0 || dyRaw != 0) {
            double angle = Math.atan2(dyRaw, dxRaw);
            facingAngle = angle;
            int oct = (int) Math.round(angle / (Math.PI / 4.0));
            switch (oct) {
                case 0  -> facing = Direction.RIGHT;
                case 1  -> facing = Direction.DOWN_RIGHT;
                case 2  -> facing = Direction.DOWN;
                case 3  -> facing = Direction.DOWN_LEFT;
                case -1 -> facing = Direction.UP_RIGHT;
                case -2 -> facing = Direction.UP;
                case -3 -> facing = Direction.UP_LEFT;
                default -> facing = Direction.LEFT;
            }
        }

        // --- Movement (normalize diagonals) ---
        double dx = dxRaw, dy = dyRaw;
        if (dx != 0 && dy != 0) {
            double inv = Math.sqrt(0.5);
            dx *= inv; dy *= inv;
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
                if (++staminaRegenCounter >= effectiveStaminaRegenInterval()) {
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
            attackTimer = scaleFrom60(weapon.getDuration());
            attackCooldown = scaleFrom60(weapon.getCooldown());

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

    private static int scaleFrom60(int ticksAt60) {
        return (int) Math.round(ticksAt60 * (TICKS_PER_SECOND / 60.0));
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
        int r = weapon.getReach();
        int w = weapon.getWidth();

        double theta = facingAngle;
        double cos = Math.cos(theta), sin = Math.sin(theta);

        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);

        double halfW = width / 2.0;
        double halfH = height / 2.0;
        double edge = Math.hypot(halfW * cos, halfH * sin);
        double ax = cx + cos * edge;
        double ay = cy + sin * edge;

        double cxBlade = ax + cos * (r / 2.0);
        double cyBlade = ay + sin * (r / 2.0);

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

        // tiny facing line
        g2.setColor(new Color(10, 40, 15));
        int px = (int) Math.round(x) - camX;
        int py = (int) Math.round(y) - camY;
        int len = Math.max(width, height) / 2 + 6;
        int tx = px + (int) Math.round(Math.cos(facingAngle) * len);
        int ty = py + (int) Math.round(Math.sin(facingAngle) * len);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(px, py, tx, ty);
    }

    public void triggerLocalAttackFx() {
        this.attackTimer = scaleFrom60(weapon.getDuration());
        this.attackCooldown = scaleFrom60(weapon.getCooldown());
    }

    public void tickClientAnimations(boolean moving) {
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer   > 0) attackTimer--;
        this.movingThisTick = moving && (knockbackTimer == 0);
        this.animTimeMs += TICK_MS;
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
        int tileSize = BaseGameManager.TILE_SIZE;
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
