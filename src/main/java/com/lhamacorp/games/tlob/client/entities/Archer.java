package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Archer extends Entity {

    private static final int ARCHER_SIZE = 18;
    private static final double ARCHER_BASE_SPEED = 1.0;
    private static final double ARCHER_MAX_HP = 2.0;
    private static final double ARCHER_MAX_STAMINA = 1.0;
    private static final double ARCHER_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 120;
    private static final int MIN_ATTACK_RANGE = 60;
    private static final double ARROW_DAMAGE = 0.5;
    private static final int ATTACK_COOLDOWN_TICKS = 90;
    private static final int ATTACK_DURATION_TICKS = 8;
    private static final int ARROW_TRAVEL_TICKS = 30;

    private static final int TICKS_PER_SECOND = 30;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int HURT_FLASH_TICKS = 6;

    // state
    private int hurtTimer = 0;
    private int attackCooldown = 0;
    private int attackTimer = 0;
    private int arrowTimer = 0;

    private long animTimeMs = 0L;
    private boolean movedThisTick = false;

    // Arrow projectile state
    private double arrowX = 0;
    private double arrowY = 0;
    private double arrowTargetX = 0;
    private double arrowTargetY = 0;

    // wander
    private int wanderTimer = 0;
    private double wanderDx = 0, wanderDy = 0;

    private int lcg;
    private final double speedScale;
    private final double aimNoiseRad;
    private final double strafeStrength;
    private final double strafeFreqHz;
    private final double strafePhase;
    private final double aggressionRadius;
    private final int baseAttackCooldown;
    private final int baseAttackDuration;
    private int ageTicks = 0;

    /**
     * Creates an archer enemy at the specified position.
     */
    public Archer(double x, double y, Weapon weapon) {
        super(x, y, ARCHER_SIZE, ARCHER_SIZE, ARCHER_BASE_SPEED, ARCHER_MAX_HP, ARCHER_MAX_STAMINA, ARCHER_MAX_MANA, 0, weapon, "Archer", Alignment.FOE);

        int seed = (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) ^ 0x9E3779B9);
        lcg = (seed == 0) ? 1 : seed;

        speedScale = 0.50 + 0.30 * rand01();
        aimNoiseRad = Math.toRadians((rand01() - 0.5) * 14.0);
        boolean willStrafe = rand01() < 0.45;
        strafeStrength = willStrafe ? 0.20 + 0.25 * rand01() : 0.0;
        strafeFreqHz = 0.6 + 0.8 * rand01();
        strafePhase = rand01() * Math.PI * 2.0;
        aggressionRadius = 220 + 140 * rand01();

        baseAttackCooldown = (int) Math.round(ATTACK_COOLDOWN_TICKS * (0.85 + 0.5 * rand01()));
        baseAttackDuration = Math.max(2, ATTACK_DURATION_TICKS + (rand01() < 0.3 ? 1 : 0));

        pickNewWanderDir();
    }

    @Override
    public void update(Object... args) {
        if (!isAlive()) return;
        Player player = (Player) args[0];
        TileMap map = (TileMap) args[1];

        if (hurtTimer > 0) hurtTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        if (arrowTimer > 0) {
            arrowTimer--;
            
            double progress = 1.0 - (double) arrowTimer / ARROW_TRAVEL_TICKS;
            double currentArrowX = arrowX + (arrowTargetX - arrowX) * progress;
            double currentArrowY = arrowY + (arrowTargetY - arrowY) * progress;
            
            double arrowToPlayerDist = Math.hypot(currentArrowX - player.getX(), currentArrowY - player.getY());
            
            if (arrowToPlayerDist <= 25) {
                player.damage(ARROW_DAMAGE);
                player.applyKnockback(arrowX, arrowY);
                arrowTimer = 0;
            }
        }

        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        boolean playerHidden = map.isHidingAtWorld(player.getX(), player.getY());

        if (!playerHidden && distToP <= ATTACK_RANGE && distToP >= MIN_ATTACK_RANGE && 
            attackCooldown == 0 && attackTimer == 0 && arrowTimer == 0) {
            
            attackTimer = baseAttackDuration;
            attackCooldown = baseAttackCooldown;
            
            arrowX = x;
            arrowY = y;
            arrowTargetX = player.getX();
            arrowTargetY = player.getY();
            arrowTimer = ARROW_TRAVEL_TICKS;
        }

        if (attackTimer > 0) {
            movedThisTick = false;
        } else if (!playerHidden && distToP <= aggressionRadius) {
            if (distToP < MIN_ATTACK_RANGE) {
                backAwayFromPlayer(player, map);
            } else {
                approachPlayer(player, map);
            }
        } else {
            wander(map);
        }

        updateFacing();
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

            double noiseAngle = aimNoiseRad;
            double cos = Math.cos(noiseAngle), sin = Math.sin(noiseAngle);
            double noisyDx = approachDx * cos - approachDy * sin;
            double noisyDy = approachDx * sin + approachDy * cos;

            if (strafeStrength > 0) {
                double strafeAngle = strafePhase + ageTicks * strafeFreqHz * 2 * Math.PI / TICKS_PER_SECOND;
                double strafeDx = Math.cos(strafeAngle) * strafeStrength;
                double strafeDy = Math.sin(strafeAngle) * strafeStrength;
                noisyDx += strafeDx;
                noisyDy += strafeDy;
            }

            double totalDist = Math.hypot(noisyDx, noisyDy);
            if (totalDist > 0) {
                noisyDx /= totalDist;
                noisyDy /= totalDist;
                
                double moveSpeed = speed * speedScale * 0.5;
                moveWithCollision(noisyDx * moveSpeed, noisyDy * moveSpeed, map);
                movedThisTick = true;
            }
        }
    }

    private void backAwayFromPlayer(Player player, TileMap map) {
        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        if (distToP > 0) {
            double awayDx = -dxToP / distToP;
            double awayDy = -dyToP / distToP;

            if (strafeStrength > 0) {
                double strafeAngle = strafePhase + ageTicks * strafeFreqHz * 2 * Math.PI / TICKS_PER_SECOND;
                double strafeDx = Math.cos(strafeAngle) * strafeStrength;
                double strafeDy = Math.sin(strafeAngle) * strafeStrength;
                awayDx += strafeDx;
                awayDy += strafeDy;
            }

            double totalDist = Math.hypot(awayDx, awayDy);
            if (totalDist > 0) {
                awayDx /= totalDist;
                awayDy /= totalDist;
                
                double moveSpeed = speed * speedScale * 0.7;
                moveWithCollision(awayDx * moveSpeed, awayDy * moveSpeed, map);
                movedThisTick = true;
            }
        }
    }

    private void wander(TileMap map) {
        if (wanderTimer <= 0) {
            pickNewWanderDir();
            wanderTimer = 30 + (int) (rand01() * 60);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.3;
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

        BufferedImage tex = TextureManager.getEnemyTexture();
        if (tex != null) {
            int px = (int) Math.round(x - width / 2.0) - camX;
            int py = (int) Math.round(y - height / 2.0) - camY;
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            g2.drawImage(tex, px, py, null);
            
            g2.setColor(new Color(0, 200, 0, 80));
            g2.fillRect(px, py, width, height);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else {
            drawCenteredRect(g2, camX, camY, width, height, Color.GREEN);
        }

        if (hurtTimer > 0) {
            int alpha = (int) (255 * (double) hurtTimer / HURT_FLASH_TICKS);
            g2.setColor(new Color(255, 255, 255, alpha));
            drawCenteredRect(g2, camX, camY, width, height, new Color(255, 255, 255, alpha));
        }

        if (attackTimer > 0) {
            g2.setColor(new Color(255, 255, 0, 150));
            int indicatorSize = width + 10;
            drawCenteredRect(g2, camX, camY, indicatorSize, indicatorSize, new Color(255, 255, 0, 150));
        }

        if (arrowTimer > 0) {
            double progress = 1.0 - (double) arrowTimer / ARROW_TRAVEL_TICKS;
            double currentArrowX = arrowX + (arrowTargetX - arrowX) * progress;
            double currentArrowY = arrowY + (arrowTargetY - arrowY) * progress;
            
            int arrowScreenX = (int) Math.round(currentArrowX) - camX;
            int arrowScreenY = (int) Math.round(currentArrowY) - camY;
            
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2f));
            int arrowSize = 5;
            g2.drawLine(arrowScreenX - arrowSize, arrowScreenY, arrowScreenX + arrowSize, arrowScreenY);
            g2.drawLine(arrowScreenX, arrowScreenY - arrowSize, arrowScreenX, arrowScreenY + arrowSize);
        }
    }
}
