package com.lhamacorp.games.tlob.client.entities;

import com.lhamacorp.games.tlob.client.inventory.Inventory;
import com.lhamacorp.games.tlob.client.managers.AudioManager;
import com.lhamacorp.games.tlob.client.managers.BaseGameManager;
import com.lhamacorp.games.tlob.client.managers.KeyManager;
import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Bow;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import com.lhamacorp.games.tlob.client.world.InputState;
import com.lhamacorp.games.tlob.client.world.PlayerInputView;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

public class Player extends Entity {

    private static final int PLAYER_SIZE = 22;
    private static final double PLAYER_SPEED = 3;
    private static final double PLAYER_MAX_HP = 6.0;
    private static final double PLAYER_MAX_STAMINA = 6.0;
    private static final double PLAYER_MAX_MANA = 0.0;
    private static final double PLAYER_MAX_SHIELD = 0;
    private static final double PLAYER_SPEED_PPS = 90.0;

    // XP and Level system constants
    private static final int XP_PER_ENEMY_KILL = 10;
    private static final int XP_PER_LEVEL_BASE = 100;
    private static final double XP_SCALING_FACTOR = 1.5; // Each level requires 50% more XP than the previous

    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int STAMINA_DRAIN_INTERVAL = TICKS_PER_SECOND;
    private static final int STAMINA_REGEN_INTERVAL = TICKS_PER_SECOND * 3;
    private static final int MANA_REGEN_INTERVAL = TICKS_PER_SECOND * 2;

    private static final double DASH_MANA_COST = 2.0;
    private static final double DASH_DISTANCE = 100.0;
    private static final int DASH_COOLDOWN_TICKS = TICKS_PER_SECOND * 2;
    private static final int DASH_MOVEMENT_TICKS = 5;

    // Block mechanism constants
    private static final double BLOCK_DAMAGE_REDUCTION = 0.8; // 80% damage reduction when blocking

    private int staminaDrainCounter = 0;
    private int staminaRegenCounter = 0;
    private int manaRegenCounter = 0;
    private boolean wasSprinting = false;
    private double facingAngle = 0.0;

    // XP and Level system fields
    private int currentXP = 0;
    private int currentLevel = 1;
    private int xpToNextLevel;

    // Level up listener for perk selection
    private LevelUpListener levelUpListener;

    // Inventory system
    private Inventory inventory;

    private boolean wasDashPressed = false;
    private boolean dashTriggered = false;
    private int dashCooldownTimer = 0;
    private boolean canDash = true;

    private int dashTrailTimer = 0;
    private double dashStartX = 0;
    private double dashStartY = 0;
    private static final int DASH_TRAIL_DURATION = 10;

    private int dashMovementTimer = 0;
    private double dashTargetX = 0;
    private double dashTargetY = 0;
    private double dashDirectionX = 0;
    private double dashDirectionY = 0;

    private static final int MAX_SHADOW_TRAILS = 8;
    private static final int SHADOW_TRAIL_INTERVAL = 1;
    private int shadowTrailCounter = 0;
    private final double[] shadowTrailX = new double[MAX_SHADOW_TRAILS];
    private final double[] shadowTrailY = new double[MAX_SHADOW_TRAILS];
    private final int[] shadowTrailTimer = new int[MAX_SHADOW_TRAILS];
    private static final int SHADOW_TRAIL_DURATION = 20;

    private boolean isInvulnerable = false;
    private boolean isBlocking = false;

    private int attackTimer = 0;
    private int attackCooldown = 0;
    private double speedMultiplier = 1.0;
    private double damageMultiplier = 1.0;

    private long animTimeMs = 0L;
    private boolean movingThisTick = false;

    private Point lastAimPoint = null;
    private int aimIndicatorAlpha = 0;
    private int attackSwingPhase = 0;
    private double attackSwingAngle = 0.0;
    private int screenShakeTimer = 0;
    private static final int AIM_INDICATOR_FADE_TIME = 60;
    private static final int ATTACK_SWING_DURATION = 10;
    private static final int SCREEN_SHAKE_DURATION = 12;

    // Arrow projectile state (for bow attacks)
    private int arrowTimer = 0;
    private int arrowTravelTicks = 30;  // Actual travel time for current arrow
    private double arrowX = 0;
    private double arrowY = 0;
    private double arrowTargetX = 0;
    private double arrowTargetY = 0;
    private static final double ARROW_COLLISION_RADIUS = 25.0;  // Hit detection radius

    private static final double SWING_ARC_RADIANS = Math.PI / 2.0;
    private static final double SWING_TRAIL_SPACING = 0.1;
    private static final int MAX_TRAIL_COUNT = 3;
    private static final int CROSSHAIR_SIZE = 8;
    private static final int ENEMY_HIGHLIGHT_SIZE = 12;
    private static final double ENEMY_HIGHLIGHT_RANGE_MULTIPLIER = 1.5;
    private static final double SCREEN_SHAKE_INTENSITY = 6.0;

