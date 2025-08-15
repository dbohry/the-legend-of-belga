package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.client.managers.GameConfig;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import com.lhamacorp.games.tlob.core.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Soldier extends Entity {

    private static final int SOLDIER_SIZE = 20;
    private static final double SOLDIER_BASE_SPEED = 3;

    private static final double SOLDIER_MAX_HP = 1.0;
    private static final double SOLDIER_MAX_STAMINA = 1.0;
    private static final double SOLDIER_MAX_MANA = 0;

    private static final int ATTACK_RANGE = 30;

    // --- timing base (60Hz for consistent simulation) ---
    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int ATTACK_COOLDOWN_TICKS = 60;
    private static final int ATTACK_DURATION_TICKS = 3;
    private static final int POST_ATTACK_SLOWDOWN_TICKS = 60;
    private static final int HURT_FLASH_TICKS = 6;

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
    private final double speedScale;
    private final double aimNoiseRad;
    private final double strafeStrength;
    private final double strafeFreqHz;
    private final double strafePhase;
    private final double aggressionRadius;
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
     * Creates a soldier enemy at the specified position.
     */
    public Soldier(double x, double y, Weapon weapon) {
        super(x, y, SOLDIER_SIZE, SOLDIER_SIZE, SOLDIER_BASE_SPEED, SOLDIER_MAX_HP, SOLDIER_MAX_STAMINA, SOLDIER_MAX_MANA, 0, 0, weapon, "Soldier", Alignment.FOE);

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

        // Initialize enhanced wandering behavior
        // Fix: Use proper random selection to ensure equal 20% chances for each behavior
        double behaviorRoll = rand01();
        if (behaviorRoll < 0.2) {
            wanderBehavior = WanderBehavior.RANDOM;
        } else if (behaviorRoll < 0.4) {
            wanderBehavior = WanderBehavior.PATROL;
        } else if (behaviorRoll < 0.6) {
            wanderBehavior = WanderBehavior.CIRCULAR;
        } else if (behaviorRoll < 0.8) {
            wanderBehavior = WanderBehavior.LINEAR;
        } else {
            wanderBehavior = WanderBehavior.IDLE;
        }
        wanderSpeedVariation = 0.3 + 0.4 * rand01();
        wanderDirectionChangeChance = 0.1 + 0.2 * rand01();
        prefersStraightPaths = rand01() < 0.6;
        idleTimeVariation = 0.5 + rand01();
        
        // Initialize personality traits
        isAggressive = rand01() < 0.4;
        isCautious = rand01() < 0.3;
        curiosityLevel = rand01();
        prefersGroupMovement = rand01() < 0.5;
        
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

        double dxToP = player.getX() - x;
        double dyToP = player.getY() - y;
        double distToP = Math.hypot(dxToP, dyToP);

        boolean playerHidden = map.isHidingAtWorld(player.getX(), player.getY());

        if (!playerHidden && distToP <= ATTACK_RANGE && attackCooldown == 0 && attackTimer == 0) {
            attackTimer = baseAttackDuration;
            attackCooldown = baseAttackCooldown;
            postAttackSlowdownTimer = POST_ATTACK_SLOWDOWN_TICKS;
            player.damage(1.0);
            player.applyKnockback(x, y);
        }

        if (attackTimer > 0) {
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
        double groupRadius = 60.0; // Distance to consider allies
        
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
                    wanderTimer = 45 + (int) (rand01() * 90);
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
            double moveSpeed = speed * speedScale * 0.25;
            
            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.5;
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
                    moveSpeed *= 0.5;
                }
                
                moveWithCollision(noisyDx * moveSpeed, noisyDy * moveSpeed, map, player);
                movedThisTick = true;
            }
        }
    }

    private void wander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            pickNewWanderDir();
            wanderTimer = 30 + (int) (rand01() * 60);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.3;
            
            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.5;
            }
            
            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }
    
    private void setupPatrolPoints() {
        // Create patrol points around the initial position
        double radius = 40 + 20 * rand01();
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
            // Aggressive soldiers change direction more frequently
            int baseTime = isAggressive ? 20 : 30;
            wanderTimer = (int) (baseTime + rand01() * 60 * idleTimeVariation);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * (0.2 + wanderSpeedVariation);
            
            // RANDOM soldiers are slower when wandering (but full speed when hunting)
            if (wanderBehavior == WanderBehavior.RANDOM) {
                moveSpeed *= 0.6; // 40% slower when wandering randomly
            }
            
            // Aggressive soldiers move faster
            if (isAggressive) moveSpeed *= 1.2;
            // Cautious soldiers move slower
            if (isCautious) moveSpeed *= 0.8;
            
            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.5;
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
            
            if (dist < 10) {
                // Reached patrol point, move to next
                patrolPointIndex = (patrolPointIndex + 1) % 3;
                // Aggressive soldiers patrol faster
                int baseTime = isAggressive ? 40 : 60;
                patrolTimer = baseTime + (int) (rand01() * 120);
            } else {
                // Move towards patrol point
                wanderDx = dx / dist;
                wanderDy = dy / dist;
                double moveSpeed = speed * speedScale * 0.4;
                
                // Aggressive soldiers move faster during patrol
                if (isAggressive) moveSpeed *= 1.1;
                
                if (postAttackSlowdownTimer > 0) {
                    moveSpeed *= 0.5;
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
            // Curious soldiers make bigger direction changes
            double maxAngleChange = Math.PI / 2 * (0.5 + curiosityLevel * 0.5);
            double angleChange = (rand01() - 0.5) * maxAngleChange;
            double newAngle = currentAngle + angleChange;
            
            wanderDx = Math.cos(newAngle);
            wanderDy = Math.sin(newAngle);
            wanderTimer = 45 + (int) (rand01() * 90);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.35;
            
            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.5;
            }
            
            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }
    
    private void linearWander(TileMap map, Player player) {
        if (wanderTimer <= 0 || (prefersStraightPaths && rand01() < wanderDirectionChangeChance)) {
            pickNewWanderDir();
            // Cautious soldiers prefer longer straight paths
            int baseTime = isCautious ? 90 : 60;
            wanderTimer = baseTime + (int) (rand01() * 120);
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            double moveSpeed = speed * speedScale * 0.4;
            
            if (postAttackSlowdownTimer > 0) {
                moveSpeed *= 0.5;
            }
            
            moveWithCollision(wanderDx * moveSpeed, wanderDy * moveSpeed, map, player);
            movedThisTick = true;
        }
    }
    
    private void idleWander(TileMap map, Player player) {
        if (wanderTimer <= 0) {
            // Aggressive soldiers are less likely to stay idle
            double moveChance = isAggressive ? 0.5 : 0.3;
            if (rand01() < moveChance) {
                pickNewWanderDir();
                wanderTimer = 20 + (int) (rand01() * 40);
            } else {
                // Cautious soldiers stay idle longer
                int baseTime = isCautious ? 120 : 60;
                wanderTimer = baseTime + (int) (rand01() * 120 * idleTimeVariation);
            }
        }

        if (wanderTimer > 0) {
            wanderTimer--;
            if (wanderTimer > 60) {
                double moveSpeed = speed * speedScale * 0.2;
                
                if (postAttackSlowdownTimer > 0) {
                    moveSpeed *= 0.5;
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

    @Override
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!isAlive()) return;

        // Use the specialized soldier sprite
        BufferedImage tex = TextureManager.getSoldierFrame(
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
            drawSoldierEnemy(g2, camX, camY);
        }

        if (hurtTimer > 0) {
            int alpha = (int) (255 * (double) hurtTimer / HURT_FLASH_TICKS);
            g2.setColor(new Color(255, 255, 255, alpha));
            drawCenteredRect(g2, camX, camY, width, height, new Color(255, 255, 255, alpha));
        }

        if (attackTimer > 0) {
            g2.setColor(new Color(255, 0, 0, 150));
            int indicatorSize = width + 10;
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
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (int) Math.round(x) - camX - fm.stringWidth(behaviorText) / 2;
            int textY = (int) Math.round(y) - camY - height / 2 - 15;
            g2.drawString(behaviorText, textX, textY);
            
            // Draw personality indicators
            int indicatorY = textY - 12;
            if (isAggressive) {
                g2.setColor(new Color(255, 0, 0, 150));
                g2.fillOval((int) Math.round(x) - camX - 3, indicatorY - 3, 6, 6);
            }
            if (isCautious) {
                g2.setColor(new Color(0, 0, 255, 150));
                g2.fillRect((int) Math.round(x) - camX - 3, indicatorY - 3, 6, 6);
            }
            if (prefersGroupMovement) {
                g2.setColor(new Color(255, 255, 0, 150));
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

    private void drawSoldierEnemy(Graphics2D g2, int camX, int camY) {
        int centerX = (int) Math.round(x) - camX;
        int centerY = (int) Math.round(y) - camY;
        
        // Enable anti-aliasing for smoother shapes
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(centerX - width/2 + 2, centerY - height/2 + height - 6, width - 4, 4);
        
        // Body (armor)
        g2.setColor(new Color(80, 80, 90)); // Dark armor
        g2.fillRoundRect(centerX - width/2 + 2, centerY - height/2 + 6, width - 4, height - 12, 6, 6);
        
        // Armor highlights
        g2.setColor(new Color(120, 120, 130));
        g2.fillRoundRect(centerX - width/2 + 3, centerY - height/2 + 7, width - 6, height - 14, 4, 4);
        
        // Head
        g2.setColor(new Color(180, 140, 100)); // Skin tone
        g2.fillOval(centerX - 6, centerY - height/2 + 2, 12, 10);
        
        // Helmet
        g2.setColor(new Color(60, 60, 70));
        g2.fillOval(centerX - 7, centerY - height/2 + 1, 14, 8);
        g2.setColor(new Color(80, 80, 90));
        g2.fillOval(centerX - 6, centerY - height/2 + 2, 12, 6);
        
        // Eyes (red glowing)
        g2.setColor(new Color(200, 0, 0));
        g2.fillOval(centerX - 4, centerY - height/2 + 4, 3, 3);
        g2.fillOval(centerX + 1, centerY - height/2 + 4, 3, 3);
        g2.setColor(new Color(255, 0, 0));
        g2.fillOval(centerX - 3, centerY - height/2 + 5, 1, 1);
        g2.fillOval(centerX + 2, centerY - height/2 + 5, 1, 1);
        
        // Weapon (sword)
        g2.setColor(new Color(160, 160, 180));
        g2.fillRect(centerX + width/2 - 1, centerY - 3, 8, 6);
        g2.setColor(new Color(200, 200, 220));
        g2.fillRect(centerX + width/2, centerY - 2, 6, 4);
        
        // Shield
        g2.setColor(new Color(100, 80, 60));
        g2.fillOval(centerX - width/2 - 2, centerY - 2, 8, 8);
        g2.setColor(new Color(120, 100, 80));
        g2.fillOval(centerX - width/2 - 1, centerY - 1, 6, 6);
        
        // Belt
        g2.setColor(new Color(40, 40, 50));
        g2.fillRect(centerX - width/2 + 3, centerY + 2, width - 6, 2);
        
        // Legs
        g2.setColor(new Color(60, 60, 70));
        g2.fillRect(centerX - 4, centerY + 4, 3, 4);
        g2.fillRect(centerX + 1, centerY + 4, 3, 4);
        
        // Boots
        g2.setColor(new Color(40, 40, 50));
        g2.fillRect(centerX - 5, centerY + 8, 5, 2);
        g2.fillRect(centerX, centerY + 8, 5, 2);
        
        // Reset anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
}
