package com.lhamacorp.games.tlob;

import java.util.Random;

public class MapGenerator {
    private final int width;
    private final int height;
    private final double wallBias; // not used in drunkard directly; reserved
    private final int steps;

    public MapGenerator(int width, int height, double wallBias, int steps) {
        this.width = width;
        this.height = height;
        this.wallBias = wallBias;
        this.steps = steps;
    }

    public int[][] generate() {
        int[][] tiles = new int[width][height];
        // Start filled with walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = TileMap.WALL;
            }
        }

        // Carve using drunkard walk from center
        int cx = width / 2;
        int cy = height / 2;
        Random rnd = new Random();

        int carved = 0;
        int targetFloor = (int) (width * height * 0.45);
        int maxSteps = Math.max(steps, width * height * 4);

        for (int i = 0; i < maxSteps && carved < targetFloor; i++) {
            if (tiles[cx][cy] == TileMap.WALL) {
                tiles[cx][cy] = TileMap.FLOOR;
                carved++;
            }
            int dir = rnd.nextInt(4);
            switch (dir) {
                case 0: cx += 1; break;
                case 1: cx -= 1; break;
                case 2: cy += 1; break;
                case 3: cy -= 1; break;
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

        // Add some smoothing by filling isolated wall corners
        tiles = smooth(tiles, 2);

        return tiles;
    }

    private int[][] smooth(int[][] tiles, int iterations) {
        int[][] result = tiles;
        for (int it = 0; it < iterations; it++) {
            int[][] next = new int[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int walls = countNeighbors(result, x, y, 1);
                    if (walls >= 5) next[x][y] = TileMap.WALL; else next[x][y] = TileMap.FLOOR;
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
                    count++; // treat out of bounds as wall
                } else if (tiles[nx][ny] == TileMap.WALL) {
                    count++;
                }
            }
        }
        return count;
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