    /**
     * Creates a player at the specified position with the given weapon.
     */
    public Player(double x, double y, Weapon weapon) {
        super(x, y, PLAYER_SIZE, PLAYER_SIZE, PLAYER_SPEED, PLAYER_MAX_HP, PLAYER_MAX_STAMINA, PLAYER_MAX_MANA, PLAYER_MAX_SHIELD, 0, weapon, "Player", Alignment.NEUTRAL);

        this.inventory = new Inventory(weapon);
        this.inventory.addWeapon(new Bow(1, 200, 5, 8, 50));

        calculateXPToNextLevel();
    }

    /**
     * Sets the player's position without triggering effects.
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the player's health, clamping to valid range.
     */
    public void setHealth(double h) {
        this.health = Math.max(0.0, Math.min(h, maxHealth));
        this.alive = (this.health > 0.0);
    }

    /**
     * Sets the player's stamina, clamping to valid range.
     */
    public void setStamina(double s) {
        this.stamina = Math.max(0.0, Math.min(s, maxStamina));
    }

    /**
     * Sets the player's shield, clamping to valid range.
     */
    public void setShield(double sh) {
        this.shield = Math.max(0.0, Math.min(sh, maxShield));
    }

    /** Maps server octant (0..7) to local facing enum + angle. 0=→, 1=↘, 2=↓, 3=↙, 4=←, 5=↖, 6=↑, 7=↗ */
    public void setFacingOctant(int octant) {
        int o = ((octant % 8) + 8) % 8;      // wrap to [0..7]
        int signed = (o <= 4) ? o : o - 8;   // [-4..4] for the same switch map we use below
        this.facingAngle = signed * (Math.PI / 4.0);
        updateFacingFromAngle(signed);
    }

    /**
     * Updates facing direction based on angle octant
     * @param octant signed octant value [-4..4]
     */
    private void updateFacingFromAngle(int octant) {
        switch (octant) {
            case 0 -> this.facing = Direction.RIGHT;
            case 1 -> this.facing = Direction.DOWN_RIGHT;
            case 2 -> this.facing = Direction.DOWN;
            case 3 -> this.facing = Direction.DOWN_LEFT;
            case -1 -> this.facing = Direction.UP_RIGHT;
            case -2 -> this.facing = Direction.UP;
            case -3 -> this.facing = Direction.UP_LEFT;
            default -> this.facing = Direction.LEFT; // ±4
        }
    }

    /**
     * Restores the player's health to maximum.
     */
    public void heal() {
        this.health = maxHealth;
        this.alive = true;
    }

    /**
     * Restores all player stats to maximum values.
     */
    public void restoreAll() {
        this.health = this.maxHealth;
        this.mana = this.maxMana;
        this.stamina = this.maxStamina;
        this.shield = this.maxShield;
    }

    private int effectiveStaminaRegenInterval() {
        double interval = STAMINA_REGEN_INTERVAL / Math.max(0.0001, staminaRegenRateMult);
        return Math.max(1, (int) Math.round(interval));
    }

    private int effectiveManaRegenInterval() {
        double interval = MANA_REGEN_INTERVAL / Math.max(0.0001, manaRegenRateMult);
        return Math.max(1, (int) Math.round(interval));
    }

    public boolean canDash() {
        return canDash && mana >= DASH_MANA_COST;
    }

    public double getDashManaCost() {
        return DASH_MANA_COST;
    }

    public boolean isDashTrailActive() {
        return dashTrailTimer > 0;
    }

    public Point getDashTrailOffset() {
        if (dashTrailTimer <= 0) return new Point(0, 0);

        double progress = (double) dashTrailTimer / DASH_TRAIL_DURATION;
        int offsetX = (int) ((dashStartX - x) * progress);
        int offsetY = (int) ((dashStartY - y) * progress);

        return new Point(offsetX, offsetY);
    }

    public boolean isDashing() {
        return dashMovementTimer > 0;
    }

    public double getDashProgress() {
        if (dashMovementTimer <= 0) return 0.0;
        return 1.0 - ((double) dashMovementTimer / DASH_MOVEMENT_TICKS);
    }

    private void addShadowTrail(double x, double y) {
        for (int i = 0; i < MAX_SHADOW_TRAILS; i++) {
            if (shadowTrailTimer[i] <= 0) {
                shadowTrailX[i] = x;
                shadowTrailY[i] = y;
                shadowTrailTimer[i] = SHADOW_TRAIL_DURATION;
                break;
            }
        }
    }

    public int getShadowTrailCount() {
        int count = 0;
        for (int i = 0; i < MAX_SHADOW_TRAILS; i++) {
            if (shadowTrailTimer[i] > 0) count++;
        }
        return count;
    }

    public double getShadowTrailX(int index) {
        if (index >= 0 && index < MAX_SHADOW_TRAILS) {
            return shadowTrailX[index];
        }
        return 0.0;
    }

