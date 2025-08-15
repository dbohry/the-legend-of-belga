package com.lhamacorp.games.tlob.client.maps;

import java.util.Random;

public class MapGenerator {
    private final int width;
    private final int height;
    private final Biome biome;
    private final Random rng;

    public MapGenerator(int width, int height, Biome biome) {
        this(width, height, biome, new Random());
    }

    public MapGenerator(int width, int height, Biome biome, Random rng) {
        this.width = width;
        this.height = height;
        this.biome = biome;
        this.rng = (rng != null) ? rng : new Random();
    }

    public int[][] generate() {
        int[][] tiles = new int[width][height];

        // Start filled with biome walls (but not edge walls)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    tiles[x][y] = TileMap.EDGE_WALL; // Set edge walls first
                } else {
                    tiles[x][y] = getBiomeWallTile(); // Set biome walls for interior
                }
            }
        }

        // Drunkard walk from center (avoid edge walls)
        int cx = width / 2, cy = height / 2;
        int carved = 0;
        int targetFloor = (int) (width * height * biome.getWallDensity());
        int maxSteps = Math.max(biome.getCarveSteps(), width * height * 4);

        for (int i = 0; i < maxSteps && carved < targetFloor; i++) {
            if (tiles[cx][cy] == getBiomeWallTile()) {
                tiles[cx][cy] = getBiomeFloorTile();
                carved++;
            }
            switch (rng.nextInt(4)) {
                case 0 -> cx++;
                case 1 -> cx--;
                case 2 -> cy++;
                case 3 -> cy--;
            }
            cx = clamp(cx, 1, width - 2);
            cy = clamp(cy, 1, height - 2);
        }

        // Smooth map a bit
        tiles = smooth(tiles, 2);

        // Decorate floors with biome-specific features
        decorateBiomeFloors(tiles);
        return tiles;
    }

    private int[][] smooth(int[][] tiles, int iterations) {
        int[][] result = tiles;
        for (int it = 0; it < iterations; it++) {
            int[][] next = new int[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Preserve edge walls during smoothing
                    if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                        next[x][y] = TileMap.EDGE_WALL;
                    } else {
                        int walls = countNeighbors(result, x, y, 1);
                        next[x][y] = (walls >= 5) ? getBiomeWallTile() : getBiomeFloorTile();
                    }
                }
            }
            result = next;
        }
        return result;
    }

    private void decorateBiomeFloors(int[][] tiles) {
        switch (biome) {
            case MEADOWS -> decorateMeadowsFloors(tiles);
            case FOREST -> decorateForestFloors(tiles);
            case CAVE -> decorateCaveFloors(tiles);
            case DESERT -> decorateDesertFloors(tiles);
            case VULCAN -> decorateVulcanFloors(tiles);
        }
    }

    private void decorateMeadowsFloors(int[][] tiles) {
        // Start with everything non-wall as grass (but preserve edge walls)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] != getBiomeWallTile() && tiles[x][y] != TileMap.EDGE_WALL) {
                    tiles[x][y] = TileMap.FLOOR_GRASS;
                }
            }
        }

        // Carve dirt paths
        int paths = Math.max(2, (width + height) / 20);
        for (int p = 0; p < paths; p++) {
            int x = clamp(rng.nextInt(width - 2) + 1, 1, width - 2);
            int y = clamp(rng.nextInt(height - 2) + 1, 1, height - 2);
            int len = 20 + rng.nextInt(Math.max(20, (width + height) / 2));
            for (int i = 0; i < len; i++) {
                if (tiles[x][y] != getBiomeWallTile()) tiles[x][y] = TileMap.FLOOR_DIRT;
                int dir = rng.nextInt(4);
                if (dir == 0) x++; else if (dir == 1) x--; else if (dir == 2) y++; else y--;
                x = clamp(x, 1, width - 2);
                y = clamp(y, 1, height - 2);
            }
        }

        // Scatter plant patches
        int patches = Math.max(3, (int) ((width * height) * biome.getPlantDensity() / 600));
        for (int k = 0; k < patches; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1 + rng.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_GRASS && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.6) tiles[nx][ny] = TileMap.FLOOR_PLANTS;
                    }
                }
            }
        }
        
        // Scatter flower patches
        int flowerPatches = Math.max(2, (int) ((width * height) * biome.getFeatureDensity() / 800));
        for (int k = 0; k < flowerPatches; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1 + rng.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_GRASS && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.7) tiles[nx][ny] = TileMap.FLOOR_MEADOW_FLOWERS;
                    }
                }
            }
        }
    }

    private void decorateForestFloors(int[][] tiles) {
        // Start with forest ground (but preserve edge walls)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] != getBiomeWallTile() && tiles[x][y] != TileMap.EDGE_WALL) {
                    tiles[x][y] = TileMap.FLOOR_FOREST_GROUND;
                }
            }
        }

        // Add fallen logs
        int logs = Math.max(2, (int) ((width + height) * biome.getFeatureDensity() / 15));
        for (int l = 0; l < logs; l++) {
            int x = clamp(rng.nextInt(width - 2) + 1, 1, width - 2);
            int y = clamp(rng.nextInt(height - 2) + 1, 1, height - 2);
            if (tiles[x][y] != getBiomeWallTile()) {
                tiles[x][y] = TileMap.WALL_FOREST_LOG;
            }
        }

        // Add leaf patches
        int leafPatches = Math.max(4, (int) ((width * height) * biome.getPlantDensity() / 400));
        for (int k = 0; k < leafPatches; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1 + rng.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_FOREST_GROUND && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.7) tiles[nx][ny] = TileMap.FLOOR_FOREST_LEAVES;
                    }
                }
            }
        }

        // Add mushroom patches
        int mushroomPatches = Math.max(2, (int) ((width * height) * biome.getFeatureDensity() / 800));
        for (int k = 0; k < mushroomPatches; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_FOREST_GROUND && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.8) tiles[nx][ny] = TileMap.FLOOR_FOREST_MUSHROOMS;
                    }
                }
            }
        }
    }

    private void decorateCaveFloors(int[][] tiles) {
        // Start with cave stone (but preserve edge walls)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] != getBiomeWallTile() && tiles[x][y] != TileMap.EDGE_WALL) {
                    tiles[x][y] = TileMap.FLOOR_CAVE_STONE;
                }
            }
        }

        // Add crystal formations
        int crystals = Math.max(1, (int) ((width * height) * biome.getFeatureDensity() / 600));
        for (int k = 0; k < crystals; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_CAVE_STONE && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.9) tiles[nx][ny] = TileMap.FLOOR_CAVE_CRYSTAL;
                    }
                }
            }
        }

        // Add water pools
        int waterPools = Math.max(1, (int) ((width * height) * biome.getFeatureDensity() / 1000));
        for (int k = 0; k < waterPools; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 2 + rng.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_CAVE_STONE && (dx * dx + dy * dy) <= radius * radius) {
                        tiles[nx][ny] = TileMap.FLOOR_CAVE_WATER;
                    }
                }
            }
        }
    }

    private void decorateDesertFloors(int[][] tiles) {
        // Start with desert sand (but preserve edge walls)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] != getBiomeWallTile() && tiles[x][y] != TileMap.EDGE_WALL) {
                    tiles[x][y] = TileMap.FLOOR_DESERT_SAND;
                }
            }
        }

        // Add rock patches
        int rockPatches = Math.max(3, (int) ((width * height) * biome.getFeatureDensity() / 500));
        for (int k = 0; k < rockPatches; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1 + rng.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_DESERT_SAND && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.6) tiles[nx][ny] = TileMap.FLOOR_DESERT_ROCK;
                    }
                }
            }
        }

        // Add cacti
        int cacti = Math.max(2, (int) ((width * height) * biome.getFeatureDensity() / 800));
        for (int k = 0; k < cacti; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            if (tiles[cx][cy] == TileMap.FLOOR_DESERT_SAND) {
                tiles[cx][cy] = TileMap.FLOOR_DESERT_CACTUS;
            }
        }
    }

    private void decorateVulcanFloors(int[][] tiles) {
        // Start with vulcan rock (but preserve edge walls)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] != getBiomeWallTile() && tiles[x][y] != TileMap.EDGE_WALL) {
                    tiles[x][y] = TileMap.FLOOR_VULCAN_ROCK;
                }
            }
        }

        // Add lava pools
        int lavaPools = Math.max(2, (int) ((width * height) * biome.getFeatureDensity() / 800));
        for (int k = 0; k < lavaPools; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 2 + rng.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_VULCAN_ROCK && (dx * dx + dy * dy) <= radius * radius) {
                        tiles[nx][ny] = TileMap.FLOOR_VULCAN_LAVA;
                    }
                }
            }
        }

        // Add ash patches
        int ashPatches = Math.max(4, (int) ((width * height) * biome.getPlantDensity() / 500));
        for (int k = 0; k < ashPatches; k++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(height);
            int radius = 1 + rng.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx <= 0 || ny <= 0 || nx >= width - 1 || ny >= height - 1) continue;
                    if (tiles[nx][ny] == TileMap.FLOOR_VULCAN_ROCK && (dx * dx + dy * dy) <= radius * radius) {
                        if (rng.nextDouble() < 0.7) tiles[nx][ny] = TileMap.FLOOR_VULCAN_ASH;
                    }
                }
            }
        }
    }

    private int getBiomeWallTile() {
        return switch (biome) {
            case FOREST -> TileMap.WALL_FOREST_TREE;
            case CAVE -> TileMap.WALL_CAVE_STALAGMITE;
            case DESERT -> TileMap.WALL_DESERT_ROCK_FORMATION;
            case VULCAN -> TileMap.WALL_VULCAN_ROCK;
            default -> TileMap.WALL; // MEADOWS
        };
    }

    private int getBiomeFloorTile() {
        return switch (biome) {
            case FOREST -> TileMap.FLOOR_FOREST_GROUND;
            case CAVE -> TileMap.FLOOR_CAVE_STONE;
            case DESERT -> TileMap.FLOOR_DESERT_SAND;
            case VULCAN -> TileMap.FLOOR_VULCAN_ROCK;
            default -> TileMap.FLOOR_GRASS; // MEADOWS
        };
    }

    private int countNeighbors(int[][] tiles, int x, int y, int radius) {
        int count = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) count++;
                else if (tiles[nx][ny] == getBiomeWallTile()) count++;
            }
        }
        return count;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
