package com.lhamacorp.games.tlob.entities;

import com.lhamacorp.games.tlob.managers.TextureManager;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Enemy extends Entity {

    private static final int ENEMY_SIZE = 20;
    private static final double ENEMY_BASE_SPEED = 3.5;

    private static final double ENEMY_MAX_HP = 1.0;
    private static final double ENEMY_MAX_STAMINA = 1.0;
    private static final double ENEMY_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 30;

    // --- timing base (keep 30Hz for anim feel, independent from sim Hz) ---
    private static final int TICKS_PER_SECOND = 30;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    // defaults (per-enemy jitter is applied on top)
    private static final int ATTACK_COOLDOWN_TICKS = 60; // 2.0s @30Hz
    private static final int ATTACK_DURATION_TICKS = 3;  // ~0.1s

    private static final int HURT_FLASH_TICKS = 6;       // ~0.2s

    // state
    private int hurtTimer = 0;
    private int attackCooldown = 0;
    private int attackTimer = 0;

    private long animTimeMs = 0L;
    private boolean movedThisTick = false;

    // wander
    private int wanderTimer = 0;
    private double wanderDx = 0, wanderDy = 0;

    // per-enemy RNG (tiny LCG) + personalization
    private int lcg;
    private final double speedScale;        // ~0.9 .. 1.2
    private final double aimNoiseRad;       // small fixed offset on approach
    private final double strafeStrength;    // 0 (most) or up to ~0.4
    private final double strafeFreqHz;      // ~0.6 .. 1.4 Hz
    private final double strafePhase;       // 0..2π
    private final double aggressionRadius;  // when to stop wandering and engage
    private final int baseAttackCooldown;   // +/- jittered cooldown
    private final int baseAttackDuration;   // usually 3, can jitter small
    private int ageTicks = 0;

    public Enemy(double x, double y, Weapon weapon) {
        super(x, y, ENEMY_SIZE, ENEMY_SIZE, ENEMY_BASE_SPEED, ENEMY_MAX_HP, ENEMY_MAX_STAMINA, ENEMY_MAX_MANA, 0, weapon, "Zombie");

        // seed LCG from position so it’s deterministic per spawn
        int seed = (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) ^ 0x9E3779B9);
        lcg = (seed == 0) ? 1 : seed;

        // personalize
        speedScale       = 0.90 + 0.30 * rand01();                    // 0.90..1.20
        aimNoiseRad      = Math.toRadians((rand01() - 0.5) * 14.0);   // ~±7°
        boolean willStrafe = rand01() < 0.45;                          // ~45% strafe
        strafeStrength   = willStrafe ? 0.20 + 0.25 * rand01() : 0.0; // 0.20..0.45
        strafeFreqHz     = 0.6 + 0.8 * rand01();                      // 0.6..1.4 Hz
        strafePhase      = rand01() * Math.PI * 2.0;
        aggressionRadius = 220 + 140 * rand01();                      // 220..360 px

        baseAttackCooldown = (int) Math.round(ATTACK_COOLDOWN_TICKS * (0.85 + 0.5 * rand01())); // 0.85x..1.35x
        baseAttackDuration = Math.max(2, ATTACK_DURATION_TICKS + (rand01() < 0.3 ? 1 : 0));     // 3 or sometimes 4

        pickNewWanderDir(); // initial wander vector
    }

    @Override
    public void update(Object... args) {
        if (!isAlive()) return;
        Player player = (Player) args[0];
        TileMap map = (TileMap) args[1];

        if (hurtTimer > 0) hurtTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        // Try melee attack
        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        if (distToP <= ATTACK_RANGE && attackCooldown == 0 && attackTimer == 0) {
            attackTimer = baseAttackDuration;
            attackCooldown = baseAttackCooldown;
            player.damage(1.0);
            player.applyKnockback(x, y);
        }

        // Desired direction (unit) toward player
        double dirX = 0, dirY = 0;
        if (distToP > 1e-6) {
            dirX = dxToP / distToP;
            dirY = dyToP / distToP;
        }

        // knockback first
        updateKnockbackWithMap(map);

        double vx = 0, vy = 0;
        if (knockbackTimer == 0) {
            double desiredSpeed = speed * speedScale;

            // choose wander vs engage
            boolean wander = (distToP > aggressionRadius) || (attackTimer > 0);
            if (wander) {
                if (--wanderTimer <= 0) pickNewWanderDir();
                dirX = wanderDx;
                dirY = wanderDy;
                desiredSpeed = 0.6 * speedScale;
            } else {
                // slight aim noise (fixed per enemy)
                if (Math.abs(aimNoiseRad) > 1e-6) {
                    double s = Math.sin(aimNoiseRad), c = Math.cos(aimNoiseRad);
                    double rx = dirX * c - dirY * s;
                    double ry = dirX * s + dirY * c;
                    dirX = rx; dirY = ry;
                }
                // optional strafing: add small perpendicular oscillation
                if (strafeStrength > 0.0) {
                    double phase = strafePhase + (ageTicks * 2.0 * Math.PI * strafeFreqHz) / TICKS_PER_SECOND;
                    double osc = Math.sin(phase) * strafeStrength;
                    double px = -dirY, py = dirX; // perp
                    dirX += osc * px;
                    dirY += osc * py;
                    double len = Math.hypot(dirX, dirY);
                    if (len > 1e-6) { dirX /= len; dirY /= len; }
                }
            }

            vx = dirX * desiredSpeed;
            vy = dirY * desiredSpeed;

            moveWithCollision(vx, vy, map, player);
        }

        // facing from actual velocity
        updateFacing(vx, vy);
        movedThisTick = (Math.abs(vx) + Math.abs(vy)) > 1e-3 && knockbackTimer == 0;

        animTimeMs += TICK_MS;
        ageTicks++;
    }

    // ---- tiny LCG helpers ----
    private int nextRand() {
        lcg = lcg * 1664525 + 1013904223;
        return lcg;
    }
    private double rand01() {
        // 24-bit mantissa -> [0,1)
        return ((nextRand() >>> 8) & 0xFFFFFF) / (double) (1 << 24);
    }

    private void pickNewWanderDir() {
        // next change 0.5–1.2s @30Hz
        int span = 15 + (int) Math.floor(rand01() * 21.0); // 15..35 ticks
        wanderTimer = span;

        double ang = rand01() * Math.PI * 2.0;
        wanderDx = Math.cos(ang);
        wanderDy = Math.sin(ang);
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

            if (hurtTimer > 0 || attackTimer > 0) {
                Color overlay = (hurtTimer > 0) ? new Color(255, 120, 120, 100)
                    : new Color(255, 200, 60, 100);
                g2.setColor(overlay);
                g2.fillRect(px, py, width, height);
            }
        } else {
            Color c = (hurtTimer > 0) ? new Color(255, 120, 120)
                : (attackTimer > 0) ? new Color(255, 200, 60)
                : new Color(200, 60, 60);
            drawCenteredRect(g2, camX, camY, width, height, c);
        }
    }

    private void updateFacing(double vx, double vy) {
        if (Math.abs(vx) < 1e-3 && Math.abs(vy) < 1e-3) return;
        if (Math.abs(vx) > Math.abs(vy)) {
            facing = (vx < 0) ? Direction.LEFT : Direction.RIGHT;
        } else {
            facing = (vy < 0) ? Direction.UP : Direction.DOWN;
        }
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
