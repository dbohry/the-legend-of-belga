package com.lhamacorp.games.tlob.core.world;

import com.lhamacorp.games.tlob.core.Constants;

import java.util.Random;

public final class GridMap {

    public static final int FLOOR = 0;
    public static final int WALL = 1;

    private final int w, h, tile;
    private final int[][] t;

    public GridMap(int w, int h, long seed) {
        this.w = w;
        this.h = h;
        this.tile = Constants.TILE_SIZE;
        this.t = new int[w][h];
        generate(seed);
    }

    private void generate(long seed) {
        Random r = new Random(seed ^ 0xCAFEBABE1234L);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++) {
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    t[x][y] = WALL;
                    continue;
                }
                t[x][y] = (r.nextDouble() < 0.45) ? WALL : FLOOR;
            }
        for (int step = 0; step < 4; step++) {
            int[][] n = new int[w][h];
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++) {
                    int walls = 0;
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            int xx = x + dx, yy = y + dy;
                            if (xx < 0 || yy < 0 || xx >= w || yy >= h || t[xx][yy] == WALL) walls++;
                        }
                    n[x][y] = (walls >= 5) ? WALL : FLOOR;
                }
            for (int x = 0; x < w; x++) System.arraycopy(n[x], 0, t[x], 0, h);
            for (int x = 0; x < w; x++) {
                t[x][0] = t[x][h - 1] = WALL;
            }
            for (int y = 0; y < h; y++) {
                t[0][y] = t[w - 1][y] = WALL;
            }
        }
    }

    public boolean isWallTile(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return true;
        return t[tx][ty] == WALL;
    }

    /** AABB (center cx,cy with half-size) vs tiles. */
    public boolean collidesBox(double cx, double cy, int half) {
        int[][] pts = {
            {(int) Math.floor((cx - half) / tile), (int) Math.floor((cy - half) / tile)},
            {(int) Math.floor((cx + half) / tile), (int) Math.floor((cy - half) / tile)},
            {(int) Math.floor((cx - half) / tile), (int) Math.floor((cy + half) / tile)},
            {(int) Math.floor((cx + half) / tile), (int) Math.floor((cy + half) / tile)}
        };
        for (int[] p : pts) if (isWallTile(p[0], p[1])) return true;
        return false;
    }

    /** Random floor tile at least minDistPixels away from (px,py). */
    public int[] randomFloorTileFarFrom(double px, double py, int minDistPixels, Random r) {
        for (int attempts = 0; attempts < 5000; attempts++) {
            int x = r.nextInt(w), y = r.nextInt(h);
            if (!isWallTile(x, y)) {
                double cx = x * tile + tile / 2.0;
                double cy = y * tile + tile / 2.0;
                if (Math.hypot(cx - px, cy - py) >= minDistPixels) return new int[]{x, y};
            }
        }
        for (int yy = 1; yy < h - 1; yy++)
            for (int xx = 1; xx < w - 1; xx++)
                if (!isWallTile(xx, yy)) return new int[]{xx, yy};
        return new int[]{1, 1};
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    public int tile() {
        return tile;
    }
}