    public double getShadowTrailY(int index) {
        if (index >= 0 && index < MAX_SHADOW_TRAILS) {
            return shadowTrailY[index];
        }
        return 0.0;
    }

    public int getShadowTrailTimer(int index) {
        if (index >= 0 && index < MAX_SHADOW_TRAILS) {
            return shadowTrailTimer[index];
        }
        return 0;
    }

    public int getMaxShadowTrails() {
        return MAX_SHADOW_TRAILS;
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    /**
     * Checks if the player is currently blocking.
     * @return true if the player is blocking, false otherwise
     */
    public boolean isBlocking() {
        return isBlocking;
    }

    /**
     * Checks if the player can currently block a specific damage amount (has enough stamina).
     * @param damage the incoming damage amount
     * @return true if the player can block, false otherwise
     */
    public boolean canBlock(double damage) {
        return stamina >= (damage * 0.5);
    }

    /**
     * Checks if the player can currently block (has enough stamina).
     * @return true if the player can block, false otherwise
     */
    public boolean canBlock() {
        return stamina > 0; // Can always attempt to block if they have any stamina
    }

    /**
     * Gets the current XP of the player.
     * @return the current XP
     */
    public int getCurrentXP() {
        return currentXP;
    }

    /**
     * Gets the current level of the player.
     * @return the current level
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Gets the XP required to reach the next level.
     * @return the XP required for next level
     */
    public int getXPToNextLevel() {
        return xpToNextLevel;
    }

    /**
     * Gets the XP progress towards the next level as a percentage (0.0 to 1.0).
     * @return the progress percentage
     */
    public double getLevelProgress() {
        if (currentLevel == 1 && currentXP == 0) return 0.0;
        
        int xpForCurrentLevel = getXPRequiredForLevel(currentLevel);
        int xpProgress = currentXP - xpForCurrentLevel;
        int xpNeeded = xpToNextLevel - xpForCurrentLevel;
        
        return Math.min(1.0, Math.max(0.0, (double) xpProgress / xpNeeded));
    }

    /**
     * Interface for listening to level-up events
     */
    public interface LevelUpListener {
        void onLevelUp(int newLevel);
    }

    /**
     * Sets the level-up listener
     * @param listener the listener to notify when leveling up
     */
    public void setLevelUpListener(LevelUpListener listener) {
        this.levelUpListener = listener;
    }

    /**
     * Gets the current level-up listener
     * @return the current level-up listener, or null if none is set
     */
    public LevelUpListener getLevelUpListener() {
        return levelUpListener;
    }

    /**
     * Adds XP to the player and handles level ups.
     * @param xp the XP to add
     * @return true if the player leveled up, false otherwise
     */
    public boolean addXP(int xp) {
        if (xp <= 0) return false;
        
        currentXP += xp;
        boolean leveledUp = false;
        
        // Check for level ups
        while (currentXP >= xpToNextLevel) {
            currentLevel++;
            leveledUp = true;
            calculateXPToNextLevel();
            
            // Play level up sound
            AudioManager.playSound("level-up.wav", -10.0f);
            
            // Notify level-up listener
            if (levelUpListener != null) {
                levelUpListener.onLevelUp(currentLevel);
            }
        }
        
        return leveledUp;
    }

    /**
     * Grants XP for killing an enemy.
     * @param enemy the enemy that was killed
     */
    public void onEnemyKilled(Entity enemy) {
        if (enemy == null) return;
        
        // Base XP for any enemy
        int xpGained = XP_PER_ENEMY_KILL;
        
        // XP is multiplied by the number of perks the enemy has
        // Formula: XP = base XP × (1 + perk count)
        // This means:
        // - 0 perks: 10 × (1 + 0) = 10 XP
        // - 1 perk: 10 × (1 + 1) = 20 XP
        // - 2 perks: 10 × (1 + 2) = 30 XP
        // - 3 perks: 10 × (1 + 3) = 40 XP
        // - etc.
        int perkCount = enemy.getPerkCount();
        xpGained *= (1 + perkCount);
        
        addXP(xpGained);
    }

    /**
     * Calculates the XP required to reach the next level.
     */
    private void calculateXPToNextLevel() {
        xpToNextLevel = getXPRequiredForLevel(currentLevel + 1);
    }

    /**
     * Gets the total XP required to reach a specific level.
     * @param level the target level
     * @return the total XP required
     */
    public static int getXPRequiredForLevel(int level) {
        if (level <= 1) return 0;
        
        // Progressive XP requirement: each level requires more XP than the previous
        // Level 2: 100 XP, Level 3: 250 XP, Level 4: 400 XP, etc.
        int totalXP = 0;
        for (int i = 2; i <= level; i++) {
            totalXP += (int) (XP_PER_LEVEL_BASE * Math.pow(XP_SCALING_FACTOR, i - 2));
        }
        return totalXP;
    }

    /**
     * Sets the player's XP and level (used for loading save games).
     * @param xp the XP to set
     * @param level the level to set
     */
    public void setXPAndLevel(int xp, int level) {
        this.currentXP = Math.max(0, xp);
        this.currentLevel = Math.max(1, level);
        calculateXPToNextLevel();
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

                public boolean defense() {
                    return is.defense;
                }
                
                public boolean dash() {
                    return is.dash;
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

                public boolean defense() {
                    return km.defense;
                }
                
                public boolean dash() {
                    return km.dash;
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

                public boolean defense() {
                    return false;
                }
                
                public boolean dash() {
                    return false;
                }
            };
        }

        TileMap map = (TileMap) args[1];
        @SuppressWarnings("unchecked") List<Entity> enemies = (List<Entity>) args[2];

        // Optional mouse-aim point in WORLD coords
        Point aimPoint = null;
        if (args.length >= 4 && args[3] instanceof Point p) aimPoint = p;

        // Update aim direction tracking
        if (aimPoint != null) {
            lastAimPoint = aimPoint;
            aimIndicatorAlpha = AIM_INDICATOR_FADE_TIME;
        } else if (aimIndicatorAlpha > 0) {
            aimIndicatorAlpha--;
        }

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
                double angle = Math.atan2(ay, ax); // -PI..PI
                facingAngle = angle;
                int oct = (int) Math.round(angle / (Math.PI / 4.0));
                updateFacingFromAngle(oct);
            }
        } else if (dxRaw != 0 || dyRaw != 0) {
            double angle = Math.atan2(dyRaw, dxRaw);
            facingAngle = angle;
            int oct = (int) Math.round(angle / (Math.PI / 4.0));
            updateFacingFromAngle(oct);
        }

