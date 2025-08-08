package com.lhamacorp.games.tlob;

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

    public TileMap(int[][] tiles) {
        this.tiles = tiles;
        this.width = tiles.length;
        this.height = tiles[0].length;
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

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean isWall(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return true;
        int tile = tiles[x][y];
        return tile == WALL || tile == EDGE_WALL;
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

    public double getWallHealth(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0;
        if (tiles[x][y] != WALL) return 0;
        return wallHealth[x][y];
    }

    public void draw(Graphics2D g2, int camX, int camY, int screenW, int screenH) {
        int tileSize = GamePanel.TILE_SIZE;

        int startX = Math.max(0, camX / tileSize);
        int startY = Math.max(0, camY / tileSize);
        int endX = Math.min(width - 1, (camX + screenW) / tileSize + 1);
        int endY = Math.min(height - 1, (camY + screenH) / tileSize + 1);

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                int px = x * tileSize - camX;
                int py = y * tileSize - camY;

                int tile = tiles[x][y];
                boolean isWall = tile == WALL || tile == EDGE_WALL;
                BufferedImage tex = isWall ? TextureManager.getStoneTexture() : TextureManager.getGrassTexture();

                if (tex != null) {
                    g2.drawImage(tex, px, py, tileSize, tileSize, null);

                    // Wall damage overlay (only for destructible walls)
                    if (tile == WALL && wallHealth[x][y] > 0 && wallHealth[x][y] < WALL_MAX_HP) {
                        double damagePercent = 1.0 - (wallHealth[x][y] / WALL_MAX_HP);
                        int overlayAlpha = (int)(damagePercent * 100);
                        g2.setColor(new Color(255, 0, 0, overlayAlpha));
                        g2.fillRect(px, py, tileSize, tileSize);
                    }
                } else {
                    // Fallback rendering
                    if (isWall) {
                        g2.setColor(new Color(30, 30, 40));
                        g2.fillRect(px, py, tileSize, tileSize);
                        g2.setColor(new Color(15, 15, 20));
                        g2.drawRect(px, py, tileSize, tileSize);

                        if (tile == WALL && wallHealth[x][y] > 0 && wallHealth[x][y] < WALL_MAX_HP) {
                            double damagePercent = 1.0 - (wallHealth[x][y] / WALL_MAX_HP);
                            int overlayAlpha = (int)(damagePercent * 100);
                            g2.setColor(new Color(255, 0, 0, overlayAlpha));
                            g2.fillRect(px, py, tileSize, tileSize);
                        }
                    } else {
                        g2.setColor(new Color(70, 85, 70));
                        g2.fillRect(px, py, tileSize, tileSize);
                        g2.setColor(new Color(60, 75, 60));
                        g2.drawRect(px, py, tileSize, tileSize);
                    }
                }
            }
        }
    }

    public int[] findSpawnTile() {
        for (int y = height / 2 - 1; y <= height / 2 + 1; y++) {
            for (int x = width / 2 - 1; x <= width / 2 + 1; x++) {
                if (!isWall(x, y)) return new int[]{x, y};
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isWall(x, y)) return new int[]{x, y};
            }
        }
        return new int[]{1, 1};
    }

    public int[] randomFloorTileFarFrom(double px, double py, int minDistance) {
        Random rnd = new Random();
        for (int attempts = 0; attempts < 5000; attempts++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            if (!isWall(x, y)) {
                double cx = x * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
                double cy = y * GamePanel.TILE_SIZE + GamePanel.TILE_SIZE / 2.0;
                if (Math.hypot(cx - px, cy - py) >= minDistance) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    public int[] getRandomFloorTile() {
        Random rnd = new Random();
        for (int attempts = 0; attempts < 1000; attempts++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            if (!isWall(x, y)) {
                return new int[]{x, y};
            }
        }
        return findSpawnTile();
    }
}
