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
import java.util.Random;

public class Player extends Entity {

    private static final int PLAYER_SIZE = 22;
    private static final double PLAYER_SPEED = 3;
    private static final double PLAYER_MAX_HP = 6.0;
    private static final double PLAYER_MAX_STAMINA = 6.0;
    private static final double PLAYER_MAX_MANA = 0.0;
    private static final double PLAYER_MAX_SHIELD = 0;
    private static final double PLAYER_SPEED_PPS = 90.0;

    private static final int TICKS_PER_SECOND = 60;
    private static final int TICK_MS = 1000 / TICKS_PER_SECOND;

    private static final int STAMINA_DRAIN_INTERVAL = TICKS_PER_SECOND;
    private static final int STAMINA_REGEN_INTERVAL = TICKS_PER_SECOND * 3;
    private static final int MANA_REGEN_INTERVAL = TICKS_PER_SECOND * 2;

    // Dash ability constants
    private static final double DASH_MANA_COST = 2.0;
    private static final double DASH_DISTANCE = 100.0; // blocks - increased for visibility
    private static final int DASH_COOLDOWN_TICKS = TICKS_PER_SECOND * 2; // 2 seconds
    private static final int DASH_MOVEMENT_TICKS = 5; // frames to complete dash movement

    private int staminaDrainCounter = 0;
    private int staminaRegenCounter = 0;
    private double staminaRegenRateMult = 1.0;
    private int manaRegenCounter = 0;
    private boolean wasSprinting = false;
    private double facingAngle = 0.0;

    // Dash ability variables
    private boolean wasShiftPressed = false;
    private boolean dashTriggered = false;
    private int dashCooldownTimer = 0;
    private boolean canDash = true;
    
    // Dash trail effect variables
    private int dashTrailTimer = 0;
    private double dashStartX = 0;
    private double dashStartY = 0;
    private static final int DASH_TRAIL_DURATION = 10; // frames
    
    // Dash movement variables
    private int dashMovementTimer = 0;
    private double dashTargetX = 0;
    private double dashTargetY = 0;
    private double dashDirectionX = 0;
    private double dashDirectionY = 0;
    
    // Dash shadow trail system
    private static final int MAX_SHADOW_TRAILS = 8;
    private static final int SHADOW_TRAIL_INTERVAL = 1; // frames between shadows
    private int shadowTrailCounter = 0;
    private final double[] shadowTrailX = new double[MAX_SHADOW_TRAILS];
    private final double[] shadowTrailY = new double[MAX_SHADOW_TRAILS];
    private final int[] shadowTrailTimer = new int[MAX_SHADOW_TRAILS];
    private static final int SHADOW_TRAIL_DURATION = 20; // frames shadows last (increased for visibility)
    
    // Dash invulnerability
    private boolean isInvulnerable = false;

    private int attackTimer = 0;
    private int attackCooldown = 0;
    private double speedMultiplier = 1.0;
    private double damageMultiplier = 1.0;

    private long animTimeMs = 0L;
    private boolean movingThisTick = false;
    
    // Aim direction and attack animation enhancements
    private Point lastAimPoint = null;
    private int aimIndicatorAlpha = 0;
    private int attackSwingPhase = 0;
    private double attackSwingAngle = 0.0;
    private int screenShakeTimer = 0;
    private static final int AIM_INDICATOR_FADE_TIME = 60; // frames
    private static final int ATTACK_SWING_DURATION = 10; // frames
    private static final int SCREEN_SHAKE_DURATION = 8; // frames
    
