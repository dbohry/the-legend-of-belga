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

    // ===== Existing assets (grass/stone unchanged) =====
    private static final String ASSETS_DIR = "assets";
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

    public static BufferedImage getGrassTextureFrame(int tick30) {
        ensureLoaded();
        if (!ANIMATE_GRASS) return grassTexture;
        if (grassFrames == null || grassFrames.length == 0) return grassTexture;
        int idx = Math.floorMod(tick30 / 2, GRASS_FRAMES);
        return grassFrames[idx];
    }

    // ===== Internal load =====
    private static synchronized void ensureLoaded() {
        if (loaded) return;

        ClassLoader cl = TextureManager.class.getClassLoader();

        // Grass & Stone: keep exactly as-is (existing behavior + fallback gen)
        grassTexture = tryLoad(cl.getResourceAsStream(GRASS_FILE));
        stoneTexture = tryLoad(cl.getResourceAsStream(STONE_FILE));

        if (grassTexture == null) {
            grassTexture = generateGrassTexture(32, 32);
            tryWrite(GRASS_FILE, grassTexture);
        }
        if (stoneTexture == null) {
            stoneTexture = generateStoneTexture(32, 32);
            tryWrite(STONE_FILE, stoneTexture);
        }

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

        Color body = new Color(200, 60, 60);
        Color shade = new Color(150, 30, 30);
        Color eyeW = new Color(250, 250, 250);
        Color eyeB = new Color(20, 20, 20);

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

                // Blobby body
                f.setColor(body);
                f.fillOval(2, 3 + wobble, fw - 4, fh - 8);

                // Belly shading
                f.setColor(shade);
                f.fillOval(4, 6 + wobble, fw - 12, fh - 14);

                // Eyes (position shifts slightly per direction)
                int ex = (r == 1) ? -1 : (r == 2) ? 1 : 0; // LEFT/RIGHT
                f.setColor(eyeW);
                f.fillOval(6 + ex, 7 + wobble, 5, 5);
                f.fillOval(fw - 11 + ex, 7 + wobble, 5, 5);
                f.setColor(eyeB);
                f.fillOval(8 + ex, 9 + wobble, 2, 2);
                f.fillOval(fw - 9 + ex, 9 + wobble, 2, 2);

                // Little mouth / beak
                f.setColor(new Color(240, 180, 120));
                f.fillOval(fw / 2 - 2, 11 + wobble, 4, 3);

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

}
