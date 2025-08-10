package com.lhamacorp.games.tlob.managers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class TextureManager {
    private static final String ASSETS_DIR = "assets";
    private static final String GRASS_FILE = ASSETS_DIR + "/grass.png";
    private static final String STONE_FILE = ASSETS_DIR + "/stone.png";
    private static final String PLAYER_FILE = ASSETS_DIR + "/player.png";
    private static final String ENEMY_FILE = ASSETS_DIR + "/enemy.png";

    private static BufferedImage grassTexture;
    private static BufferedImage stoneTexture;
    private static BufferedImage playerTexture;
    private static BufferedImage enemyTexture;

    private TextureManager() {
    }

    public static BufferedImage getGrassTexture() {
        ensureLoaded();
        return grassTexture;
    }

    public static BufferedImage getStoneTexture() {
        ensureLoaded();
        return stoneTexture;
    }

    public static BufferedImage getPlayerTexture() {
        ensureLoaded();
        return playerTexture;
    }

    public static BufferedImage getEnemyTexture() {
        ensureLoaded();
        return enemyTexture;
    }

    private static synchronized void ensureLoaded() {
        if (grassTexture != null && stoneTexture != null) return;
        ClassLoader cl = TextureManager.class.getClassLoader();

        grassTexture = tryLoad(cl.getResourceAsStream(GRASS_FILE));
        stoneTexture = tryLoad(cl.getResourceAsStream(STONE_FILE));
        playerTexture = tryLoad(cl.getResourceAsStream(PLAYER_FILE));
        enemyTexture = tryLoad(cl.getResourceAsStream(ENEMY_FILE));

        if (grassTexture == null) {
            grassTexture = generateGrassTexture(32, 32);
            tryWrite(GRASS_FILE, grassTexture);
        }
        if (stoneTexture == null) {
            stoneTexture = generateStoneTexture(32, 32);
            tryWrite(STONE_FILE, stoneTexture);
        }
        if (playerTexture == null) {
            playerTexture = generatePlayerTexture(22, 22);
            tryWrite(PLAYER_FILE, playerTexture);
        }
        if (enemyTexture == null) {
            enemyTexture = generateEnemyTexture(20, 20);
            tryWrite(ENEMY_FILE, enemyTexture);
        }
    }

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
            ImageIO.write(img, "png", new File(path));
        } catch (IOException ignored) {
        }
    }

    private static BufferedImage generateGrassTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Base color
        g.setColor(new Color(70, 120, 70));
        g.fillRect(0, 0, w, h);
        // Noise blades
        for (int i = 0; i < w * h / 12; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int len = 2 + (int) (Math.random() * 3);
            g.setColor(new Color(60 + (int) (Math.random() * 30), 100 + (int) (Math.random() * 40), 60 + (int) (Math.random() * 30)));
            g.drawLine(x, y, x, Math.max(0, y - len));
        }
        // Subtle vignette
        g.setColor(new Color(0, 0, 0, 20));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage generateStoneTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Base
        g.setColor(new Color(120, 120, 130));
        g.fillRect(0, 0, w, h);
        // Noise speckles
        for (int i = 0; i < w * h / 10; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int a = 40 + (int) (Math.random() * 60);
            int c = 90 + (int) (Math.random() * 70);
            g.setColor(new Color(c, c, c, a));
            g.fillRect(x, y, 1, 1);
        }
        // Cracks
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
        // Border
        g.setColor(new Color(0, 0, 0, 25));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage generatePlayerTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Base body (green)
        g.setColor(new Color(40, 160, 70));
        g.fillRect(2, 2, w - 4, h - 4);

        // Eyes (white)
        g.setColor(Color.WHITE);
        g.fillRect(w / 2 - 6, h / 2 - 4, 3, 3);
        g.fillRect(w / 2 + 3, h / 2 - 4, 3, 3);

        // Pupils (black)
        g.setColor(Color.BLACK);
        g.fillRect(w / 2 - 5, h / 2 - 3, 1, 1);
        g.fillRect(w / 2 + 4, h / 2 - 3, 1, 1);

        // Sword handle (brown)
        g.setColor(new Color(139, 69, 19));
        g.fillRect(w - 4, h / 2 - 2, 2, 4);

        // Border
        g.setColor(new Color(0, 0, 0, 50));
        g.drawRect(0, 0, w - 1, h - 1);

        g.dispose();
        return img;
    }

    private static BufferedImage generateEnemyTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Base body (red)
        g.setColor(new Color(200, 60, 60));
        g.fillRect(2, 2, w - 4, h - 4);

        // Eyes (white)
        g.setColor(Color.WHITE);
        g.fillRect(w / 2 - 5, h / 2 - 4, 3, 3);
        g.fillRect(w / 2 + 2, h / 2 - 4, 3, 3);

        // Pupils (black)
        g.setColor(Color.BLACK);
        g.fillRect(w / 2 - 4, h / 2 - 3, 1, 1);
        g.fillRect(w / 2 + 3, h / 2 - 3, 1, 1);

        // Teeth (white)
        g.setColor(Color.WHITE);
        g.fillRect(w / 2 - 2, h / 2 + 2, 1, 2);
        g.fillRect(w / 2 + 1, h / 2 + 2, 1, 2);

        // Border
        g.setColor(new Color(0, 0, 0, 50));
        g.drawRect(0, 0, w - 1, h - 1);

        g.dispose();
        return img;
    }
}