    // Animation and visual constants
    private static final double SWING_ARC_RADIANS = Math.PI / 2.0; // 60 degrees
    private static final double SWING_TRAIL_SPACING = 0.1; // trail effect spacing
    private static final int MAX_TRAIL_COUNT = 3; // number of trail effects
    private static final int CROSSHAIR_SIZE = 8;
    private static final int ENEMY_HIGHLIGHT_SIZE = 12;
    private static final double ENEMY_HIGHLIGHT_RANGE_MULTIPLIER = 1.5;
    private static final double SCREEN_SHAKE_INTENSITY = 2.0;

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
        updateFacingFromAngle(signed);
    }
    
    /**
     * Updates facing direction based on angle octant
     * @param octant signed octant value [-4..4]
     */
    private void updateFacingFromAngle(int octant) {
        switch (octant) {
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
        // Find an available slot for the shadow trail
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
        
        // Update aim direction tracking
        if (aimPoint != null) {
            lastAimPoint = aimPoint;
            aimIndicatorAlpha = AIM_INDICATOR_FADE_TIME;
        } else if (aimIndicatorAlpha > 0) {
            aimIndicatorAlpha--;
        }

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
            dx *= inv; dy *= inv;
        }

        movingThisTick = (dxRaw != 0 || dyRaw != 0) && knockbackTimer == 0;

        // --- Dash ability and Sprint / stamina ---
        boolean shiftPressed = input.sprint();
        boolean sprinting = false;
        double speedBase = effectiveBaseSpeed();
        
        // Update dash cooldown
        if (dashCooldownTimer > 0) {
            dashCooldownTimer--;
            if (dashCooldownTimer == 0) {
                canDash = true;
            }
        }
        
        // Handle dash ability
        if (shiftPressed && !wasShiftPressed && canDash && mana >= DASH_MANA_COST && (dxRaw != 0 || dyRaw != 0)) {
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
        
        // Handle sprinting (only if not dashing and has stamina)
        if (shiftPressed && stamina >= 1.0 && !dashTriggered && dashMovementTimer == 0) {
            sprinting = true;
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
        
        // Independent mana regeneration (separate from stamina)
        if (++manaRegenCounter >= MANA_REGEN_INTERVAL) {
            if (mana < maxMana) {
                mana = Math.min(maxMana, mana + 0.5);
            }
            manaRegenCounter = 0;
        }
        
        // Reset dash trigger when shift is released and dash movement is complete
        if (!shiftPressed && dashMovementTimer == 0) {
            dashTriggered = false;
        }
        
        wasShiftPressed = shiftPressed;
        wasSprinting = sprinting;

        // --- Knockback & movement ---
        updateKnockbackWithMap(map);
        if (knockbackTimer == 0 && dashMovementTimer == 0) moveWithCollision(dx * speed, dy * speed, map, enemies);

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

        if (input.attack() && attackCooldown == 0 && attackTimer == 0 && stamina > 0) {
            stamina -= 0.5;
            attackTimer = scaleFrom60(weapon.getDuration());
            attackCooldown = scaleFrom60(weapon.getCooldown());
            
            // Initialize attack swing animation
            attackSwingPhase = ATTACK_SWING_DURATION;
            attackSwingAngle = facingAngle;
            screenShakeTimer = SCREEN_SHAKE_DURATION;

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
        draw(g2, camX, camY, null);
    }
    
    public void draw(Graphics2D g2, int camX, int camY, List<Enemy> enemies) {
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
            // Enhanced sword attack animation with swing phase
            int r = weapon.getReach();
            int w = weapon.getWidth();
            
            // Calculate swing animation based on phase
            double swingProgress = 1.0 - (double) attackSwingPhase / ATTACK_SWING_DURATION;
            double swingOffset = SWING_ARC_RADIANS * swingProgress;
            double theta = attackSwingAngle - SWING_ARC_RADIANS/2 + swingOffset;
            
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
                            double trailAngle = attackSwingAngle - SWING_ARC_RADIANS/2 + (SWING_ARC_RADIANS * trailProgress);
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
            int weaponRange = weapon.getReach();
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
                for (Enemy enemy : enemies) {
                    if (enemy.isAlive()) {
                        double distance = Math.hypot(enemy.getX() - x, enemy.getY() - y);
                        if (distance < weapon.getReach() * ENEMY_HIGHLIGHT_RANGE_MULTIPLIER) {
                            int enemyScreenX = (int) Math.round(enemy.getX()) - camX;
                            int enemyScreenY = (int) Math.round(enemy.getY()) - camY;
                            
                            // Draw enemy highlight when in range
                            g2.setColor(new Color(255, 100, 100, (int) (120 * alpha)));
                            g2.setStroke(new BasicStroke(2f));
                            g2.drawOval(enemyScreenX - ENEMY_HIGHLIGHT_SIZE, enemyScreenY - ENEMY_HIGHLIGHT_SIZE, 
                                       ENEMY_HIGHLIGHT_SIZE * 2, ENEMY_HIGHLIGHT_SIZE * 2);
                            
                            // Draw line to enemy if it's the closest one
                            if (distance < weapon.getReach()) {
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
        this.attackTimer = scaleFrom60(weapon.getDuration());
        this.attackCooldown = scaleFrom60(weapon.getCooldown());
    }

    public void tickClientAnimations(boolean moving) {
        if (attackCooldown > 0) attackCooldown--;
        if (attackTimer   > 0) attackTimer--;
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
        
        int shakeX = (int) (shakeRng.nextDouble() * shakeAmount - shakeAmount/2);
        int shakeY = (int) (shakeRng.nextDouble() * shakeAmount - shakeAmount/2);
        
        return new Point(shakeX, shakeY);
    }

    @Override
    public void damage(double amount) {
        // Check if player is invulnerable (e.g., during dash)
        if (isInvulnerable) {
            return; // No damage taken while invulnerable
        }
        
        // Call parent damage method
        super.damage(amount);
        
        // Play audio effects
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
