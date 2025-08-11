package com.lhamacorp.games.tlob.maps;

import java.util.Random;

public class MapGenerator {
    private final int width;
    private final int height;
    private final double wallBias;
    private final int steps;
    private final Random rng;

    /** Backward-compatible ctor: pass density as wallBias (e.g., 0.45). */
    public MapGenerator(int width, int height, double wallBias, int steps) {
        this(width, height, wallBias, steps, null);
    }

    public MapGenerator(int width, int height, double wallBias, int steps, Random rng) {
        this.width = width;
        this.height = height;
        this.wallBias = wallBias;
        this.steps = steps;
        this.rng = (rng != null) ? rng : new Random();
    }

    public int[][] generate() {
        int[][] tiles = new int[width][height];

        // Start filled with walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = TileMap.WALL;
            }
        }

        // Drunkard walk from center
        int cx = width / 2;
        int cy = height / 2;

        int carved = 0;
        int targetFloor = (int) (width * height * wallBias); // keep original 0.45 behavior
        int maxSteps = Math.max(steps, width * height * 4);

        for (int i = 0; i < maxSteps && carved < targetFloor; i++) {
            if (tiles[cx][cy] == TileMap.WALL) {
                tiles[cx][cy] = TileMap.FLOOR;
                carved++;
            }
            switch (rng.nextInt(4)) {
                case 0 -> cx += 1;
                case 1 -> cx -= 1;
                case 2 -> cy += 1;
                case 3 -> cy -= 1;
            }
            cx = clamp(cx, 1, width - 2);
            cy = clamp(cy, 1, height - 2);
        }

        // Ensure border walls
        for (int x = 0; x < width; x++) {
            tiles[x][0] = TileMap.WALL;
            tiles[x][height - 1] = TileMap.WALL;
        }
        for (int y = 0; y < height; y++) {
            tiles[0][y] = TileMap.WALL;
            tiles[width - 1][y] = TileMap.WALL;
        }

        // Smooth map a bit
        return smooth(tiles, 2);
    }

    private int[][] smooth(int[][] tiles, int iterations) {
        int[][] result = tiles;
        for (int it = 0; it < iterations; it++) {
            int[][] next = new int[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int walls = countNeighbors(result, x, y, 1);
                    next[x][y] = (walls >= 5) ? TileMap.WALL : TileMap.FLOOR;
                }
            }
            result = next;
        }
        return result;
    }

    private int countNeighbors(int[][] tiles, int x, int y, int radius) {
        int count = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    count++; // out of bounds = wall
                } else if (tiles[nx][ny] == TileMap.WALL) {
                    count++;
                }
            }
        }
        return count;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
