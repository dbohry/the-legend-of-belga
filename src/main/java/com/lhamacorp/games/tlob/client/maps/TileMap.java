package com.lhamacorp.games.tlob.client.maps;

import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.core.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TileMap {
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int EDGE_WALL = 2;

    private final int width;
    private final int height;
    private final int[][] tiles;
    private final double[][] wallHealth;
    private static final double WALL_MAX_HP = 4.0;

    private final Random rng;

    // Back-compat (non-deterministic)
    public TileMap(int[][] tiles) {
        this(tiles, new Random());
    }

    public TileMap(int[][] tiles, Random rng) {
        this.tiles = tiles;
        this.width = tiles.length;
        this.height = tiles[0].length;
        this.rng = (rng != null) ? rng : new Random();
        this.wallHealth = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == WALL) {
                    if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                        tiles[x][y] = EDGE_WALL;
                        wallHealth[x][y] = -1; // Indestructible
                    } else {
                        wallHealth[x][y] = WALL_MAX_HP;
                    }
                } else {
                    wallHealth[x][y] = 0;
                }
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return true;
        int t = tiles[x][y];
        return t == WALL || t == EDGE_WALL;
    }

    public boolean damageWall(int x, int y, double damage) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        if (tiles[x][y] != WALL || wallHealth[x][y] <= 0) return false;
        if (wallHealth[x][y] == -1) return false; // Indestructible
        wallHealth[x][y] -= damage;
        if (wallHealth[x][y] <= 0) {
            wallHealth[x][y] = 0;
            tiles[x][y] = FLOOR;
            return true;
        }
        return false;
    }

    // Back-compat
    public void draw(Graphics2D g, int camX, int camY, int viewW, int viewH) {
        draw(g, camX, camY, viewW, viewH, 0);
    }

    /** Animated draw; tick30 is a 30 Hz counter (see GameManager). */
    public void draw(Graphics2D g, int camX, int camY, int viewW, int viewH, int tick30) {
        final int tileSize = Constants.TILE_SIZE;

        int startX = Math.max(0, camX / tileSize);
        int startY = Math.max(0, camY / tileSize);
        int endX = Math.min(width - 1, (camX + viewW) / tileSize + 1);
        int endY = Math.min(height - 1, (camY + viewH) / tileSize + 1);

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                int px = x * tileSize - camX;
                int py = y * tileSize - camY;

                int t = tiles[x][y];
                boolean isWall = (t == WALL || t == EDGE_WALL);

                BufferedImage tex = isWall
                    ? TextureManager.getStoneTexture()
                    : TextureManager.getGrassTextureFrame(tick30);

                if (tex != null) {
                    g.drawImage(tex, px, py, tileSize, tileSize, null);

                    if (t == WALL && wallHealth[x][y] > 0 && wallHealth[x][y] < WALL_MAX_HP) {
                        double damagePercent = 1.0 - (wallHealth[x][y] / WALL_MAX_HP);
                        int overlayAlpha = (int) Math.round(damagePercent * 110);
                        g.setColor(new Color(255, 60, 60, overlayAlpha));
                        g.fillRect(px, py, tileSize, tileSize);
                    }
                } else {
                    if (isWall) {
                        g.setColor(new Color(35, 35, 45));
                        g.fillRect(px, py, tileSize, tileSize);
                        g.setColor(new Color(20, 20, 28));
                        g.drawRect(px, py, tileSize, tileSize);

                        if (t == WALL && wallHealth[x][y] > 0 && wallHealth[x][y] < WALL_MAX_HP) {
                            double damagePercent = 1.0 - (wallHealth[x][y] / WALL_MAX_HP);
                            int overlayAlpha = (int) Math.round(damagePercent * 110);
                            g.setColor(new Color(255, 60, 60, overlayAlpha));
                            g.fillRect(px, py, tileSize, tileSize);
                        }
                    } else {
                        g.setColor(new Color(72, 110, 72));
                        g.fillRect(px, py, tileSize, tileSize);
                        g.setColor(new Color(60, 90, 60));
                        g.drawRect(px, py, tileSize, tileSize);
                    }
                }
            }
        }
    }

    public int[] findSpawnTile() {
        for (int y = height / 2 - 1; y <= height / 2 + 1; y++)
            for (int x = width / 2 - 1; x <= width / 2 + 1; x++)
                if (!isWall(x, y)) return new int[]{x, y};

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (!isWall(x, y)) return new int[]{x, y};

        return new int[]{1, 1};
    }

    public int[] randomFloorTileFarFrom(double px, double py, int minDistance) {
        for (int attempts = 0; attempts < 5000; attempts++) {
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);
            if (!isWall(x, y)) {
                double cx = x * Constants.TILE_SIZE + Constants.TILE_SIZE / 2.0;
                double cy = y * Constants.TILE_SIZE + Constants.TILE_SIZE / 2.0;
                if (Math.hypot(cx - px, cy - py) >= minDistance) return new int[]{x, y};
            }
        }
        return null;
    }

    public int[] getRandomFloorTile() {
        for (int attempts = 0; attempts < 1000; attempts++) {
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);
            if (!isWall(x, y)) return new int[]{x, y};
        }
        return findSpawnTile();
    }
}