        // --- Movement (normalize diagonals) ---
        double dx = dxRaw, dy = dyRaw;
        if (dx != 0 && dy != 0) {
            double inv = Math.sqrt(0.5);
            dx *= inv;
            dy *= inv;
        }

        movingThisTick = (dxRaw != 0 || dyRaw != 0) && knockbackTimer == 0;

        // --- Dash ability and Sprint / stamina ---
        boolean shiftPressed = input.sprint();
        boolean dashPressed = input.dash();
        boolean sprinting = false;
        double speedBase = (PLAYER_SPEED_PPS / TICKS_PER_SECOND) * speedMultiplier;

        // Update dash cooldown
        if (dashCooldownTimer > 0) {
            dashCooldownTimer--;
            if (dashCooldownTimer == 0) {
                canDash = true;
            }
        }

        // Handle dash ability
        if (dashPressed && !wasDashPressed && canDash && mana >= DASH_MANA_COST && (dxRaw != 0 || dyRaw != 0)) {
            // Dash triggered - consume mana and set up dash movement
            mana -= DASH_MANA_COST;
            dashTriggered = true;
            canDash = false;
            dashCooldownTimer = DASH_COOLDOWN_TICKS;

            // Record dash start position for trail effect
            dashStartX = x;
            dashStartY = y;
            dashTrailTimer = DASH_TRAIL_DURATION;

            // Calculate dash target position
            double dashX = x + dxRaw * DASH_DISTANCE;
            double dashY = y + dyRaw * DASH_DISTANCE;

            // Check if dash destination is valid (no collision)
            if (!collidesWithMap(dashX, dashY, map)) {
                dashTargetX = dashX;
                dashTargetY = dashY;
            } else {
                // Try shorter dash if full distance is blocked
                double shorterDash = DASH_DISTANCE * 0.5;
                dashX = x + dxRaw * shorterDash;
                dashY = y + dyRaw * shorterDash;
                if (!collidesWithMap(dashX, dashY, map)) {
                    dashTargetX = dashX;
                    dashTargetY = dashY;
                } else {
                    // Dash completely blocked, cancel it
                    dashTriggered = false;
                    canDash = true;
                    dashCooldownTimer = 0;
                    mana += DASH_MANA_COST; // Refund mana
                }
            }

            // Set up dash movement if target is valid
            if (dashTriggered) {
                dashMovementTimer = DASH_MOVEMENT_TICKS;
                dashDirectionX = (dashTargetX - x) / DASH_MOVEMENT_TICKS;
                dashDirectionY = (dashTargetY - y) / DASH_MOVEMENT_TICKS;

                // Start invulnerability during dash
                isInvulnerable = true;

                // Add dash screen shake effect
                screenShakeTimer = SCREEN_SHAKE_DURATION;
            }
        }

