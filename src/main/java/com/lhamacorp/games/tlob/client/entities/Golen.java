package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.GameConfig;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Golen extends Entity {

    private static final int GOLEN_SIZE = 80;
    private static final double GOLEN_BASE_SPEED = 1.0;

    private static final double GOLEN_MAX_HP = 20.0;
    private static final double GOLEN_MAX_STAMINA = 2.0;
    private static final double GOLEN_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 60;
    private static final double ATTACK_DAMAGE = 10.0;

    // --- timing base (keep 30Hz for anim feel, independent from sim Hz) ---
    private static final int TICKS_PER_SECOND = 30;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int ATTACK_COOLDOWN_TICKS = 180; // 3x slower than Soldier
    private static final int ATTACK_DURATION_TICKS = 15; // Much longer attack animation
    private static final int POST_ATTACK_SLOWDOWN_TICKS = 120; // Much longer recovery
    private static final int HURT_FLASH_TICKS = 12; // Longer hurt flash
    private static final int CHARGE_UP_TICKS = 25; // Longer charge-up for dramatic effect
    private static final int STOMP_COOLDOWN_TICKS = 300; // New: ground stomp ability
    private static final int STOMP_DURATION_TICKS = 20; // Stomp animation duration
    private static final int RAGE_MODE_THRESHOLD = 2; // HP threshold to enter rage mode

    // state
    private int hurtTimer = 0;
    private int attackCooldown = 0;
    private int attackTimer = 0;
    private int postAttackSlowdownTimer = 0;
    private int chargeUpTimer = 0;

    // New unique abilities
    private int stompCooldown = 0;
    private int stompTimer = 0;
    private boolean isRageMode = false;
    private int rageModeTimer = 0;
    private static final int RAGE_MODE_DURATION = 600; // 20 seconds at 30Hz
    private boolean hasStompedThisLevel = false;

    private long animTimeMs = 0L;
    private boolean movedThisTick = false;

    // wander
    private int wanderTimer = 0;
    private double wanderDx = 0, wanderDy = 0;

    // Enhanced wandering behavior
    private final WanderBehavior wanderBehavior;
    private int patrolTimer = 0;
    private int patrolPointIndex = 0;
    private final double[] patrolPointsX = new double[3];
    private final double[] patrolPointsY = new double[3];
    private final double wanderSpeedVariation;
    private final double wanderDirectionChangeChance;
    private final boolean prefersStraightPaths;
    private final double idleTimeVariation;
    private final boolean isAggressive;
    private final boolean isCautious;
    private final double curiosityLevel;
    private final boolean prefersGroupMovement;

    private int lcg;
    private final double baseSpeedScale; // Base speed scale (final)
    private double speedScale; // Current speed scale (modifiable for rage mode)
    private final double aimNoiseRad;
    private final double strafeStrength;
    private final double strafeFreqHz;
    private final double strafePhase;
    private double aggressionRadius; // Made non-final for rage mode modifications
    private final int baseAttackCooldown;
    private final int baseAttackDuration;
    private int ageTicks = 0;

    // Wander behavior types
    private enum WanderBehavior {
        RANDOM,      // Completely random movement
        PATROL,      // Move between defined patrol points
        CIRCULAR,    // Move in circular patterns
        LINEAR,      // Prefer straight line movement
        IDLE         // Stay mostly still with occasional movement
    }

    /**
     * Creates a Golen enemy at the specified position.
     */
    public Golen(double x, double y, Weapon weapon) {
        super(x, y, GOLEN_SIZE, GOLEN_SIZE, GOLEN_BASE_SPEED, GOLEN_MAX_HP, GOLEN_MAX_STAMINA, GOLEN_MAX_MANA, 0, 0, weapon, "Golen", Alignment.FOE);

        // Improved seed generation with more entropy
        int seed = (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) ^ 0x9E3779B9);
        lcg = (seed == 0) ? 1 : seed;

        // Add additional entropy based on current time and object hash
        lcg ^= System.nanoTime() & 0xFFFF;
        lcg ^= this.hashCode() & 0xFFFF;

        baseSpeedScale = 0.30 + 0.20 * rand01(); // Golen are slower
        speedScale = baseSpeedScale; // Initialize current speed scale
        aimNoiseRad = Math.toRadians((rand01() - 0.5) * 8.0); // Less aim noise due to size
        boolean willStrafe = rand01() < 0.25; // Less likely to strafe
        strafeStrength = willStrafe ? 0.15 + 0.20 * rand01() : 0.0;
        strafeFreqHz = 0.4 + 0.6 * rand01(); // Slower strafing
        strafePhase = rand01() * Math.PI * 2.0;
        aggressionRadius = 280 + 160 * rand01(); // Larger aggression radius

        baseAttackCooldown = (int) Math.round(ATTACK_COOLDOWN_TICKS * (0.9 + 0.4 * rand01()));
        baseAttackDuration = Math.max(6, ATTACK_DURATION_TICKS + (rand01() < 0.3 ? 2 : 0));

        // Initialize enhanced wandering behavior
        double behaviorRoll = rand01();
        if (behaviorRoll < 0.3) {
            wanderBehavior = WanderBehavior.IDLE; // Golen prefer to stay still
        } else if (behaviorRoll < 0.5) {
            wanderBehavior = WanderBehavior.PATROL;
        } else if (behaviorRoll < 0.7) {
            wanderBehavior = WanderBehavior.CIRCULAR;
        } else if (behaviorRoll < 0.9) {
            wanderBehavior = WanderBehavior.LINEAR;
        } else {
            wanderBehavior = WanderBehavior.RANDOM;
        }
        wanderSpeedVariation = 0.2 + 0.3 * rand01(); // Less variation
        wanderDirectionChangeChance = 0.05 + 0.15 * rand01(); // Less frequent changes
        prefersStraightPaths = rand01() < 0.7; // Prefer straight paths
        idleTimeVariation = 0.8 + rand01() * 1.2; // Longer idle times

        // Initialize personality traits - Golen are more cautious and less aggressive
        isAggressive = rand01() < 0.25; // Less aggressive than Soldier (0.4)
        isCautious = rand01() < 0.6; // More cautious than Soldier (0.3)
        curiosityLevel = 0.3 + 0.4 * rand01(); // Lower curiosity
        prefersGroupMovement = rand01() < 0.3; // Less likely to group

        // Set up patrol points if using patrol behavior
        if (wanderBehavior == WanderBehavior.PATROL) {
            setupPatrolPoints();
        }

        pickNewWanderDir();
    }

    @Override
    public void update(Object... args) {
        if (!isAlive()) return;
        Player player = (Player) args[0];
        TileMap map = (TileMap) args[1];

        // Check for group behavior if enemies list is provided
        List<Entity> nearbyAllies = null;
        if (args.length > 2 && args[2] instanceof List) {
            @SuppressWarnings("unchecked")
            List<Entity> enemies = (List<Entity>) args[2];
            nearbyAllies = findNearbyAllies(enemies);
        }

        if (hurtTimer > 0) hurtTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;
        if (postAttackSlowdownTimer > 0) postAttackSlowdownTimer--;
        if (chargeUpTimer > 0) chargeUpTimer--;
        if (stompCooldown > 0) stompCooldown--;
        if (stompTimer > 0) stompTimer--;
        if (rageModeTimer > 0) rageModeTimer--;

        // Check for rage mode activation
        if (!isRageMode && health <= RAGE_MODE_THRESHOLD) {
            enterRageMode();
        }

        // Exit rage mode when timer expires
        if (isRageMode && rageModeTimer <= 0) {
            exitRageMode();
        }

        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        boolean playerHidden = map.isHidingAtWorld(player.getX(), player.getY());

        // Golen unique attack logic with multiple attack types
        if (!playerHidden && distToP <= ATTACK_RANGE && attackCooldown == 0 && attackTimer == 0 && chargeUpTimer == 0) {
            // Decide attack type based on distance and rage mode
            if (distToP <= 40 && !hasStompedThisLevel && stompCooldown == 0) {
                // Ground stomp attack (close range, area effect)
                stompTimer = STOMP_DURATION_TICKS;
                stompCooldown = STOMP_COOLDOWN_TICKS;
                hasStompedThisLevel = true;
            } else {
                // Regular charge-up attack
                chargeUpTimer = CHARGE_UP_TICKS;
            }
        }

        if (chargeUpTimer == 1) { // End of charge-up, start regular attack
            attackTimer = baseAttackDuration;
            attackCooldown = baseAttackCooldown;
            postAttackSlowdownTimer = POST_ATTACK_SLOWDOWN_TICKS;
            player.damage(ATTACK_DAMAGE);
            player.applyKnockback(x, y);
        }

        if (stompTimer == 1) { // End of stomp, apply area damage
            // Area damage to all nearby entities (including player)
            applyStompDamage(player);
            stompTimer = 0;
        }

        if (attackTimer > 0 || chargeUpTimer > 0) {
            movedThisTick = false;
        } else if (!playerHidden && distToP <= aggressionRadius) {
            approachPlayer(player, map);
        } else {
            // Use group behavior if allies are nearby
            if (nearbyAllies != null && !nearbyAllies.isEmpty() && prefersGroupMovement) {
                groupWander(map, player, nearbyAllies);
            } else {
                enhancedWander(map, player);
            }
        }

        updateFacing();
        animTimeMs += TICK_MS;
        ageTicks++;
    }

    private List<Entity> findNearbyAllies(List<Entity> enemies) {
        List<Entity> allies = new ArrayList<>();
        double groupRadius = 80.0; // Larger group radius due to size

        for (Entity enemy : enemies) {
            if (enemy != this && enemy.isAlive() && enemy.getAlignment() == Alignment.FOE) {
                double dist = Math.hypot(enemy.getX() - x, enemy.getY() - y);
                if (dist <= groupRadius) {
                    allies.add(enemy);
                }
            }
        }
        return allies;
    }

    private void groupWander(TileMap map, Player player, List<Entity> allies) {
        if (wanderTimer <= 0) {
            // Group behavior: move towards the center of nearby allies
            if (!allies.isEmpty()) {
                double centerX = 0, centerY = 0;
                for (Entity ally : allies) {
                    centerX += ally.getX();
                    centerY += ally.getY();
                }
                centerX /= allies.size();
                centerY /= allies.size();

                // Move towards the group center
                double dx = centerX - x;
                double dy = centerY - y;
                double dist = Math.hypot(dx, dy);

                if (dist > 0) {
                    wanderDx = dx / dist;
                    wanderDy = dy / dist;
                    wanderTimer = 60 + (int) (rand01() * 120); // Longer group movement
                } else {
                    pickNewWanderDir();
                    wanderTimer = 45 + (int) (rand01() * 90);
                }
            } else {
                pickNewWanderDir();
                wanderTimer = 45 + (int) (rand01() * 90);
            }
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.15; // Slower group movement

            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.3; // More slowdown after attack
            }

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
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

            double totalDist = Math.hypot(noisyDx, noisyDy);
            if (totalDist > 0) {
                noisyDx /= totalDist;
                noisyDy /= totalDist;

                double moveSpeed = speed * speedScale;

                if (postAttackSlowdownTimer > 0) {
                    moveSpeed *= 0.3; // More slowdown after attack
                }

                moveWithCollision(noisyDx * moveSpeed, noisyDy * moveSpeed, map, player);
                movedThisTick = true;
            }
        }
    }

    private void setupPatrolPoints() {
        // Create patrol points around the initial position - larger radius due to size
        double radius = 60 + 30 * rand01();
        for (int i = 0; i < 3; i++) {
            double angle = (i * 2 * Math.PI / 3) + rand01() * Math.PI / 4;
            patrolPointsX[i] = x + Math.cos(angle) * radius;
            patrolPointsY[i] = y + Math.sin(angle) * radius;
        }
    }

    private void enhancedWander(TileMap map, Player player) {
        switch (wanderBehavior) {
            case RANDOM:
                randomWander(map, player);
                break;
            case PATROL:
                patrolWander(map, player);
                break;
            case CIRCULAR:
                circularWander(map, player);
                break;
            case LINEAR:
                linearWander(map, player);
                break;
            case IDLE:
                idleWander(map, player);
                break;
        }
    }

    private void randomWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            pickNewWanderDir();
            // Golen change direction less frequently
            int baseTime = isAggressive ? 30 : 45;
            wanderTimer = (int) (baseTime + rand01() * 90 * idleTimeVariation);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * (0.15 + wanderSpeedVariation);

            // Golen are much slower when wandering
            moveSpeed *= 0.4;

            // Aggressive Golen move slightly faster
            if (isAggressive) moveSpeed *= 1.1;
            // Cautious Golen move even slower
            if (isCautious) moveSpeed *= 0.7;

            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.3;
            }

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }

    private void patrolWander(TileMap map, Player player) {
        if (patrolTimer <= 0) {
            // Move to next patrol point
            double targetX = patrolPointsX[patrolPointIndex];
            double targetY = patrolPointsY[patrolPointIndex];

            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.hypot(dx, dy);

            if (dist < 15) { // Larger tolerance due to size
                // Reached patrol point, move to next
                patrolPointIndex = (patrolPointIndex + 1) % 3;
                // Golen patrol slower
                int baseTime = isAggressive ? 60 : 90;
                patrolTimer = baseTime + (int) (rand01() * 180);
            } else {
                // Move towards patrol point
                wanderDx = dx / dist;
                wanderDy = dy / dist;
                double moveSpeed = speed * speedScale * 0.25;

                // Aggressive Golen move slightly faster during patrol
                if (isAggressive) moveSpeed *= 1.05;

                if (postAttackSlowdownTimer > 0) {
                    moveSpeed *= 0.3;
                }

                moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
                movedThisTick = true;
            }
        } else {
            patrolTimer--;
        }
    }

    private void circularWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            // Change direction in a more circular pattern
            double currentAngle = Math.atan2(wanderDy, wanderDx);
            // Curious Golen make smaller direction changes
            double maxAngleChange = Math.PI / 3 * (0.3 + curiosityLevel * 0.4);
            double angleChange = (rand01() - 0.5) * maxAngleChange;
            double newAngle = currentAngle + angleChange;

            wanderDx = Math.cos(newAngle);
            wanderDy = Math.sin(newAngle);
            wanderTimer = 60 + (int) (rand01() * 120);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.2;

            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.3;
            }

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }

    private void linearWander(TileMap map, Player player) {
        if (wanderTimer <= 0 || (prefersStraightPaths && rand01() < wanderDirectionChangeChance)) {
            pickNewWanderDir();
            // Cautious Golen prefer much longer straight paths
            int baseTime = isCautious ? 150 : 90;
            wanderTimer = baseTime + (int) (rand01() * 180);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.25;

            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.3;
            }

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }

    private void idleWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            // Golen are much more likely to stay idle
            double moveChance = isAggressive ? 0.3 : 0.15;
            if (rand01() < moveChance) {
                pickNewWanderDir();
                wanderTimer = 30 + (int) (rand01() * 60);
            } else {
                // Golen stay idle much longer
                int baseTime = isCautious ? 240 : 180;
                wanderTimer = baseTime + (int) (rand01() * 240 * idleTimeVariation);
            }
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            if (wanderTimer > 120) { // Longer idle period
                double moveSpeed = speed * speedScale * 0.1;

                if (postAttackSlowdownTimer > 0) {
                    moveSpeed *= 0.3;
                }

                moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
                movedThisTick = true;
            }
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

    private void moveWithCollision(double dx, double dy, TileMap map, Player player) {
        double newX = x + dx;
        double newY = y + dy;

        // Check map collision for X movement
        if (!collidesWithMap(newX, y, map) && !collidesWithPlayer(newX, y, player)) {
            x = newX;
        }

        // Check map collision for Y movement
        if (!collidesWithMap(x, newY, map) && !collidesWithPlayer(x, newY, player)) {
            y = newY;
        }
    }

    private double rand01() {
        lcg = lcg * 1103515245 + 12345;
        return (lcg >>> 16) / 65536.0;
    }

    /**
     * Enters rage mode when health drops below threshold.
     * Rage mode increases speed, damage, and aggression.
     */
    private void enterRageMode() {
        isRageMode = true;
        rageModeTimer = RAGE_MODE_DURATION;
        // Rage mode effects: increased speed and aggression
        speedScale = baseSpeedScale * 3;
        aggressionRadius *= 1.5;
    }

    /**
     * Exits rage mode when timer expires.
     */
    private void exitRageMode() {
        isRageMode = false;
        rageModeTimer = 0;
        // Reset to normal values
        speedScale = baseSpeedScale;
        aggressionRadius /= 1.3;
    }

    /**
     * Applies area damage from ground stomp attack.
     * Damages all entities within stomp radius.
     */
    private void applyStompDamage(Player player) {
        // Area damage effect - this would need to be implemented with entity lists
        // For now, just damage the player if they're close
        double stompRadius = 50.0; // Stomp affects area around Golen
        double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);

        if (distToPlayer <= stompRadius) {
            // Stomp does more damage than regular attack
            double stompDamage = ATTACK_DAMAGE * 1.5;
            player.damage(stompDamage);
            player.applyKnockback(x, y);
        }
    }

    /**
     * Resets stomp ability when entering a new level.
     */
    public void resetStompAbility() {
        hasStompedThisLevel = false;
    }

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!isAlive()) return;

        // Golen always uses custom drawing instead of texture
        drawGolenEnemy(g2, camX, camY);

        if (hurtTimer > 0) {
            int alpha = (int) (255 * (double) hurtTimer / HURT_FLASH_TICKS);
            g2.setColor(new Color(255, 255, 255, alpha));
            drawCenteredRect(g2, camX, camY, width, height, new Color(255, 255, 255, alpha));
        }

        if (chargeUpTimer > 0) {
            // Draw charge-up indicator
            int chargeAlpha = Math.min(255, 150 + (chargeUpTimer * 5)); // Clamp alpha to 0-255
            g2.setColor(new Color(255, 165, 0, chargeAlpha)); // Orange glow
            int indicatorSize = width + 20;
            drawCenteredRect(g2, camX, camY, indicatorSize, indicatorSize, new Color(255, 165, 0, chargeAlpha));
        }

        if (stompTimer > 0) {
            // Draw stomp indicator - red pulsing effect
            int stompAlpha = Math.min(255, 150 + (stompTimer * 5)); // Clamp alpha to 0-255
            g2.setColor(new Color(255, 0, 0, stompAlpha));
            int stompSize = width + 30;
            drawCenteredRect(g2, camX, camY, stompSize, stompSize, new Color(255, 0, 0, stompAlpha));

            // Draw stomp shockwave effect
            g2.setColor(new Color(255, 100, 0, 100));
            int shockwaveSize = width + 40 + (stompTimer * 2);
            drawCenteredRect(g2, camX, camY, shockwaveSize, shockwaveSize, new Color(255, 100, 0, 100));
        }

        if (isRageMode) {
            // Draw rage mode indicator - intense red aura
            int rageAlpha = Math.min(255, 100 + (rageModeTimer % 30) * 3); // Clamp alpha to 0-255
            g2.setColor(new Color(255, 0, 0, rageAlpha));
            int rageSize = width + 25;
            drawCenteredRect(g2, camX, camY, rageSize, rageSize, new Color(255, 0, 0, rageAlpha));

            // Draw rage mode text
            g2.setColor(new Color(255, 0, 0, 200));
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            String rageText = "RAGE";
            int textX = (int) Math.round(x) - camX - fm.stringWidth(rageText) / 2;
            int textY = (int) Math.round(y) - camY - height / 2 - 35;
            g2.drawString(rageText, textX, textY);
        }

        if (attackTimer > 0) {
            g2.setColor(new Color(255, 0, 0, 150));
            int indicatorSize = width + 15;
            drawCenteredRect(g2, camX, camY, indicatorSize, indicatorSize, new Color(255, 0, 0, 150));
        }

        if (postAttackSlowdownTimer > 0) {
            g2.setColor(new Color(0, 0, 255, 80));
            drawCenteredRect(g2, camX, camY, width, height, new Color(0, 0, 255, 80));
        }

        // Draw wandering behavior indicator
        if (GameConfig.getInstance().isShowEnemyBehaviorIndicators() && (wanderTimer > 0 || patrolTimer > 0)) {
            String behaviorText = wanderBehavior.name().substring(0, 1);
            g2.setColor(new Color(0, 255, 0, 200));
            g2.setFont(new Font("Arial", Font.BOLD, 14)); // Larger font for Golen
            FontMetrics fm = g2.getFontMetrics();
            int textX = (int) Math.round(x) - camX - fm.stringWidth(behaviorText) / 2;
            int textY = (int) Math.round(y) - camY - height / 2 - 20;
            g2.drawString(behaviorText, textX, textY);

            // Draw personality indicators
            int indicatorY = textY - 15;
            if (isAggressive) {
                g2.setColor(new Color(255, 0, 0, 150));
                g2.fillOval((int) Math.round(x) - camX - 4, indicatorY - 4, 8, 8);
            }
            if (isCautious) {
                g2.setColor(new Color(0, 0, 255, 150));
                g2.fillRect((int) Math.round(x) - camX - 4, indicatorY - 4, 8, 8);
            }
            if (prefersGroupMovement) {
                g2.setColor(new Color(255, 255, 0, 150));
                g2.fillPolygon(
                    new int[]{(int) Math.round(x) - camX - 4, (int) Math.round(x) - camX + 4, (int) Math.round(x) - camX},
                    new int[]{indicatorY + 4, indicatorY + 4, indicatorY - 4}, 3);
            }
        }
        
        // Draw perk count indicator
        if (GameConfig.getInstance().isShowEnemyBehaviorIndicators() && getPerkCount() > 0) {
            String perkText = String.valueOf(getPerkCount());
            g2.setColor(new Color(255, 215, 0, 220)); // Gold color for perks
            g2.setFont(new Font("Arial", Font.BOLD, 14)); // Same size as behavior indicator
            FontMetrics fm = g2.getFontMetrics();
            int textX = (int) Math.round(x) - camX + 10; // To the right of behavior indicator
            int textY = (int) Math.round(y) - camY - height / 2 - 20; // Same Y as behavior indicator
            g2.drawString(perkText, textX, textY);
        }
    }

    private void drawGolenEnemy(Graphics2D g2, int camX, int camY) {
        int centerX = (int) Math.round(x) - camX;
        int centerY = (int) Math.round(y) - camY;

        // Enable anti-aliasing for smoother shapes
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(centerX - width / 2 + 4, centerY - height / 2 + height - 12, width - 8, 8);

        // Unique Golen body shape - hexagonal/octagonal stone structure
        int[] bodyX = {
            centerX - width / 2 + 8,    // Left edge
            centerX - width / 2 + 4,    // Left curve
            centerX - width / 2,        // Left point
            centerX - width / 2 + 4,    // Left curve
            centerX - width / 2 + 8,    // Left edge
            centerX + width / 2 - 8,    // Right edge
            centerX + width / 2 - 4,    // Right curve
            centerX + width / 2,        // Right point
            centerX + width / 2 - 4,    // Right curve
            centerX + width / 2 - 8     // Right edge
        };
        int[] bodyY = {
            centerY - height / 2 + 12,  // Top left
            centerY - height / 2 + 8,   // Top curve
            centerY - height / 2 + 4,   // Top point
            centerY - height / 2,       // Top curve
            centerY - height / 2 + 4,   // Top edge
            centerY - height / 2 + 4,   // Top edge
            centerY - height / 2,       // Top curve
            centerY - height / 2 + 4,   // Top point
            centerY - height / 2 + 8,   // Top curve
            centerY - height / 2 + 12   // Top right
        };

        // Main body (dark stone)
        g2.setColor(new Color(80, 80, 90));
        g2.fillPolygon(bodyX, bodyY, bodyX.length);

        // Body highlights (lighter stone)
        g2.setColor(new Color(120, 120, 130));
        int[] highlightX = {
            centerX - width / 2 + 10,
            centerX - width / 2 + 6,
            centerX - width / 2 + 2,
            centerX - width / 2 + 6,
            centerX - width / 2 + 10,
            centerX + width / 2 - 10,
            centerX + width / 2 - 6,
            centerX + width / 2 - 2,
            centerX + width / 2 - 6,
            centerX + width / 2 - 10
        };
        int[] highlightY = {
            centerY - height / 2 + 14,
            centerY - height / 2 + 10,
            centerY - height / 2 + 6,
            centerY - height / 2 + 2,
            centerY - height / 2 + 6,
            centerY - height / 2 + 6,
            centerY - height / 2 + 2,
            centerY - height / 2 + 6,
            centerY - height / 2 + 10,
            centerY - height / 2 + 14
        };
        g2.fillPolygon(highlightX, highlightY, highlightX.length);

        // Unique head shape - triangular/arrowhead design
        int[] headX = {
            centerX,                   // Top point
            centerX - 16,              // Left base
            centerX - 12,              // Left inner
            centerX - 8,               // Left curve
            centerX + 8,               // Right curve
            centerX + 12,              // Right inner
            centerX + 16               // Right base
        };
        int[] headY = {
            centerY - height / 2 - 4,    // Top point
            centerY - height / 2 + 8,    // Left base
            centerY - height / 2 + 6,    // Left inner
            centerY - height / 2 + 4,    // Left curve
            centerY - height / 2 + 4,    // Right curve
            centerY - height / 2 + 6,    // Right inner
            centerY - height / 2 + 8     // Right base
        };

        // Head (dark stone)
        g2.setColor(new Color(60, 60, 70));
        g2.fillPolygon(headX, headY, headX.length);

        // Head highlights
        g2.setColor(new Color(100, 100, 110));
        int[] headHighlightX = {
            centerX,
            centerX - 14,
            centerX - 10,
            centerX - 6,
            centerX + 6,
            centerX + 10,
            centerX + 14
        };
        int[] headHighlightY = {
            centerY - height / 2 - 2,
            centerY - height / 2 + 6,
            centerY - height / 2 + 4,
            centerY - height / 2 + 2,
            centerY - height / 2 + 2,
            centerY - height / 2 + 4,
            centerY - height / 2 + 6
        };
        g2.fillPolygon(headHighlightX, headHighlightY, headHighlightX.length);

        // Eyes (blue glowing) - positioned in the triangular head
        g2.setColor(new Color(100, 100, 255));
        g2.fillOval(centerX - 6, centerY - height / 2 + 2, 4, 4);
        g2.fillOval(centerX + 2, centerY - height / 2 + 2, 4, 4);
        g2.setColor(new Color(150, 150, 255));
        g2.fillOval(centerX - 5, centerY - height / 2 + 3, 2, 2);
        g2.fillOval(centerX + 3, centerY - height / 2 + 3, 2, 2);

        // Unique weapon - massive stone maul with spiked head
        // Handle
        g2.setColor(new Color(100, 100, 110));
        g2.fillRect(centerX + width / 2 - 3, centerY - 6, 12, 8);

        // Maul head - hexagonal shape
        int[] maulX = {
            centerX + width / 2 + 8,
            centerX + width / 2 + 12,
            centerX + width / 2 + 16,
            centerX + width / 2 + 20,
            centerX + width / 2 + 16,
            centerX + width / 2 + 12
        };
        int[] maulY = {
            centerY - 12,
            centerY - 16,
            centerY - 14,
            centerY - 8,
            centerY - 6,
            centerY - 10
        };
        g2.setColor(new Color(80, 80, 90));
        g2.fillPolygon(maulX, maulY, maulX.length);

        // Maul highlights
        g2.setColor(new Color(120, 120, 130));
        int[] maulHighlightX = {
            centerX + width / 2 + 10,
            centerX + width / 2 + 14,
            centerX + width / 2 + 18,
            centerX + width / 2 + 18,
            centerX + width / 2 + 14,
            centerX + width / 2 + 10
        };
        int[] maulHighlightY = {
            centerY - 10,
            centerY - 14,
            centerY - 12,
            centerY - 6,
            centerY - 4,
            centerY - 8
        };
        g2.fillPolygon(maulHighlightX, maulHighlightY, maulHighlightX.length);

        // Spikes on the maul
        g2.setColor(new Color(60, 60, 70));
        g2.fillPolygon(
            new int[]{centerX + width / 2 + 12, centerX + width / 2 + 14, centerX + width / 2 + 16},
            new int[]{centerY - 18, centerY - 22, centerY - 18}, 3);
        g2.fillPolygon(
            new int[]{centerX + width / 2 + 16, centerX + width / 2 + 18, centerX + width / 2 + 20},
            new int[]{centerY - 16, centerY - 20, centerY - 16}, 3);

        // Unique shield - triangular stone barrier
        int[] shieldX = {
            centerX - width / 2 - 8,
            centerX - width / 2 - 4,
            centerX - width / 2,
            centerX - width / 2 - 4
        };
        int[] shieldY = {
            centerY - 8,
            centerY - 12,
            centerY - 4,
            centerY
        };
        g2.setColor(new Color(80, 60, 40));
        g2.fillPolygon(shieldX, shieldY, shieldX.length);
        g2.setColor(new Color(100, 80, 60));
        int[] shieldHighlightX = {
            centerX - width / 2 - 6,
            centerX - width / 2 - 2,
            centerX - width / 2 + 2,
            centerX - width / 2 - 2
        };
        int[] shieldHighlightY = {
            centerY - 6,
            centerY - 10,
            centerY - 2,
            centerY + 2
        };
        g2.fillPolygon(shieldHighlightX, shieldHighlightY, shieldHighlightX.length);

        // Unique legs - hexagonal stone pillars
        // Left leg
        int[] leftLegX = {
            centerX - 10, centerX - 8, centerX - 6, centerX - 4, centerX - 6, centerX - 8
        };
        int[] leftLegY = {
            centerY + 8, centerY + 6, centerY + 4, centerY + 6, centerY + 8, centerY + 10
        };
        g2.setColor(new Color(70, 70, 80));
        g2.fillPolygon(leftLegX, leftLegY, leftLegX.length);

        // Right leg
        int[] rightLegX = {
            centerX + 4, centerX + 6, centerX + 8, centerX + 10, centerX + 8, centerX + 6
        };
        int[] rightLegY = {
            centerY + 6, centerY + 4, centerY + 6, centerY + 8, centerY + 10, centerY + 8
        };
        g2.setColor(new Color(70, 70, 80));
        g2.fillPolygon(rightLegX, rightLegY, rightLegX.length);

        // Stone boots - triangular feet
        g2.setColor(new Color(50, 50, 60));
        g2.fillPolygon(
            new int[]{centerX - 12, centerX - 8, centerX - 4},
            new int[]{centerY + 18, centerY + 22, centerY + 18}, 3);
        g2.fillPolygon(
            new int[]{centerX + 2, centerX + 6, centerX + 10},
            new int[]{centerY + 18, centerY + 22, centerY + 18}, 3);

        // Stone armor plates - geometric patterns
        g2.setColor(new Color(90, 90, 100));
        // Chest plate
        g2.fillPolygon(
            new int[]{centerX - 8, centerX - 4, centerX + 4, centerX + 8},
            new int[]{centerY + 2, centerY - 2, centerY - 2, centerY + 2}, 4);
        // Shoulder plates
        g2.fillPolygon(
            new int[]{centerX - 12, centerX - 8, centerX - 4},
            new int[]{centerY - 4, centerY - 8, centerY - 4}, 3);
        g2.fillPolygon(
            new int[]{centerX + 4, centerX + 8, centerX + 12},
            new int[]{centerY - 4, centerY - 8, centerY - 4}, 3);

        // Reset anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
}
