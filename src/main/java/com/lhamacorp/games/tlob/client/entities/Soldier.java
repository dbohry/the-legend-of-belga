package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Soldier extends Entity {

    private static final int SOLDIER_SIZE = 20;
    private static final double SOLDIER_BASE_SPEED = 2.5;

    private static final double SOLDIER_MAX_HP = 1.0;
    private static final double SOLDIER_MAX_STAMINA = 1.0;
    private static final double SOLDIER_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 30;

    // --- timing base (keep 30Hz for anim feel, independent from sim Hz) ---
    private static final int TICKS_PER_SECOND = 30;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    // defaults (per-enemy jitter is applied on top)
    private static final int ATTACK_COOLDOWN_TICKS = 60; // 2.0s @30Hz
    private static final int ATTACK_DURATION_TICKS = 3;  // ~0.1s
    private static final int POST_ATTACK_SLOWDOWN_TICKS = 60; // 2.0s @30Hz - speed reduction period

    private static final int HURT_FLASH_TICKS = 6;       // ~0.2s

    // state
    private int hurtTimer = 0;
    private int attackCooldown = 0;
    private int attackTimer = 0;
    private int postAttackSlowdownTimer = 0;

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

    public Soldier(double x, double y, Weapon weapon) {
        super(x, y, SOLDIER_SIZE, SOLDIER_SIZE, SOLDIER_BASE_SPEED, SOLDIER_MAX_HP, SOLDIER_MAX_STAMINA, SOLDIER_MAX_MANA, 0, weapon, "Soldier", Alignment.FOE);

        // seed LCG from position so it's deterministic per spawn
        int seed = (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) ^ 0x9E3779B9);
        lcg = (seed == 0) ? 1 : seed;

        // personalize
        speedScale = 0.50 + 0.30 * rand01(); // 0.90..1.20
        aimNoiseRad = Math.toRadians((rand01() - 0.5) * 14.0); // ~±7°
        boolean willStrafe = rand01() < 0.45; // ~45% strafe
        strafeStrength = willStrafe ? 0.20 + 0.25 * rand01() : 0.0; // 0.20..0.45
        strafeFreqHz = 0.6 + 0.8 * rand01(); // 0.6..1.4 Hz
        strafePhase = rand01() * Math.PI * 2.0;
        aggressionRadius = 220 + 140 * rand01(); // 220..360 px

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
        if (postAttackSlowdownTimer > 0) postAttackSlowdownTimer--;

        // Try melee attack
        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        // Stealth: if player stands on hiding tile (grass/plants), enemies cannot find them
        boolean playerHidden = map.isHidingAtWorld(player.getX(), player.getY());

        if (!playerHidden && distToP <= ATTACK_RANGE && attackCooldown == 0 && attackTimer == 0) {
            attackTimer = baseAttackDuration;
            attackCooldown = baseAttackCooldown;
            postAttackSlowdownTimer = POST_ATTACK_SLOWDOWN_TICKS;
            player.damage(1.0);
            player.applyKnockback(x, y);
        }

        // Movement logic
        if (attackTimer > 0) {
            // Attack in progress - stand still
            movedThisTick = false;
        } else if (!playerHidden && distToP <= aggressionRadius) {
            // Player in range - approach
            approachPlayer(player, map);
        } else {
            // Wander around
            wander(map);
        }

        // Update facing direction
        updateFacing();

        // Advance animation
        animTimeMs += TICK_MS;
        ageTicks++;
    }

    private void approachPlayer(Player player, TileMap map) {
        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        if (distToP > 0) {
            // Normalize direction
            double approachDx = dxToP / distToP;
            double approachDy = dyToP / distToP;

            // Add aim noise for more natural movement
            double noiseAngle = aimNoiseRad;
            double cos = Math.cos(noiseAngle), sin = Math.sin(noiseAngle);
            double noisyDx = approachDx * cos - approachDy * sin;
            double noisyDy = approachDx * sin + approachDy * cos;

            // Add strafing movement
            if (strafeStrength > 0) {
                double strafeAngle = strafePhase + ageTicks * strafeFreqHz * 2 * Math.PI / TICKS_PER_SECOND;
                double strafeDx = Math.cos(strafeAngle) * strafeStrength;
                double strafeDy = Math.sin(strafeAngle) * strafeStrength;
                noisyDx += strafeDx;
                noisyDy += strafeDy;
            }

            // Normalize and apply movement
            double totalDist = Math.hypot(noisyDx, noisyDy);
            if (totalDist > 0) {
                noisyDx /= totalDist;
                noisyDy /= totalDist;
                
                double moveSpeed = speed * speedScale;
                
                // Apply post-attack slowdown (half speed for 2 seconds)
                if (postAttackSlowdownTimer > 0) {
                    moveSpeed *= 0.5;
                }
                
                moveWithCollision(noisyDx * moveSpeed, noisyDy * moveSpeed, map);
                movedThisTick = true;
            }
        }
    }

    private void wander(TileMap map) {
        if (wanderTimer <= 0) {
            pickNewWanderDir();
            wanderTimer = 30 + (int) (rand01() * 60); // 1-3 seconds
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.3; // Slower when wandering
            
            // Apply post-attack slowdown (half speed for 2 seconds)
            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.5;
            }
            
            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map);
            movedThisTick = true;
        }
    }

    private void pickNewWanderDir() {
        double angle = rand01() * Math.PI * 2.0;
        wanderDx = Math.cos(angle);
        wanderDy = Math.sin(angle);
    }

    private void updateFacing() {
        if (movedThisTick) {
            // Determine facing based on movement direction
            if (Math.abs(wanderDx) > Math.abs(wanderDy)) {
                facing = wanderDx > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                facing = wanderDy > 0 ? Direction.DOWN : Direction.UP;
            }
        }
    }

    private void moveWithCollision(double dx, double dy, TileMap map) {
        double newX = x + dx;
        double newY = y + dy;

        if (!collidesWithMap(newX, y, map)) {
            x = newX;
        }
        if (!collidesWithMap(x, newY, map)) {
            y = newY;
        }
    }

    private double rand01() {
        lcg = lcg * 1103515245 + 12345;
        return (lcg >>> 16) / 65536.0;
    }

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!isAlive()) return;

        // Get enemy texture
        BufferedImage tex = TextureManager.getEnemyTexture();
        if (tex != null) {
            int px = (int) Math.round(x - width / 2.0) - camX;
            int py = (int) Math.round(y - height / 2.0) - camY;
            g2.drawImage(tex, px, py, null);
        } else {
            // Fallback: draw colored rectangle
            drawCenteredRect(g2, camX, camY, width, height, Color.RED);
        }

        // Draw hurt flash effect
        if (hurtTimer > 0) {
            int alpha = (int) (255 * (double) hurtTimer / HURT_FLASH_TICKS);
            g2.setColor(new Color(255, 255, 255, alpha));
            drawCenteredRect(g2, camX, camY, width, height, new Color(255, 255, 255, alpha));
        }

        // Draw attack indicator
        if (attackTimer > 0) {
            g2.setColor(new Color(255, 0, 0, 150));
            int indicatorSize = width + 10;
            drawCenteredRect(g2, camX, camY, indicatorSize, indicatorSize, new Color(255, 0, 0, 150));
        }

        // Draw slowdown indicator (blue tint when in post-attack slowdown)
        if (postAttackSlowdownTimer > 0) {
            g2.setColor(new Color(0, 0, 255, 80));
            drawCenteredRect(g2, camX, camY, width, height, new Color(0, 0, 255, 80));
        }
    }
}