        // Handle sprinting (only if not dashing, not blocking, and has stamina)
        if (shiftPressed && stamina >= 1.0 && !dashTriggered && dashMovementTimer == 0 && !isBlocking) {
            sprinting = true;
            speed = speedBase * 2.0;
            if (++staminaDrainCounter >= STAMINA_DRAIN_INTERVAL) {
                stamina = Math.max(0, stamina - 1.0);
                staminaDrainCounter = 0;
            }
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

        // Independent mana regeneration (separate from stamina)
        if (++manaRegenCounter >= effectiveManaRegenInterval()) {
            if (mana < maxMana) {
                mana = Math.min(maxMana, mana + 0.5);
            }
            manaRegenCounter = 0;
        }

        // Reset dash trigger when dash is released and dash movement is complete
        if (!dashPressed && dashMovementTimer == 0) {
            dashTriggered = false;
        }

        wasDashPressed = dashPressed;
        wasSprinting = sprinting;

        // --- Knockback & movement ---
        updateKnockbackWithMap(map);
        if (knockbackTimer == 0 && dashMovementTimer == 0) moveWithCollision(dx * speed, dy * speed, map, enemies);

        // --- Block mechanism ---
        boolean defensePressed = input.defense();
        isBlocking = defensePressed && stamina > 0; // Will check actual stamina cost when damage is received

        if (isBlocking && input.attack()) {
            isBlocking = false;
        }

        // --- Attack ---
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;

        // Update attack swing animation
        if (attackSwingPhase > 0) attackSwingPhase--;
        if (screenShakeTimer > 0) screenShakeTimer--;

        // Update dash trail effect
        if (dashTrailTimer > 0) dashTrailTimer--;

        // Update shadow trail timers
        for (int i = 0; i < MAX_SHADOW_TRAILS; i++) {
            if (shadowTrailTimer[i] > 0) {
                shadowTrailTimer[i]--;
            }
        }

        // Update arrow projectile (if in flight)
        if (arrowTimer > 0) {
            arrowTimer--;

            // Calculate current arrow position (linear interpolation)
            double progress = 1.0 - (double) arrowTimer / arrowTravelTicks;
            double currentArrowX = arrowX + (arrowTargetX - arrowX) * progress;
            double currentArrowY = arrowY + (arrowTargetY - arrowY) * progress;

            // Check collision with enemies
            double dmg = getEffectiveAttackDamage();
            for (Entity e : enemies) {
                if (e.isAlive()) {
                    double dist = Math.hypot(currentArrowX - e.getX(), currentArrowY - e.getY());
                    if (dist <= ARROW_COLLISION_RADIUS) {
                        e.damage(dmg);
                        e.applyKnockback(arrowX, arrowY); // Knockback from arrow origin
                        arrowTimer = 0; // Arrow consumed on hit
                        AudioManager.playSound("slash-hit.wav", -15.0f);
                        break; // Arrow stops after first hit
                    }
                }
            }
        }

        // Handle dash movement
        if (dashMovementTimer > 0) {
            dashMovementTimer--;

            // Move towards dash target
            double newX = x + dashDirectionX;
            double newY = y + dashDirectionY;

            // Check collision for this movement step
            if (!collidesWithMap(newX, newY, map)) {
                x = newX;
                y = newY;

                // Create shadow trail at every frame for maximum visibility
                addShadowTrail(x, y);
                shadowTrailCounter++;
            } else {
                // Hit something during dash, stop movement
                dashMovementTimer = 0;
            }

            // Dash movement complete
            if (dashMovementTimer == 0) {
                dashTriggered = false;
                isInvulnerable = false; // End invulnerability
            }
        }

        if (input.attack() && attackCooldown == 0 && attackTimer == 0 && stamina > 0 && !isBlocking) {
            stamina -= 0.5;
            Weapon currentWeapon = getWeapon();
            attackTimer = scaleFrom60(currentWeapon.getDuration());
            attackCooldown = scaleFrom60(currentWeapon.getCooldown());

            // Branch based on weapon type
            if (currentWeapon.getType() == Weapon.WeaponType.BOW) {
                // BOW ATTACK: Fire arrow projectile
                performBowAttack(aimPoint, enemies);
            } else {
                // SWORD ATTACK: Immediate melee collision (existing code)
                performSwordAttack(map, enemies);
            }
        }

        // --- Advance local animation clock (60 Hz sim) ---
        animTimeMs += TICK_MS;
    }

    private void performSwordAttack(TileMap map, List<Entity> enemies) {
        // Initialize attack swing animation
        attackSwingPhase = ATTACK_SWING_DURATION;
        attackSwingAngle = facingAngle;

        // Get swing shape and check collisions
        Shape swing = getSwordSwingShape();
        boolean hitSomething = false;

        double dmg = getEffectiveAttackDamage();

        // Damage enemies
        for (Entity e : enemies) {
            if (e.isAlive() && swing.intersects(e.getBounds())) {
                e.damage(dmg);
                e.applyKnockback(x, y);
                hitSomething = true;
            }
        }

        // Damage breakable walls
        if (damageWallsInShape(swing, map, dmg)) hitSomething = true;

        // Play sound effects
        if (hitSomething) AudioManager.playSound("slash-hit.wav", -15.0f);
        else AudioManager.playSound("slash-clean.wav");
    }

