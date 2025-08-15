package com.lhamacorp.games.tlob.client.managers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class TextureManager {

    // ===== Asset directory =====
    private static final String ASSETS_DIR = "assets";
    
    // ===== Biome-specific texture file patterns =====
    private static final String BIOME_TEXTURE_PATTERN = ASSETS_DIR + "/%s_%s.png";
    
    // ===== Legacy texture files (for backward compatibility) =====
    private static final String GRASS_FILE = ASSETS_DIR + "/grass.png";
    private static final String STONE_FILE = ASSETS_DIR + "/stone.png";
    // Treat these as sprite sheets now (4x4). If only a single frame exists, we degrade gracefully.

    // ===== Tile sizes (keep your existing sizes) =====
    private static final int PLAYER_W = 22, PLAYER_H = 22;
    private static final int ENEMY_W = 20, ENEMY_H = 20;

    // ===== Animation layout =====
    private static final int FRAMES_PER_ROW = 4;  // walk cycle frames
    private static final int ROWS_PER_SHEET = 4;  // DOWN, LEFT, RIGHT, UP
    private static final int DEFAULT_FRAME_DURATION_MS = 120;

    // ===== Cached textures =====
    private static final boolean ANIMATE_GRASS = Boolean.getBoolean("tlob.anim.grass");
    private static final int GRASS_FRAMES = 4;
    private static BufferedImage[] grassFrames;
    private static BufferedImage grassTexture;
    private static BufferedImage stoneTexture;
    private static BufferedImage dirtTexture;
    private static BufferedImage plantsTexture;
    private static BufferedImage swordTexture;
    
    // ===== Biome-specific textures =====
    
    // Meadows biome textures
    private static BufferedImage meadowGrassTexture;
    private static BufferedImage meadowDirtTexture;
    private static BufferedImage meadowPlantsTexture;
    private static BufferedImage meadowFlowersTexture;
    
    // Forest biome textures
    private static BufferedImage forestTreeTexture;
    private static BufferedImage forestLogTexture;
    private static BufferedImage forestGroundTexture;
    private static BufferedImage forestLeavesTexture;
    private static BufferedImage forestMushroomsTexture;
    
    // Cave biome textures
    private static BufferedImage caveStoneTexture;
    private static BufferedImage caveCrystalTexture;
    private static BufferedImage caveWaterTexture;
    private static BufferedImage caveStalagmiteTexture;
    private static BufferedImage caveCrystalFormationTexture;
    
    // Desert biome textures
    private static BufferedImage desertSandTexture;
    private static BufferedImage desertRockTexture;
    private static BufferedImage desertCactusTexture;
    private static BufferedImage desertRockFormationTexture;
    
    // Vulcan biome textures
    private static BufferedImage vulcanRockTexture;
    private static BufferedImage vulcanLavaTexture;
    private static BufferedImage vulcanAshTexture;
    private static BufferedImage vulcanCrystalTexture;

    // For legacy compatibility (first frame, idle/down)
    private static BufferedImage playerFirstFrame;
    private static BufferedImage enemyFirstFrame;

    // Animation caches
    private static Map<Key, SpriteAnimation> playerAnimations;
    private static Map<Key, SpriteAnimation> enemyAnimations;

    private static boolean loaded;

    private TextureManager() {
    }

    // ===== Public API (unchanged grass/stone) =====
    public static BufferedImage getGrassTexture() {
        ensureLoaded();
        return grassTexture;
    }

    public static BufferedImage getStoneTexture() {
        ensureLoaded();
        return stoneTexture;
    }

    public static BufferedImage getDirtTexture() {
        ensureLoaded();
        return dirtTexture;
    }

    public static BufferedImage getPlantsTexture() {
        ensureLoaded();
        return plantsTexture;
    }

    public static BufferedImage getSwordTexture() {
        ensureLoaded();
        return swordTexture;
    }
    
    // ===== Biome-specific texture getters =====
    
    // Meadows biome textures
    public static BufferedImage getMeadowGrassTexture() {
        ensureLoaded();
        return meadowGrassTexture;
    }
    
    public static BufferedImage getMeadowDirtTexture() {
        ensureLoaded();
        return meadowDirtTexture;
    }
    
    public static BufferedImage getMeadowPlantsTexture() {
        ensureLoaded();
        return meadowPlantsTexture;
    }
    
    public static BufferedImage getMeadowFlowersTexture() {
        ensureLoaded();
        return meadowFlowersTexture;
    }
    
    // Forest biome textures
    public static BufferedImage getForestTreeTexture() {
        ensureLoaded();
        return forestTreeTexture;
    }
    
    public static BufferedImage getForestLogTexture() {
        ensureLoaded();
        return forestLogTexture;
    }
    
    public static BufferedImage getForestGroundTexture() {
        ensureLoaded();
        return forestGroundTexture;
    }
    
    public static BufferedImage getForestLeavesTexture() {
        ensureLoaded();
        return forestLeavesTexture;
    }
    
    public static BufferedImage getForestMushroomsTexture() {
        ensureLoaded();
        return forestMushroomsTexture;
    }
    
    // Cave biome textures
    public static BufferedImage getCaveStoneTexture() {
        ensureLoaded();
        return caveStoneTexture;
    }
    
    public static BufferedImage getCaveCrystalTexture() {
        ensureLoaded();
        return caveCrystalTexture;
    }
    
    public static BufferedImage getCaveWaterTexture() {
        ensureLoaded();
        return caveWaterTexture;
    }
    
    public static BufferedImage getCaveStalagmiteTexture() {
        ensureLoaded();
        return caveStalagmiteTexture;
    }
    
    public static BufferedImage getCaveCrystalFormationTexture() {
        ensureLoaded();
        return caveCrystalFormationTexture;
    }
    
    // Desert biome textures
    public static BufferedImage getDesertSandTexture() {
        ensureLoaded();
        return desertSandTexture;
    }
    
    public static BufferedImage getDesertRockTexture() {
        ensureLoaded();
        return desertRockTexture;
    }
    
    public static BufferedImage getDesertCactusTexture() {
        ensureLoaded();
        return desertCactusTexture;
    }
    
    public static BufferedImage getDesertRockFormationTexture() {
        ensureLoaded();
        return desertRockFormationTexture;
    }
    
    // Vulcan biome textures
    public static BufferedImage getVulcanRockTexture() {
        ensureLoaded();
        return vulcanRockTexture;
    }
    
    public static BufferedImage getVulcanLavaTexture() {
        ensureLoaded();
        return vulcanLavaTexture;
    }
    
    public static BufferedImage getVulcanAshTexture() {
        ensureLoaded();
        return vulcanAshTexture;
    }
    
    public static BufferedImage getVulcanCrystalTexture() {
        ensureLoaded();
        return vulcanCrystalTexture;
    }

    // Legacy getters (return idle/down frame)
    public static BufferedImage getPlayerTexture() {
        ensureLoaded();
        return playerFirstFrame;
    }

    public static BufferedImage getEnemyTexture() {
        ensureLoaded();
        return enemyFirstFrame;
    }

    // ===== New animation API =====
    public enum Direction {DOWN, LEFT, RIGHT, UP}

    public enum Motion {IDLE, WALK}

    public static final class SpriteAnimation {
        private final BufferedImage[] frames;
        private final int frameDurationMs;

        private SpriteAnimation(BufferedImage[] frames, int frameDurationMs) {
            this.frames = frames;
            this.frameDurationMs = Math.max(40, frameDurationMs);
        }

        public BufferedImage frameAt(long elapsedMs) {
            if (frames.length == 0) return null;
            int idx = (int) ((elapsedMs / frameDurationMs) % frames.length);
            return frames[idx];
        }

        public BufferedImage[] frames() {
            return frames;
        }

        public int getFrameDurationMs() {
            return frameDurationMs;
        }

        public int length() {
            return frames.length;
        }
    }

    public static SpriteAnimation getPlayerAnimation(Direction dir, Motion motion) {
        ensureLoaded();
        return playerAnimations.get(new Key(motion, dir));
    }

    public static SpriteAnimation getEnemyAnimation(Direction dir, Motion motion) {
        ensureLoaded();
        return enemyAnimations.get(new Key(motion, dir));
    }

    public static BufferedImage getGrassTextureFrame(int tick60) {
        ensureLoaded();
        if (!ANIMATE_GRASS) return grassTexture;
        if (grassFrames == null || grassFrames.length == 0) return grassTexture;
        int idx = Math.floorMod(tick60 / 2, GRASS_FRAMES);
        return grassFrames[idx];
    }

    // ===== Internal load =====
    private static synchronized void ensureLoaded() {
        if (loaded) return;

        ClassLoader cl = TextureManager.class.getClassLoader();

        // Grass & Stone: now use biome-specific naming for meadows biome
        // Try to load meadows-specific textures first, fall back to legacy names
        grassTexture = tryLoad(cl.getResourceAsStream(getBiomeTextureFilename("meadows", "grass")));
        if (grassTexture == null) {
            grassTexture = tryLoad(cl.getResourceAsStream(GRASS_FILE));
        }
        if (grassTexture == null) {
            grassTexture = generateGrassTexture(32, 32);
            tryWrite(getBiomeTextureFilename("meadows", "grass"), grassTexture);
        }
        
        stoneTexture = tryLoad(cl.getResourceAsStream(getBiomeTextureFilename("meadows", "stone")));
        if (stoneTexture == null) {
            stoneTexture = tryLoad(cl.getResourceAsStream(STONE_FILE));
        }
        if (stoneTexture == null) {
            stoneTexture = generateStoneTexture(32, 32);
            tryWrite(getBiomeTextureFilename("meadows", "stone"), stoneTexture);
        }

        // Procedural textures for floor variants (no external files required)
        dirtTexture = generateDirtTexture(32, 32);
        plantsTexture = generatePlantsTexture(32, 32);
        swordTexture = generateSwordTexture(48, 16);
        
        // Generate biome-specific textures
        generateBiomeTextures();

        // Player / Enemy: sprite sheets (4x4). Fallback to procedural generation if missing.
        BufferedImage playerSheet = null;
        BufferedImage enemySheet = null;

        if (playerSheet == null || !looksLikeSheet(playerSheet, PLAYER_W, PLAYER_H)) {
            playerSheet = generatePlayerSheet(PLAYER_W, PLAYER_H, ROWS_PER_SHEET, FRAMES_PER_ROW);
        }
        if (enemySheet == null || !looksLikeSheet(enemySheet, ENEMY_W, ENEMY_H)) {
            enemySheet = generateEnemySheet(ENEMY_W, ENEMY_H, ROWS_PER_SHEET, FRAMES_PER_ROW);
        }

        playerAnimations = sliceIntoAnimations(playerSheet, PLAYER_W, PLAYER_H);
        enemyAnimations = sliceIntoAnimations(enemySheet, ENEMY_W, ENEMY_H);

        // Legacy first frame = idle/down (row 0, col 0)
        playerFirstFrame = playerAnimations.get(new Key(Motion.IDLE, Direction.DOWN)).frames()[0];
        enemyFirstFrame = enemyAnimations.get(new Key(Motion.IDLE, Direction.DOWN)).frames()[0];

        // Build animated grass frames only if enabled
        if (ANIMATE_GRASS && grassFrames == null) {
            grassFrames = new BufferedImage[GRASS_FRAMES];
            BufferedImage base = (grassTexture != null) ? grassTexture : generateGrassTexture(32, 32);
            grassFrames[0] = base;
            for (int i = 1; i < GRASS_FRAMES; i++) {
                grassFrames[i] = swayAndTintGrass(base, i);
            }
        }

        loaded = true;
    }

    private static BufferedImage swayAndTintGrass(BufferedImage base, int phase) {
        int w = base.getWidth(), h = base.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Sway amounts (-0.06 .. +0.06)
        double shearX = switch (phase % GRASS_FRAMES) {
            case 1 -> -0.04;
            case 2 -> 0.00;
            case 3 -> 0.04;
            default -> 0.00;
        };

        // Shear around vertical center to keep within bounds
        AffineTransform at = new AffineTransform();
        at.translate(-shearX * (h / 2.0), 0); // compensate x shift introduced by shear
        at.shear(shearX, 0);
        g.drawImage(base, at, null);

        // Tiny lighting change so it doesn’t look like a pure transform
        // (alternates a gentle highlight/darken overlay)
        int alpha = 18; // very subtle
        if (phase % 2 == 1) {
            g.setColor(new Color(255, 255, 255, alpha));
        } else {
            g.setColor(new Color(0, 0, 0, alpha));
        }
        g.fillRect(0, 0, w, h);

        g.dispose();
        return out;
    }

    private static boolean looksLikeSheet(BufferedImage img, int fw, int fh) {
        return img.getWidth() >= fw && img.getHeight() >= fh && (img.getWidth() % fw == 0) && (img.getHeight() % fh == 0);
    }

    private static Map<Key, SpriteAnimation> sliceIntoAnimations(BufferedImage sheet, int fw, int fh) {
        int cols = Math.max(1, sheet.getWidth() / fw);
        int rows = Math.max(1, sheet.getHeight() / fh);

        // Row order: DOWN, LEFT, RIGHT, UP (if fewer rows, we reuse what’s there)
        Direction[] rowDir = new Direction[]{Direction.DOWN, Direction.LEFT, Direction.RIGHT, Direction.UP};
        Map<Key, SpriteAnimation> map = new HashMap<>();

        for (int r = 0; r < Math.min(rows, ROWS_PER_SHEET); r++) {
            Direction dir = rowDir[r];
            BufferedImage[] walk = new BufferedImage[Math.max(1, cols)];
            for (int c = 0; c < walk.length; c++) {
                walk[c] = sheet.getSubimage(c * fw, r * fh, fw, fh);
            }
            // IDLE uses first column (single-frame)
            BufferedImage[] idle = new BufferedImage[]{walk[0]};

            map.put(new Key(Motion.IDLE, dir), new SpriteAnimation(idle, DEFAULT_FRAME_DURATION_MS));
            map.put(new Key(Motion.WALK, dir), new SpriteAnimation(walk, DEFAULT_FRAME_DURATION_MS));
        }

        // Fill missing directions by mirroring/rehusing DOWN if needed
        for (Direction d : Direction.values()) {
            map.computeIfAbsent(new Key(Motion.IDLE, d), k -> map.get(new Key(Motion.IDLE, Direction.DOWN)));
            map.computeIfAbsent(new Key(Motion.WALK, d), k -> map.get(new Key(Motion.WALK, Direction.DOWN)));
        }
        return map;
    }

    private record Key(Motion motion, Direction dir) {
    }

    // ===== IO helpers =====
    private static BufferedImage tryLoad(InputStream stream) {
        if (stream == null) return null;
        try {
            return ImageIO.read(stream);
        } catch (IOException e) {
            return null;
        }
    }

    private static void tryWrite(String path, BufferedImage img) {
        try {
            // Ensure directory exists when running from IDE
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            ImageIO.write(img, "png", f);
        } catch (IOException ignored) {
        }
    }
    
    /**
     * Generates a biome-specific texture filename.
     * @param biome The biome name (lowercase)
     * @param textureType The texture type (e.g., "grass", "stone", "dirt")
     * @return The filename for the biome texture
     */
    private static String getBiomeTextureFilename(String biome, String textureType) {
        return String.format(BIOME_TEXTURE_PATTERN, biome, textureType);
    }
    
    /**
     * Tries to load a biome-specific texture, falling back to generation if not found.
     * @param biome The biome name (lowercase)
     * @param textureType The texture type (e.g., "grass", "stone", "dirt")
     * @param generator Function to generate the texture if loading fails
     * @return The loaded or generated texture
     */
    private static BufferedImage loadOrGenerateBiomeTexture(String biome, String textureType, 
                                                           java.util.function.Supplier<BufferedImage> generator) {
        String filename = getBiomeTextureFilename(biome, textureType);
        
        // Try to load from file first
        BufferedImage texture = tryLoad(TextureManager.class.getClassLoader().getResourceAsStream(filename));
        
        if (texture == null) {
            // Generate if not found
            texture = generator.get();
            // Save the generated texture
            tryWrite(filename, texture);
        }
        
        return texture;
    }

    // ===== Unchanged grass/stone generation =====
    private static BufferedImage generateGrassTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(70, 120, 70));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < w * h / 12; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int len = 2 + (int) (Math.random() * 3);
            g.setColor(new Color(60 + (int) (Math.random() * 30), 100 + (int) (Math.random() * 40), 60 + (int) (Math.random() * 30)));
            g.drawLine(x, y, x, Math.max(0, y - len));
        }
        g.setColor(new Color(0, 0, 0, 20));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage generateStoneTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(120, 120, 130));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < w * h / 10; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int a = 40 + (int) (Math.random() * 60);
            int c = 90 + (int) (Math.random() * 70);
            g.setColor(new Color(c, c, c, a));
            g.fillRect(x, y, 1, 1);
        }
        g.setColor(new Color(60, 60, 70, 80));
        for (int i = 0; i < 3; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int segments = 4 + (int) (Math.random() * 4);
            int px = x, py = y;
            for (int s = 0; s < segments; s++) {
                int nx = px + (int) (Math.random() * 7 - 3);
                int ny = py + (int) (Math.random() * 7 - 3);
                g.drawLine(px, py, nx, ny);
                px = nx;
                py = ny;
            }
        }
        g.setColor(new Color(0, 0, 0, 25));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage generateDirtTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(124, 93, 62)); // base dirt
        g.fillRect(0, 0, w, h);
        // speckles
        for (int i = 0; i < w * h / 8; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int a = 40 + (int) (Math.random() * 60);
            int r = 90 + (int) (Math.random() * 40);
            int gch = 60 + (int) (Math.random() * 30);
            int b = 40 + (int) (Math.random() * 20);
            g.setColor(new Color(r, gch, b, a));
            g.fillRect(x, y, 1, 1);
        }
        g.setColor(new Color(0, 0, 0, 25));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage generatePlantsTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(58, 94, 58)); // darker base
        g.fillRect(0, 0, w, h);
        // small leaf clusters
        for (int i = 0; i < w * h / 20; i++) {
            int x = (int) (Math.random() * (w - 4));
            int y = (int) (Math.random() * (h - 4));
            Color leaf1 = new Color(76, 132, 76);
            Color leaf2 = new Color(96, 160, 96);
            g.setColor(leaf1);
            g.fillOval(x, y + 2, 3, 2);
            g.fillOval(x + 2, y, 2, 3);
            g.setColor(leaf2);
            g.fillOval(x + 1, y + 1, 3, 2);
        }
        // subtle stems
        g.setColor(new Color(40, 70, 40, 120));
        for (int i = 0; i < 6; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int len = 2 + (int) (Math.random() * 4);
            g.drawLine(x, y, x, Math.max(0, y - len));
        }
        g.setColor(new Color(0, 0, 0, 25));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage generateSwordTexture(int w, int h) {
        // Create a horizontal sword pointing to the right.
        // Overall canvas w x h, with transparent background.
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Base measurements relative to h
        int bladeH = Math.max(3, (int) Math.round(h * 0.4));
        int guardH = Math.max(2, (int) Math.round(h * 0.6));
        int handleH = Math.max(2, (int) Math.round(h * 0.35));

        int centerY = h / 2;
        int bladeY = centerY - bladeH / 2;
        int guardY = centerY - guardH / 2;
        int handleY = centerY - handleH / 2;

        // Layout along X: [handle | guard | blade tip]
        int handleLen = Math.max(4, w / 6);
        int guardLen = Math.max(2, w / 16);
        int bladeLen = w - handleLen - guardLen - 2;

        int x0 = 1; // left padding
        int xHandleEnd = x0 + handleLen;
        int xGuardEnd = xHandleEnd + guardLen;
        int xBladeEnd = xGuardEnd + bladeLen;

        // Handle (brown) with darker outline
        g.setColor(new Color(92, 62, 36));
        g.fillRect(x0, handleY, handleLen, handleH);
        g.setColor(new Color(60, 40, 24));
        g.drawRect(x0, handleY, handleLen, handleH);

        // Pommel (small cap)
        g.setColor(new Color(80, 56, 32));
        g.fillRect(Math.max(0, x0 - 2), handleY + handleH / 4, 2, Math.max(2, handleH / 2));

        // Guard (gold) with enhanced detail
        g.setColor(new Color(196, 166, 66));
        g.fillRect(xHandleEnd, guardY, guardLen, guardH);
        g.setColor(new Color(140, 110, 40));
        g.drawRect(xHandleEnd, guardY, guardLen, guardH);
        
        // Guard center detail
        g.setColor(new Color(255, 215, 100));
        g.drawLine(xHandleEnd + guardLen/2, guardY, xHandleEnd + guardLen/2, guardY + guardH);

        // Blade base (light steel) with enhanced metallic look
        g.setColor(new Color(200, 210, 220));
        g.fillRect(xGuardEnd, bladeY, bladeLen, bladeH);

        // Blade center line and edges for pixel look
        g.setColor(new Color(160, 170, 178));
        g.drawLine(xGuardEnd, bladeY + bladeH / 2, xBladeEnd - 1, bladeY + bladeH / 2);
        g.setColor(new Color(140, 150, 158));
        g.drawLine(xGuardEnd, bladeY, xBladeEnd - 1, bladeY);
        g.drawLine(xGuardEnd, bladeY + bladeH - 1, xBladeEnd - 1, bladeY + bladeH - 1);
        
        // Blade shine effect
        g.setColor(new Color(255, 255, 255, 100));
        g.drawLine(xGuardEnd + 1, bladeY + 1, xBladeEnd - 2, bladeY + 1);

        // Tip (simple triangle-ish pixel tip)
        g.setColor(new Color(220, 230, 240));
        int tipW = Math.max(2, bladeH / 2);
        for (int i = 0; i < tipW; i++) {
            int yy = bladeY + i;
            int hh = bladeH - i * 2;
            if (hh <= 0) break;
            g.fillRect(xBladeEnd - tipW + i, yy, 1, hh);
        }
        
        // Add a subtle glow effect around the blade
        g.setColor(new Color(255, 255, 200, 30));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(xGuardEnd - 1, bladeY - 1, bladeLen + 2, bladeH + 2);

        g.dispose();
        return img;
    }

    // ===== Procedural player/enemy sprite sheets (4x4) =====
    private static BufferedImage generatePlayerSheet(int fw, int fh, int rows, int cols) {
        BufferedImage sheet = new BufferedImage(fw * cols, fh * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        // Pixel-art look: disable AA and use nearest neighbor when scaling by the game
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // A tiny 16-bit-like palette
        Color tunicMid = new Color(52, 168, 76);
        Color tunicDark = new Color(34, 120, 52);
        Color tunicLight = new Color(82, 198, 106);
        Color hatMid = new Color(36, 132, 58);
        Color hatDark = new Color(24, 96, 42);
        Color boots = new Color(84, 64, 40);
        Color bootsDark = new Color(64, 48, 30);
        Color gloves = new Color(64, 96, 64);
        Color skin = new Color(238, 205, 180);
        Color skinDark = new Color(206, 170, 148);
        Color hair = new Color(140, 100, 60);
        Color belt = new Color(60, 48, 36);
        Color buckle = new Color(228, 208, 120);
        Color line = new Color(0, 0, 0, 160);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x0 = c * fw, y0 = r * fh;
                BufferedImage frame = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
                Graphics2D f = frame.createGraphics();
                f.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                // Walk cycle phase
                int step = (c % 4); // 0..3
                int legSwing = (step == 1 || step == 3) ? 1 : -1;
                int armSwing = -legSwing;

                // Shadow (oval)
                f.setColor(new Color(0, 0, 0, 80));
                f.fillOval(3, fh - 6, fw - 6, 4);

                // Body base
                f.setColor(tunicMid);
                f.fillRoundRect(4, 7, fw - 8, fh - 12, 4, 4);
                // Tunic shading: dark at left/bottom, light at top-right
                f.setColor(tunicDark);
                f.fillRect(4, 7 + (fh - 12) - 5, fw - 8, 5);
                f.fillRect(4, 7, 2, fh - 12);
                f.setColor(tunicLight);
                f.fillRect(fw - 10, 9, 4, 3);

                // Belt + buckle
                f.setColor(belt);
                f.fillRect(4, 12, fw - 8, 2);
                f.setColor(buckle);
                f.fillRect(fw / 2 - 1, 12, 2, 2);

                // Head + hair/ears depend on facing
                // Base head
                f.setColor(skin);
                f.fillOval(6, 1, fw - 12, 10);
                // Hair back rim (shows more on UP)
                int hairY = (r == 3) ? 2 : 3;
                f.setColor(hair);
                f.fillRect(7, hairY, fw - 14, 2);
                // Ears for DOWN/LEFT/RIGHT
                if (r != 3) {
                    f.setColor(skinDark);
                    f.fillRect(5, 5, 2, 2);
                    f.fillRect(fw - 7, 5, 2, 2);
                }

                // Face details for DOWN only
                if (r == 0) {
                    f.setColor(new Color(20, 20, 20));
                    f.fillRect(9, 6, 2, 2); // left eye
                    f.fillRect(fw - 11, 6, 2, 2); // right eye
                }

                // Hat (direction-aware), 3-tone triangle-ish cap
                if (r == 0) { // DOWN
                    f.setColor(hatDark);
                    f.fillPolygon(new int[]{6, fw - 6, fw - 8}, new int[]{5, 5, 2}, 3);
                    f.setColor(hatMid);
                    f.fillPolygon(new int[]{6, fw - 6, fw - 9}, new int[]{5, 5, 3}, 3);
                } else if (r == 1) { // LEFT
                    f.setColor(hatDark);
                    f.fillPolygon(new int[]{6, 6, 2}, new int[]{5, 2, 7}, 3);
                    f.setColor(hatMid);
                    f.fillPolygon(new int[]{6, 6, 3}, new int[]{5, 3, 7}, 3);
                } else if (r == 2) { // RIGHT
                    f.setColor(hatDark);
                    f.fillPolygon(new int[]{fw - 6, fw - 6, fw - 2}, new int[]{5, 2, 7}, 3);
                    f.setColor(hatMid);
                    f.fillPolygon(new int[]{fw - 6, fw - 6, fw - 3}, new int[]{5, 3, 7}, 3);
                } else { // UP
                    f.setColor(hatDark);
                    f.fillPolygon(new int[]{6, fw - 6, 7}, new int[]{5, 5, 2}, 3);
                    f.setColor(hatMid);
                    f.fillPolygon(new int[]{6, fw - 6, 8}, new int[]{5, 5, 3}, 3);
                }

                // Arms + gloves (simple swing)
                f.setColor(gloves);
                int armY = 10 + armSwing;
                f.fillRect(3, armY, 3, 6);
                f.fillRect(fw - 6, 10 - armSwing, 3, 6);

                // Add small shield depending on facing (left hand)
                f.setColor(new Color(96, 120, 144)); // steel
                if (r == 0) { // DOWN
                    f.fillRect(2, 11, 3, 5);
                } else if (r == 1) { // LEFT
                    f.fillRect(1, 11, 3, 6);
                } else if (r == 2) { // RIGHT
                    f.fillRect(fw - 4, 11, 3, 6);
                } else { // UP
                    f.fillRect(2, 10, 3, 5);
                }

                // Legs (walk)
                f.setColor(boots);
                f.fillRect(5, fh - 9 + legSwing, 4, 6);
                f.setColor(bootsDark);
                f.fillRect(5, fh - 4 + legSwing, 4, 1);
                f.setColor(boots);
                f.fillRect(fw - 9, fh - 9 - legSwing, 4, 6);
                f.setColor(bootsDark);
                f.fillRect(fw - 9, fh - 4 - legSwing, 4, 1);

                // Simple scabbard on right hip (down/left/right)
                if (r != 3) {
                    f.setColor(new Color(120, 84, 48));
                    f.fillRect(fw - 7, 13, 2, 5);
                }

                // Outline
                f.setColor(line);
                f.drawRoundRect(4, 7, fw - 8, fh - 12, 4, 4);
                f.dispose();

                g.drawImage(frame, x0, y0, null);
            }
        }

        g.dispose();
        return sheet;
    }

    private static BufferedImage generateEnemySheet(int fw, int fh, int rows, int cols) {
        BufferedImage sheet = new BufferedImage(fw * cols, fh * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Enhanced enemy color palette
        Color armorDark = new Color(80, 80, 90);
        Color armorMid = new Color(120, 120, 130);
        Color armorLight = new Color(160, 160, 170);
        Color skin = new Color(180, 140, 100);
        Color eyeW = new Color(250, 250, 250);
        Color eyeB = new Color(20, 20, 20);
        Color weaponDark = new Color(100, 80, 60);
        Color weaponLight = new Color(140, 120, 100);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x0 = c * fw, y0 = r * fh;
                BufferedImage frame = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
                Graphics2D f = frame.createGraphics();
                f.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int step = c % 4;
                int wobble = (step == 0 || step == 2) ? 0 : 1;

                // Shadow
                f.setColor(new Color(0, 0, 0, 70));
                f.fillOval(3, fh - 6, fw - 6, 4);

                // Body (armor)
                f.setColor(armorDark);
                f.fillRoundRect(2, 6 + wobble, fw - 4, fh - 12, 6, 6);
                
                // Armor highlights
                f.setColor(armorMid);
                f.fillRoundRect(3, 7 + wobble, fw - 6, fh - 14, 4, 4);
                
                // Armor details
                f.setColor(armorLight);
                f.fillRect(4, 8 + wobble, fw - 8, 2); // chest plate
                f.fillRect(4, 12 + wobble, fw - 8, 2); // belt line

                // Head
                f.setColor(skin);
                f.fillOval(4, 2 + wobble, fw - 8, 8);
                
                // Helmet
                f.setColor(armorDark);
                f.fillOval(3, 1 + wobble, fw - 6, 10);
                f.setColor(armorMid);
                f.fillOval(4, 2 + wobble, fw - 8, 8);
                
                // Helmet visor
                f.setColor(armorDark);
                f.fillRect(5, 4 + wobble, fw - 10, 2);

                // Eyes (red glowing for enemies)
                f.setColor(new Color(200, 0, 0));
                f.fillOval(6, 5 + wobble, 3, 3);
                f.fillOval(fw - 9, 5 + wobble, 3, 3);
                f.setColor(new Color(255, 0, 0));
                f.fillOval(7, 6 + wobble, 1, 1);
                f.fillOval(fw - 8, 6 + wobble, 1, 1);

                // Weapon (sword)
                f.setColor(weaponDark);
                f.fillRect(fw - 2, fh/2 - 3, 6, 6);
                f.setColor(weaponLight);
                f.fillRect(fw - 1, fh/2 - 2, 4, 4);
                
                // Shield
                f.setColor(weaponDark);
                f.fillOval(0, fh/2 - 4, 8, 8);
                f.setColor(weaponLight);
                f.fillOval(1, fh/2 - 3, 6, 6);

                // Legs
                f.setColor(armorDark);
                f.fillRect(5, fh - 8 + wobble, 3, 6);
                f.fillRect(fw - 8, fh - 8 - wobble, 3, 6);
                
                // Boots
                f.setColor(armorDark);
                f.fillRect(4, fh - 4 + wobble, 5, 2);
                f.fillRect(fw - 9, fh - 4 - wobble, 5, 2);

                f.dispose();
                g.drawImage(frame, x0, y0, null);
            }
        }

        g.dispose();
        return sheet;
    }

    // ===== Optional utility: flip image horizontally (if you ever want to derive RIGHT from LEFT) =====
    @SuppressWarnings("unused")
    private static BufferedImage flipHorizontal(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-src.getWidth(), 0);
        g.drawImage(src, tx, null);
        g.dispose();
        return dst;
    }

    // Add inside TextureManager (public API)
    public static BufferedImage getPlayerFrame(Direction dir, Motion motion, long timeMs) {
        ensureLoaded();
        SpriteAnimation a = playerAnimations.get(new Key(motion, dir));
        return (a != null && a.length() > 0) ? a.frameAt(timeMs) : playerFirstFrame;
    }

    public static BufferedImage getEnemyFrame(Direction dir, Motion motion, long timeMs) {
        ensureLoaded();
        SpriteAnimation a = enemyAnimations.get(new Key(motion, dir));
        return (a != null && a.length() > 0) ? a.frameAt(timeMs) : enemyFirstFrame;
    }
    
    // ===== Biome texture generation =====
    
    /**
     * Generates all biome-specific textures using the new biome-specific file naming system.
     * Textures are saved as {biome}_{texture}.png files for easy management.
     */
    private static void generateBiomeTextures() {
        // Meadows biome textures - reuse existing grass and stone textures
        meadowGrassTexture = grassTexture; // Reuse the legacy grass texture
        meadowDirtTexture = loadOrGenerateBiomeTexture("meadows", "dirt", () -> generateMeadowDirtTexture(32, 32));
        meadowPlantsTexture = loadOrGenerateBiomeTexture("meadows", "plants", () -> generateMeadowPlantsTexture(32, 32));
        meadowFlowersTexture = loadOrGenerateBiomeTexture("meadows", "flowers", () -> generateMeadowFlowersTexture(32, 32));
        
        // Forest biome textures
        forestTreeTexture = loadOrGenerateBiomeTexture("forest", "tree", () -> generateForestTreeTexture(32, 32));
        forestLogTexture = loadOrGenerateBiomeTexture("forest", "log", () -> generateForestLogTexture(32, 32));
        forestGroundTexture = loadOrGenerateBiomeTexture("forest", "ground", () -> generateForestGroundTexture(32, 32));
        forestLeavesTexture = loadOrGenerateBiomeTexture("forest", "leaves", () -> generateForestLeavesTexture(32, 32));
        forestMushroomsTexture = loadOrGenerateBiomeTexture("forest", "mushrooms", () -> generateForestMushroomsTexture(32, 32));
        
        // Cave biome textures
        caveStoneTexture = loadOrGenerateBiomeTexture("cave", "stone", () -> generateCaveStoneTexture(32, 32));
        caveCrystalTexture = loadOrGenerateBiomeTexture("cave", "crystal", () -> generateCaveCrystalTexture(32, 32));
        caveWaterTexture = loadOrGenerateBiomeTexture("cave", "water", () -> generateCaveWaterTexture(32, 32));
        caveStalagmiteTexture = loadOrGenerateBiomeTexture("cave", "stalagmite", () -> generateCaveStalagmiteTexture(32, 32));
        caveCrystalFormationTexture = loadOrGenerateBiomeTexture("cave", "crystal_formation", () -> generateCaveCrystalFormationTexture(32, 32));
        
        // Desert biome textures
        desertSandTexture = loadOrGenerateBiomeTexture("desert", "sand", () -> generateDesertSandTexture(32, 32));
        desertRockTexture = loadOrGenerateBiomeTexture("desert", "rock", () -> generateDesertRockTexture(32, 32));
        desertCactusTexture = loadOrGenerateBiomeTexture("desert", "cactus", () -> generateDesertCactusTexture(32, 32));
        desertRockFormationTexture = loadOrGenerateBiomeTexture("desert", "rock_formation", () -> generateDesertRockFormationTexture(32, 32));
        
        // Vulcan biome textures
        vulcanRockTexture = loadOrGenerateBiomeTexture("vulcan", "rock", () -> generateVulcanRockTexture(32, 32));
        vulcanLavaTexture = loadOrGenerateBiomeTexture("vulcan", "lava", () -> generateVulcanLavaTexture(32, 32));
        vulcanAshTexture = loadOrGenerateBiomeTexture("vulcan", "ash", () -> generateVulcanAshTexture(32, 32));
        vulcanCrystalTexture = loadOrGenerateBiomeTexture("vulcan", "crystal", () -> generateVulcanCrystalTexture(32, 32));
    }
    
    // Forest texture generators
    private static BufferedImage generateForestTreeTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(40, 80, 40));
        g.fillRect(0, 0, w, h);
        
        // Tree trunk
        g.setColor(new Color(80, 50, 30));
        g.fillRect(w/2 - 3, h/2, 6, h/2);
        
        // Tree foliage
        g.setColor(new Color(60, 120, 60));
        g.fillOval(w/2 - 8, 2, 16, 16);
        g.setColor(new Color(80, 140, 80));
        g.fillOval(w/2 - 6, 4, 12, 12);
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateForestLogTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(100, 70, 40));
        g.fillRect(0, 0, w, h);
        
        // Log rings
        g.setColor(new Color(80, 50, 30));
        for (int i = 0; i < 3; i++) {
            int y = h/4 + i * h/4;
            g.drawLine(2, y, w-2, y);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateForestGroundTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 100, 60));
        g.fillRect(0, 0, w, h);
        
        // Forest floor details
        g.setColor(new Color(50, 80, 50));
        for (int i = 0; i < w * h / 20; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 2, 2);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateForestLeavesTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(70, 120, 70));
        g.fillRect(0, 0, w, h);
        
        // Leaf patterns
        g.setColor(new Color(90, 140, 90));
        for (int i = 0; i < w * h / 15; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 3, 2);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateForestMushroomsTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 100, 60));
        g.fillRect(0, 0, w, h);
        
        // Mushrooms
        g.setColor(new Color(200, 100, 100));
        g.fillOval(w/2 - 4, h/2 - 2, 8, 4);
        g.setColor(new Color(150, 80, 80));
        g.fillRect(w/2 - 1, h/2 + 2, 2, 4);
        
        g.dispose();
        return img;
    }
    
    // Cave texture generators
    private static BufferedImage generateCaveStoneTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 80, 90));
        g.fillRect(0, 0, w, h);
        
        // Cave stone details
        g.setColor(new Color(60, 60, 70));
        for (int i = 0; i < w * h / 12; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 2, 2);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateCaveCrystalTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 80, 90));
        g.fillRect(0, 0, w, h);
        
        // Crystal formation
        g.setColor(new Color(150, 200, 255));
        g.fillPolygon(
            new int[]{w/2, w/2 - 3, w/2 + 3},
            new int[]{h/2 - 4, h/2 + 2, h/2 + 2},
            3
        );
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateCaveWaterTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 80, 120));
        g.fillRect(0, 0, w, h);
        
        // Water ripples
        g.setColor(new Color(80, 100, 160));
        for (int i = 0; i < 3; i++) {
            int y = h/4 + i * h/4;
            g.drawLine(2, y, w-2, y);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateCaveStalagmiteTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 80, 90));
        g.fillRect(0, 0, w, h);
        
        // Stalagmite
        g.setColor(new Color(100, 100, 110));
        g.fillPolygon(
            new int[]{w/2, w/2 - 4, w/2 + 4},
            new int[]{h, h/2, h/2},
            3
        );
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateCaveCrystalFormationTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 80, 90));
        g.fillRect(0, 0, w, h);
        
        // Crystal cluster
        g.setColor(new Color(150, 200, 255));
        for (int i = 0; i < 5; i++) {
            int x = w/2 + (i - 2) * 3;
            int y = h/2 + (i % 2) * 2;
            g.fillPolygon(
                new int[]{x, x - 2, x + 2},
                new int[]{y - 3, y + 2, y + 2},
                3
            );
        }
        
        g.dispose();
        return img;
    }
    
    // Desert texture generators
    private static BufferedImage generateDesertSandTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(240, 220, 180));
        g.fillRect(0, 0, w, h);
        
        // Sand texture
        g.setColor(new Color(220, 200, 160));
        for (int i = 0; i < w * h / 25; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 1, 1);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateDesertRockTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 180, 160));
        g.fillRect(0, 0, w, h);
        
        // Rock details
        g.setColor(new Color(180, 160, 140));
        for (int i = 0; i < w * h / 20; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 2, 2);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateDesertCactusTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(240, 220, 180));
        g.fillRect(0, 0, w, h);
        
        // Cactus
        g.setColor(new Color(80, 120, 80));
        g.fillRect(w/2 - 2, h/2 - 6, 4, 12);
        g.fillRect(w/2 - 4, h/2, 8, 4);
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateDesertRockFormationTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 180, 160));
        g.fillRect(0, 0, w, h);
        
        // Rock formation
        g.setColor(new Color(180, 160, 140));
        g.fillPolygon(
            new int[]{w/2, w/2 - 6, w/2 + 6},
            new int[]{h, h/2, h/2},
            3
        );
        
        g.dispose();
        return img;
    }

    // Vulcan texture generators
    private static BufferedImage generateVulcanRockTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(100, 100, 100));
        g.fillRect(0, 0, w, h);
        
        // Rock details
        g.setColor(new Color(80, 80, 80));
        for (int i = 0; i < w * h / 15; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 2, 2);
        }
        
        g.dispose();
        return img;
    }

    private static BufferedImage generateVulcanLavaTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 0, 0));
        g.fillRect(0, 0, w, h);
        
        // Lava flow
        g.setColor(new Color(255, 0, 0));
        for (int i = 0; i < w * h / 100; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 1, 1);
        }
        
        g.dispose();
        return img;
    }

    private static BufferedImage generateVulcanAshTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(150, 150, 150));
        g.fillRect(0, 0, w, h);
        
        // Ash particles
        g.setColor(new Color(100, 100, 100));
        for (int i = 0; i < w * h / 100; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 1, 1);
        }
        
        g.dispose();
        return img;
    }

    private static BufferedImage generateVulcanCrystalTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(150, 200, 255));
        g.fillRect(0, 0, w, h);
        
        // Crystal formation
        g.setColor(new Color(200, 255, 255));
        for (int i = 0; i < 5; i++) {
            int x = w/2 + (i - 2) * 3;
            int y = h/2 + (i % 2) * 2;
            g.fillPolygon(
                new int[]{x, x - 2, x + 2},
                new int[]{y - 3, y + 2, y + 2},
                3
            );
        }
        
        g.dispose();
        return img;
    }
    
    // Meadows texture generators
    private static BufferedImage generateMeadowGrassTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 140, 80));
        g.fillRect(0, 0, w, h);
        
        // Grass blade details
        g.setColor(new Color(100, 160, 100));
        for (int i = 0; i < w * h / 25; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 2, 1);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateMeadowDirtTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(120, 100, 80));
        g.fillRect(0, 0, w, h);
        
        // Dirt texture details
        g.setColor(new Color(100, 80, 60));
        for (int i = 0; i < w * h / 30; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 3, 2);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateMeadowPlantsTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(70, 130, 70));
        g.fillRect(0, 0, w, h);
        
        // Plant details
        g.setColor(new Color(90, 150, 90));
        for (int i = 0; i < w * h / 20; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 2, 3);
        }
        
        g.dispose();
        return img;
    }
    
    private static BufferedImage generateMeadowFlowersTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(90, 140, 90));
        g.fillRect(0, 0, w, h);
        
        // Flower details
        g.setColor(new Color(255, 200, 200));
        for (int i = 0; i < w * h / 40; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            g.fillOval(x, y, 4, 4);
        }
        
        g.dispose();
        return img;
    }
}
