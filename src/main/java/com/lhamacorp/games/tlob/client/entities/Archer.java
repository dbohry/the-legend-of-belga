package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.GameConfig;
import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Archer extends Entity {

    private static final int ARCHER_SIZE = 18;
    private static final double ARCHER_BASE_SPEED = 3.0;
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

    // Enhanced wandering behavior for archers
    private final ArcherWanderBehavior wanderBehavior;
    private int tacticalTimer = 0;
    private final double preferredDistance;
    private final double tacticalRadius;
    private final boolean prefersHighGround;
    private final double retreatChance;
    private final double flankingTendency;
    private final double coverSeeking;
    private final boolean isTactical;
    private final boolean isCowardly;
    private final double precisionLevel;

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

    // Archer-specific wander behavior types
    private enum ArcherWanderBehavior {
        TACTICAL,     // Maintain optimal attack distance
        COVER_SEEKING, // Prefer to move behind cover
        FLANKING,     // Try to move to player's sides
        RETREAT,      // Prefer to move away when threatened
        AMBUSH        // Stay still and wait for opportunities
    }

    /**
     * Creates an archer enemy at the specified position.
     */
    public Archer(double x, double y, Weapon weapon) {
        super(x, y, ARCHER_SIZE, ARCHER_SIZE, ARCHER_BASE_SPEED, ARCHER_MAX_HP, ARCHER_MAX_STAMINA, ARCHER_MAX_MANA, 0, 0, weapon, "Archer", Alignment.FOE);

        // Improved seed generation with more entropy
        int seed = (int) ((Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y)) ^ 0x9E3779B9);
        lcg = (seed == 0) ? 1 : seed;

        // Add additional entropy based on current time and object hash
        lcg ^= System.nanoTime() & 0xFFFF;
        lcg ^= this.hashCode() & 0xFFFF;

        speedScale = 0.50 + 0.30 * rand01();
        aimNoiseRad = Math.toRadians((rand01() - 0.5) * 14.0);
        boolean willStrafe = rand01() < 0.45;
        strafeStrength = willStrafe ? 0.20 + 0.25 * rand01() : 0.0;
        strafeFreqHz = 0.6 + 0.8 * rand01();
        strafePhase = rand01() * Math.PI * 2.0;
        aggressionRadius = 220 + 140 * rand01();

        baseAttackCooldown = (int) Math.round(ATTACK_COOLDOWN_TICKS * (0.85 + 0.5 * rand01()));
        baseAttackDuration = Math.max(2, ATTACK_DURATION_TICKS + (rand01() < 0.3 ? 1 : 0));

        // Initialize enhanced archer wandering behavior
        // Fix: Use proper random selection to ensure equal 20% chances for each behavior
        double behaviorRoll = rand01();
        if (behaviorRoll < 0.2) {
            wanderBehavior = ArcherWanderBehavior.TACTICAL;
        } else if (behaviorRoll < 0.4) {
            wanderBehavior = ArcherWanderBehavior.COVER_SEEKING;
        } else if (behaviorRoll < 0.6) {
            wanderBehavior = ArcherWanderBehavior.FLANKING;
        } else if (behaviorRoll < 0.8) {
            wanderBehavior = ArcherWanderBehavior.RETREAT;
        } else {
            wanderBehavior = ArcherWanderBehavior.AMBUSH;
        }
        preferredDistance = MIN_ATTACK_RANGE + (ATTACK_RANGE - MIN_ATTACK_RANGE) * (0.3 + 0.4 * rand01());
        tacticalRadius = 80 + 60 * rand01();
        prefersHighGround = rand01() < 0.4;
        retreatChance = 0.2 + 0.3 * rand01();
        flankingTendency = 0.3 + 0.4 * rand01();
        coverSeeking = 0.4 + 0.4 * rand01();

        // Initialize personality traits
        isTactical = rand01() < 0.6;
        isCowardly = rand01() < 0.3;
        precisionLevel = 0.5 + 0.5 * rand01();

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
            // Use tactical group behavior if allies are nearby
            if (nearbyAllies != null && !nearbyAllies.isEmpty() && isTactical) {
                tacticalGroupWander(map, player, nearbyAllies);
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
        double groupRadius = 80.0; // Archers have longer tactical range

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

    private void tacticalGroupWander(TileMap map, Player player, List<Entity> allies) {
        if (tacticalTimer <= 0) {
            // Tactical group behavior: position for crossfire or support
            if (!allies.isEmpty()) {
                // Find the closest ally to coordinate with
                Entity closestAlly = null;
                double closestDist = Double.POSITIVE_INFINITY;

                for (Entity ally : allies) {
                    double dist = Math.hypot(ally.getX() - x, ally.getY() - y);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestAlly = ally;
                    }
                }

                if (closestAlly != null) {
                    // Position for tactical advantage (crossfire, support, etc.)
                    double playerX = player.getX();
                    double playerY = player.getY();
                    double allyX = closestAlly.getX();
                    double allyY = closestAlly.getY();

                    // Calculate tactical position (perpendicular to ally-player line)
                    double dxToPlayer = playerX - allyX;
                    double dyToPlayer = playerY - allyY;
                    double distToPlayer = Math.hypot(dxToPlayer, dyToPlayer);

                    if (distToPlayer > 0) {
                        // Position perpendicular to the ally-player line
                        double perpAngle = Math.atan2(dyToPlayer, dxToPlayer) + Math.PI / 2;
                        double tacticalX = allyX + Math.cos(perpAngle) * preferredDistance;
                        double tacticalY = allyY + Math.sin(perpAngle) * preferredDistance;

                        // Move towards tactical position
                        double dx = tacticalX - x;
                        double dy = tacticalY - y;
                        double dist = Math.hypot(dx, dy);

                        if (dist > 0) {
                            wanderDx = dx / dist;
                            wanderDy = dy / dist;
                            tacticalTimer = 90 + (int) (rand01() * 120);
                        } else {
                            pickNewWanderDir();
                            tacticalTimer = 60 + (int) (rand01() * 90);
                        }
                    } else {
                        pickNewWanderDir();
                        tacticalTimer = 60 + (int) (rand01() * 90);
                    }
                } else {
                    pickNewWanderDir();
                    tacticalTimer = 60 + (int) (rand01() * 90);
                }
            } else {
                pickNewWanderDir();
                tacticalTimer = 60 + (int) (rand01() * 90);
            }
        }

        if (tacticalTimer > 0) {
            tacticalTimer--;
            double moveSpeed = speed * speedScale * 0.3;

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
                moveWithCollision(noisyDx * moveSpeed, noisyDy * moveSpeed, map, player);
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
                moveWithCollision(awayDx * moveSpeed, awayDy * moveSpeed, map, player);
                movedThisTick = true;
            }
        }
    }

    private void enhancedWander(TileMap map, Player player) {
        switch (wanderBehavior) {
            case TACTICAL:
                tacticalWander(map, player);
                break;
            case COVER_SEEKING:
                coverSeekingWander(map, player);
                break;
            case FLANKING:
                flankingWander(map, player);
                break;
            case RETREAT:
                retreatWander(map, player);
                break;
            case AMBUSH:
                ambushWander(map, player);
                break;
        }
    }

    private void tacticalWander(TileMap map, Player player) {
        if (tacticalTimer <= 0) {
            double distToP = Math.hypot(x - player.getX(), y - player.getY());
            if (distToP < preferredDistance) {
                // If too close, back away
                backAwayFromPlayer(player, map);
            } else if (distToP > preferredDistance + tacticalRadius) {
                // If too far, approach
                approachPlayer(player, map);
            } else {
                // If at preferred distance, make small tactical movements
                double angle = Math.atan2(player.getY() - y, player.getX() - x) + (rand01() - 0.5) * Math.PI / 4;
                wanderDx = Math.cos(angle) * 0.3;
                wanderDy = Math.sin(angle) * 0.3;
                double moveSpeed = speed * speedScale * 0.2;

                // Tactical archers move more precisely
                if (isTactical) moveSpeed *= 0.8;

                moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
                movedThisTick = true;
            }
            // Tactical archers adjust position more frequently
            int baseTime = isTactical ? 40 : 60;
            tacticalTimer = baseTime + (int) (rand01() * 120);
        } else {
            tacticalTimer--;
        }
    }

    private void coverSeekingWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            // Try to find cover by moving perpendicular to player direction
            double dxToP = player.getX() - x;
            double dyToP = player.getY() - y;
            double distToP = Math.hypot(dxToP, dyToP);

            if (distToP > 0) {
                // Move perpendicular to player direction (seeking cover)
                double perpAngle = Math.atan2(dyToP, dxToP) + Math.PI / 2 + (rand01() - 0.5) * Math.PI / 2;
                wanderDx = Math.cos(perpAngle);
                wanderDy = Math.sin(perpAngle);
                wanderTimer = 45 + (int) (rand01() * 90);
            } else {
                pickNewWanderDir();
                wanderTimer = 30 + (int) (rand01() * 60);
            }
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.25;

            // Cover-seeking archers move more carefully
            if (coverSeeking > 0.7) moveSpeed *= 0.8;

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }

    private void flankingWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            // Try to move to player's sides for flanking
            double dxToP = player.getX() - x;
            double dyToP = player.getY() - y;
            double distToP = Math.hypot(dxToP, dyToP);

            if (distToP > 0) {
                // Calculate flanking position (to the side of the player)
                double flankAngle = Math.atan2(dyToP, dxToP) + (rand01() < 0.5 ? Math.PI / 2 : -Math.PI / 2);
                double flankX = player.getX() + Math.cos(flankAngle) * preferredDistance;
                double flankY = player.getY() + Math.sin(flankAngle) * preferredDistance;

                // Move towards flanking position
                double dx = flankX - x;
                double dy = flankY - y;
                double dist = Math.hypot(dx, dy);
                if (dist > 0) {
                    wanderDx = dx / dist;
                    wanderDy = dy / dist;
                    // High flanking tendency means longer flanking movements
                    int baseTime = (int) (60 + flankingTendency * 60);
                    wanderTimer = baseTime + (int) (rand01() * 120);
                } else {
                    pickNewWanderDir();
                    wanderTimer = 30 + (int) (rand01() * 60);
                }
            } else {
                pickNewWanderDir();
                wanderTimer = 30 + (int) (rand01() * 60);
            }
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.3;

            // High flanking tendency means faster movement
            if (flankingTendency > 0.7) moveSpeed *= 1.1;

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }

    private void retreatWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            double distToP = Math.hypot(x - player.getX(), y - player.getY());
            // Cowardly archers retreat more often
            double actualRetreatChance = isCowardly ? retreatChance * 1.5 : retreatChance;
            if (distToP < preferredDistance && rand01() < actualRetreatChance) {
                // Retreat away from player
                double retreatAngle = Math.atan2(y - player.getY(), x - player.getX());
                wanderDx = Math.cos(retreatAngle);
                wanderDy = Math.sin(retreatAngle);
                wanderTimer = 90 + (int) (rand01() * 120);
            } else {
                // Normal wandering
                pickNewWanderDir();
                wanderTimer = 30 + (int) (rand01() * 60);
            }
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.35;

            // Cowardly archers retreat faster
            if (isCowardly) moveSpeed *= 1.2;

            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }

    private void ambushWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            double distToP = Math.hypot(x - player.getX(), y - player.getY());
            if (distToP > preferredDistance + 20) {
                // If too far, move closer
                approachPlayer(player, map);
                wanderTimer = 30 + (int) (rand01() * 60);
            } else {
                // Stay still and wait for opportunities
                // Precision archers wait longer for perfect shots
                int baseTime = (int) (120 + precisionLevel * 120);
                wanderTimer = baseTime + (int) (rand01() * 180);
            }
        } else {
            wanderTimer--;
            // Only move occasionally during ambush
            if (wanderTimer < 30 && rand01() < 0.1) {
                pickNewWanderDir();
                double moveSpeed = speed * speedScale * 0.15;
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

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!isAlive()) return;

        // Use the specialized archer sprite
        BufferedImage tex = TextureManager.getArcherFrame(
            TextureManager.convertEntityDirection(facing), 
            TextureManager.convertEntityMotion(movedThisTick), 
            animTimeMs
        );
        if (tex != null) {
            int px = (int) Math.round(x - width / 2.0) - camX;
            int py = (int) Math.round(y - height / 2.0) - camY;
            g2.drawImage(tex, px, py, null);
        } else {
            // Fallback to custom drawing if sprite is not available
            drawArcherEnemy(g2, camX, camY);
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

        // Draw wandering behavior indicator
        if (GameConfig.getInstance().isShowEnemyBehaviorIndicators() && (wanderTimer > 0 || tacticalTimer > 0)) {
            String behaviorText = wanderBehavior.name().substring(0, 1);
            g2.setColor(new Color(0, 255, 255, 200));
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (int) Math.round(x) - camX - fm.stringWidth(behaviorText) / 2;
            int textY = (int) Math.round(y) - camY - height / 2 - 15;
            g2.drawString(behaviorText, textX, textY);

            // Draw personality indicators
            int indicatorY = textY - 12;
            if (isTactical) {
                g2.setColor(new Color(0, 255, 0, 150));
                g2.fillRect((int) Math.round(x) - camX - 3, indicatorY - 3, 6, 6);
            }
            if (isCowardly) {
                g2.setColor(new Color(255, 165, 0, 150));
                g2.fillOval((int) Math.round(x) - camX - 3, indicatorY - 3, 6, 6);
            }
            if (precisionLevel > 0.8) {
                g2.setColor(new Color(255, 0, 255, 150));
                g2.fillPolygon(
                    new int[]{(int) Math.round(x) - camX - 3, (int) Math.round(x) - camX + 3, (int) Math.round(x) - camX},
                    new int[]{indicatorY + 3, indicatorY + 3, indicatorY - 3}, 3);
            }
        }
        
        // Draw perk count indicator
        if (GameConfig.getInstance().isShowEnemyBehaviorIndicators() && getPerkCount() > 0) {
            String perkText = String.valueOf(getPerkCount());
            g2.setColor(new Color(255, 215, 0, 220)); // Gold color for perks
            g2.setFont(new Font("Arial", Font.BOLD, 10)); // Same size as behavior indicator
            FontMetrics fm = g2.getFontMetrics();
            int textX = (int) Math.round(x) - camX + 8; // To the right of behavior indicator
            int textY = (int) Math.round(y) - camY - height / 2 - 15; // Same Y as behavior indicator
            g2.drawString(perkText, textX, textY);
        }
    }

    private void drawArcherEnemy(Graphics2D g2, int camX, int camY) {
        int centerX = (int) Math.round(x) - camX;
        int centerY = (int) Math.round(y) - camY;

        // Enable anti-aliasing for smoother shapes
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(centerX - width / 2 + 2, centerY - height / 2 + height - 6, width - 4, 4);

        // Body (armor) - BLUE instead of gray
        g2.setColor(new Color(60, 80, 120)); // Dark blue armor
        g2.fillRoundRect(centerX - width / 2 + 2, centerY - height / 2 + 6, width - 4, height - 12, 6, 6);

        // Armor highlights - BLUE instead of gray
        g2.setColor(new Color(80, 100, 140));
        g2.fillRoundRect(centerX - width / 2 + 3, centerY - height / 2 + 7, width - 6, height - 14, 4, 4);

        // Head
        g2.setColor(new Color(180, 140, 100)); // Skin tone
        g2.fillOval(centerX - 6, centerY - height / 2 + 2, 12, 10);

        // Helmet - BLUE instead of gray
        g2.setColor(new Color(40, 60, 100));
        g2.fillOval(centerX - 7, centerY - height / 2 + 1, 14, 8);
        g2.setColor(new Color(60, 80, 120));
        g2.fillOval(centerX - 6, centerY - height / 2 + 2, 12, 6);

        // Eyes (blue glowing instead of red)
        g2.setColor(new Color(0, 100, 200));
        g2.fillOval(centerX - 4, centerY - height / 2 + 4, 3, 3);
        g2.fillOval(centerX + 1, centerY - height / 2 + 4, 3, 3);
        g2.setColor(new Color(0, 150, 255));
        g2.fillOval(centerX - 3, centerY - height / 2 + 5, 1, 1);
        g2.fillOval(centerX + 2, centerY - height / 2 + 5, 1, 1);

        // Weapon (bow instead of sword) - BLUE to match armor
        g2.setColor(new Color(80, 100, 140));
        g2.fillRect(centerX + width / 2 - 1, centerY - 3, 8, 6);
        g2.setColor(new Color(100, 120, 160));
        g2.fillRect(centerX + width / 2, centerY - 2, 6, 4);

        // Bow string
        g2.setColor(new Color(220, 220, 220));
        g2.drawLine(centerX + width / 2 + 1, centerY - 1, centerX + width / 2 + 1, centerY + 1);

        // NO SHIELD - removed as requested

        // Belt - BLUE instead of gray
        g2.setColor(new Color(40, 60, 80));
        g2.fillRect(centerX - width / 2 + 3, centerY + 2, width - 6, 2);

        // Legs - BLUE instead of gray
        g2.setColor(new Color(60, 80, 100));
        g2.fillRect(centerX - 4, centerY + 4, 3, 4);
        g2.fillRect(centerX + 1, centerY + 4, 3, 4);

        // Boots - BLUE instead of gray
        g2.setColor(new Color(40, 60, 80));
        g2.fillRect(centerX - 5, centerY + 8, 5, 2);
        g2.setColor(new Color(40, 60, 80));
        g2.fillRect(centerX, centerY + 8, 5, 2);

        // Reset anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
}