    private void performBowAttack(Point aimPoint, List<Entity> enemies) {
        // Arrow starts at player position
        arrowX = x;
        arrowY = y;

        int range = getWeapon().getReach();

        // Determine target position
        if (aimPoint != null) {
            // Use mouse aim if available, but clamp to weapon range
            double dx = aimPoint.x - x;
            double dy = aimPoint.y - y;
            double distance = Math.hypot(dx, dy);

            if (distance > range) {
                // Clamp to max range in the aim direction
                double angle = Math.atan2(dy, dx);
                arrowTargetX = x + Math.cos(angle) * range;
                arrowTargetY = y + Math.sin(angle) * range;
            } else {
                arrowTargetX = aimPoint.x;
                arrowTargetY = aimPoint.y;
            }
        } else {
            // Use facing direction with max range
            arrowTargetX = x + Math.cos(facingAngle) * range;
            arrowTargetY = y + Math.sin(facingAngle) * range;
        }

        // Start arrow flight with duration-based travel time
        arrowTravelTicks = scaleFrom60(getWeapon().getDuration());
        arrowTimer = arrowTravelTicks;

        // Optional: Play bow sound effect
        AudioManager.playSound("slash-clean.wav"); // TODO: Add bow sound
    }

    private static int scaleFrom60(int ticksAt60) {
        return (int) Math.round(ticksAt60 * (TICKS_PER_SECOND / 60.0));
    }

    private void moveWithCollision(double dx, double dy, TileMap map, List<Entity> enemies) {
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

    private boolean collidesWithEnemies(double cx, double cy, List<Entity> enemies) {
        Rectangle playerBounds = getBoundsAt(cx, cy);
        for (Entity enemy : enemies) {
            if (enemy.isAlive() && playerBounds.intersects(enemy.getBounds())) return true;
        }
        return false;
    }

    private Shape getSwordSwingShape() {
        Weapon currentWeapon = getWeapon();
        int r = currentWeapon.getReach();
        int w = currentWeapon.getWidth();

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
        draw(g2, camX, camY, null);
    }

    public void draw(Graphics2D g2, int camX, int camY, List<Entity> enemies) {
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

        // Draw block indicator when blocking
        if (isBlocking) {
            int px = (int) Math.round(x) - camX;
            int py = (int) Math.round(y) - camY;

            // Draw shield-like effect around the player
            g2.setColor(new Color(100, 150, 255, 120)); // Blue shield with transparency
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(px - width / 2 - 4, py - height / 2 - 4, width + 8, height + 8);

            // Draw inner shield ring
            g2.setColor(new Color(150, 200, 255, 80));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(px - width / 2 - 2, py - height / 2 - 2, width + 4, height + 4);
        }

        // Draw arrow projectile if in flight
        if (arrowTimer > 0) {
            double progress = 1.0 - (double) arrowTimer / arrowTravelTicks;
            double currentArrowX = arrowX + (arrowTargetX - arrowX) * progress;
            double currentArrowY = arrowY + (arrowTargetY - arrowY) * progress;

            int arrowScreenX = (int) Math.round(currentArrowX) - camX;
            int arrowScreenY = (int) Math.round(currentArrowY) - camY;

            // Draw arrow as crosshair (yellow)
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2f));
            int arrowSize = 5;
            g2.drawLine(arrowScreenX - arrowSize, arrowScreenY, arrowScreenX + arrowSize, arrowScreenY);
            g2.drawLine(arrowScreenX, arrowScreenY - arrowSize, arrowScreenX, arrowScreenY + arrowSize);
        }

        if (attackTimer > 0 && getWeapon().getType() != Weapon.WeaponType.BOW) {
            // Enhanced sword attack animation with swing phase (only for non-bow weapons)
            Weapon currentWeapon = getWeapon();
            int r = currentWeapon.getReach();
            int w = currentWeapon.getWidth();

            // Calculate swing animation based on phase
            double swingProgress = 1.0 - (double) attackSwingPhase / ATTACK_SWING_DURATION;
            double swingOffset = SWING_ARC_RADIANS * swingProgress;
            double theta = attackSwingAngle - SWING_ARC_RADIANS / 2 + swingOffset;

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

            BufferedImage sword = TextureManager.getSwordTexture();
            if (sword != null) {
                AffineTransform at = new AffineTransform();
                at.translate(cxBlade - camX, cyBlade - camY);
                at.rotate(theta);
                double scaleX = r / (double) sword.getWidth();
                double scaleY = w / (double) sword.getHeight();
                at.scale(scaleX, scaleY);
                at.translate(-sword.getWidth() / 2.0, -sword.getHeight() / 2.0);
                g2.drawImage(sword, at, null);

                // Enhanced swing highlight with animation
                Shape swingWorld = getSwordSwingShape();
                Shape swing = AffineTransform.getTranslateInstance(-camX, -camY).createTransformedShape(swingWorld);

                // Animated swing trail effect
                int alpha = (int) (180 * swingProgress);
                g2.setColor(new Color(255, 255, 200, alpha));
                g2.fill(swing);

                // Animated swing outline
                g2.setColor(new Color(255, 255, 255, alpha + 40));
                g2.setStroke(new BasicStroke(3f * (float) swingProgress));
                g2.draw(swing);

                // Dynamic swing trail effect
                if (swingProgress > 0.3) {
                    int trailAlpha = (int) (100 * swingProgress);
                    g2.setColor(new Color(255, 255, 150, trailAlpha));
                    g2.setStroke(new BasicStroke(1f));

                    // Draw multiple trail lines
                    for (int i = 1; i <= MAX_TRAIL_COUNT; i++) {
                        double trailProgress = swingProgress - (i * SWING_TRAIL_SPACING);
                        if (trailProgress > 0) {
                            double trailAngle = attackSwingAngle - SWING_ARC_RADIANS / 2 + (SWING_ARC_RADIANS * trailProgress);
                            double trailCos = Math.cos(trailAngle), trailSin = Math.sin(trailAngle);
                            double trailAx = cx + trailCos * edge;
                            double trailAy = cy + trailSin * edge;
                            double trailCxBlade = trailAx + trailCos * (r / 2.0);
                            double trailCyBlade = trailAy + trailSin * (r / 2.0);

                            Rectangle2D.Double trailBlade = new Rectangle2D.Double(-r / 2.0, -w / 2.0, r, w);
                            AffineTransform trailAt = new AffineTransform();
                            trailAt.translate(trailCxBlade - camX, trailCyBlade - camY);
                            trailAt.rotate(trailAngle);
                            Shape trailShape = trailAt.createTransformedShape(trailBlade);
                            Shape trailScreen = AffineTransform.getTranslateInstance(-camX, -camY).createTransformedShape(trailShape);

                            g2.setColor(new Color(255, 255, 150, (int) (trailAlpha * trailProgress)));
                            g2.draw(trailScreen);
                        }
                    }
                }
            } else {
                // Fallback to original debug swing visuals
                Shape swingWorld = getSwordSwingShape();
                Shape swing = AffineTransform.getTranslateInstance(-camX, -camY).createTransformedShape(swingWorld);

                g2.setColor(new Color(255, 255, 180, 180));
                g2.fill(swing);
                g2.setColor(new Color(255, 255, 255, 220));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(swing);
            }
        }

        // Aim direction indicator
        if (aimIndicatorAlpha > 0 && lastAimPoint != null) {
            int px = (int) Math.round(x) - camX;
            int py = (int) Math.round(y) - camY;
            int ax = lastAimPoint.x - camX;
            int ay = lastAimPoint.y - camY;

            // Calculate alpha for fade effect
            float alpha = (float) aimIndicatorAlpha / AIM_INDICATOR_FADE_TIME;

            // Draw aim line
            g2.setColor(new Color(255, 255, 0, (int) (180 * alpha)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(px, py, ax, ay);

            // Draw aim crosshair at mouse position
            g2.setColor(new Color(255, 255, 0, (int) (200 * alpha)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(ax - CROSSHAIR_SIZE, ay, ax + CROSSHAIR_SIZE, ay);
            g2.drawLine(ax, ay - CROSSHAIR_SIZE, ax, ay + CROSSHAIR_SIZE);

            // Draw aim circle around player
            int aimRadius = Math.max(width, height) / 2 + 4;
            g2.setColor(new Color(255, 255, 0, (int) (60 * alpha)));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(px - aimRadius, py - aimRadius, aimRadius * 2, aimRadius * 2);

            // Draw weapon range indicator
            Weapon currentWeapon = getWeapon();
            int weaponRange = currentWeapon.getReach();
            g2.setColor(new Color(255, 255, 0, (int) (30 * alpha)));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(px - weaponRange, py - weaponRange, weaponRange * 2, weaponRange * 2);

            // Draw range markers at cardinal directions
            g2.setColor(new Color(255, 255, 0, (int) (80 * alpha)));
            g2.setStroke(new BasicStroke(1f));
            int markerSize = 3;
            g2.drawLine(px - weaponRange, py - markerSize, px - weaponRange, py + markerSize);
            g2.drawLine(px + weaponRange, py - markerSize, px + weaponRange, py + markerSize);
            g2.drawLine(px - markerSize, py - weaponRange, px + markerSize, py - weaponRange);
            g2.drawLine(px - markerSize, py + weaponRange, px + markerSize, py + weaponRange);

            // Draw aim assist indicator when close to enemies
            if (enemies != null) {
                for (Entity enemy : enemies) {
                    if (enemy.isAlive()) {
                        double distance = Math.hypot(enemy.getX() - x, enemy.getY() - y);
                        if (distance < currentWeapon.getReach() * ENEMY_HIGHLIGHT_RANGE_MULTIPLIER) {
                            int enemyScreenX = (int) Math.round(enemy.getX()) - camX;
                            int enemyScreenY = (int) Math.round(enemy.getY()) - camY;

                            // Draw enemy highlight when in range
                            g2.setColor(new Color(255, 100, 100, (int) (120 * alpha)));
                            g2.setStroke(new BasicStroke(2f));
                            g2.drawOval(enemyScreenX - ENEMY_HIGHLIGHT_SIZE, enemyScreenY - ENEMY_HIGHLIGHT_SIZE,
                                ENEMY_HIGHLIGHT_SIZE * 2, ENEMY_HIGHLIGHT_SIZE * 2);

                            // Draw line to enemy if it's the closest one
                            if (distance < currentWeapon.getReach()) {
                                g2.setColor(new Color(255, 100, 100, (int) (100 * alpha)));
                                g2.setStroke(new BasicStroke(1f));
                                g2.drawLine(px, py, enemyScreenX, enemyScreenY);
                            }
                        }
                    }
                }
            }
        }

        // tiny facing line (debug-only; disabled by default)
        if (Boolean.getBoolean("tlob.showFacingLine")) {
            g2.setColor(new Color(10, 40, 15));
            int px = (int) Math.round(x) - camX;
            int py = (int) Math.round(y) - camY;
            int len = Math.max(width, height) / 2 + 6;
            int tx = px + (int) Math.round(Math.cos(facingAngle) * len);
            int ty = py + (int) Math.round(Math.sin(facingAngle) * len);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(px, py, tx, ty);
        }
    }

    public void triggerLocalAttackFx() {
        Weapon currentWeapon = getWeapon();
        this.attackTimer = scaleFrom60(currentWeapon.getDuration());
        this.attackCooldown = scaleFrom60(currentWeapon.getCooldown());
    }

    public void tickClientAnimations(boolean moving) {
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer > 0) attackTimer--;
        if (attackSwingPhase > 0) attackSwingPhase--;
        if (screenShakeTimer > 0) screenShakeTimer--;
        this.movingThisTick = moving && (knockbackTimer == 0);
        this.animTimeMs += TICK_MS;
    }

    public Point getScreenShakeOffset() {
        if (screenShakeTimer <= 0) return new Point(0, 0);

        double intensity = (double) screenShakeTimer / SCREEN_SHAKE_DURATION;
        double shakeAmount = SCREEN_SHAKE_INTENSITY * intensity;

        // Create a subtle random shake pattern
        long seed = (long) (x * 1000 + y * 1000 + screenShakeTimer);
        Random shakeRng = new Random(seed);

        int shakeX = (int) (shakeRng.nextDouble() * shakeAmount - shakeAmount / 2);
        int shakeY = (int) (shakeRng.nextDouble() * shakeAmount - shakeAmount / 2);

        return new Point(shakeX, shakeY);
    }

    /**
     * Debug method to check screen shake status
     */
    public String getScreenShakeDebugInfo() {
        if (screenShakeTimer <= 0) {
            return "No screen shake active";
        }
        return "Screen shake active: Timer=" + screenShakeTimer + 
               "/" + SCREEN_SHAKE_DURATION + 
               ", Intensity=" + String.format("%.2f", (double) screenShakeTimer / SCREEN_SHAKE_DURATION);
    }

    @Override
    public void damage(double amount) {
        // Check if player is invulnerable (e.g., during dash)
        if (isInvulnerable) {
            return; // No damage taken while invulnerable
        }

        // Handle blocking
        double blockStaminaCost = amount * 0.5;
        if (isBlocking && stamina >= blockStaminaCost) {
            // Consume stamina for blocking (half of incoming damage)
            stamina -= blockStaminaCost;

            // Reduce damage by block reduction percentage
            double reducedDamage = amount * (1.0 - BLOCK_DAMAGE_REDUCTION);

            // Call parent damage method with reduced damage
            super.damage(reducedDamage);

            // Play block sound effect (if available)
            AudioManager.playSound("slash-hit.wav", -20.0f); // Quieter than normal hit

            // Add visual feedback for successful block
            screenShakeTimer = SCREEN_SHAKE_DURATION / 2; // Reduced screen shake for blocks
        } else {
            // Normal damage handling
            super.damage(amount);

            // Add screen shake when player takes damage
            screenShakeTimer = SCREEN_SHAKE_DURATION;

            // Play audio effects
            if (isAlive()) AudioManager.playSound("hero-hurt.wav", -10);
            else AudioManager.playSound("hero-death.wav");
        }
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

    /**
     * Gets the player's inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Gets the currently equipped weapon from inventory.
     * Overrides Entity.getWeapon() to use inventory system.
     */
    @Override
    public Weapon getWeapon() {
        if (inventory != null) {
            Weapon invWeapon = inventory.getCurrentWeapon();
            if (invWeapon != null) {
                return invWeapon;
            }
        }
        // Fallback to parent's weapon field if inventory not initialized
        return weapon;
    }
}
