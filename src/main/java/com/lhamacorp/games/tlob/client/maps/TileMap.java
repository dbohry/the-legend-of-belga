package com.lhamacorp.games.tlob.client.maps;

import com.lhamacorp.games.tlob.client.managers.TextureManager;
import com.lhamacorp.games.tlob.core.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TileMap {
    // Base tile types
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int EDGE_WALL = 2;

    // Meadows biome tiles (current tiles)
    public static final int FLOOR_GRASS = FLOOR;
    public static final int FLOOR_DIRT = 3;
    public static final int FLOOR_PLANTS = 4;
    
    // Forest biome tiles
    public static final int FLOOR_FOREST_GROUND = 5;
    public static final int FLOOR_FOREST_LEAVES = 6;
    public static final int FLOOR_FOREST_MUSHROOMS = 7;
    public static final int WALL_FOREST_TREE = 8;
    public static final int WALL_FOREST_LOG = 9;
    
    // Cave biome tiles
    public static final int FLOOR_CAVE_STONE = 10;
    public static final int FLOOR_CAVE_CRYSTAL = 11;
    public static final int FLOOR_CAVE_WATER = 12;
    public static final int WALL_CAVE_STALAGMITE = 13;
    public static final int WALL_CAVE_CRYSTAL_FORMATION = 14;
    
    // Desert biome tiles
    public static final int FLOOR_DESERT_SAND = 15;
    public static final int FLOOR_DESERT_ROCK = 16;
    public static final int FLOOR_DESERT_CACTUS = 17;
    public static final int WALL_DESERT_ROCK_FORMATION = 18;

    private final int width;
    private final int height;
    private final int[][] tiles;
    private final double[][] wallHealth;
    private static final double WALL_MAX_HP = 4.0;
    
    private final Biome biome;
    private final Random rng;

    /** Returns true if the given tile id should hide the player from enemies. */
    public static boolean isHidingTileId(int tileId) {
        return tileId == FLOOR_PLANTS || 
               tileId == FLOOR_FOREST_LEAVES || 
               tileId == FLOOR_FOREST_MUSHROOMS ||
               tileId == WALL_CAVE_CRYSTAL_FORMATION;
    }
    
    /**
     * Returns true if the given tile id is a wall that can be destroyed.
     */
    public static boolean isDestructibleWall(int tileId) {
        return tileId == WALL || 
               tileId == WALL_FOREST_TREE || 
               tileId == WALL_FOREST_LOG ||
               tileId == WALL_CAVE_STALAGMITE ||
               tileId == WALL_DESERT_ROCK_FORMATION;
    }

    /**
     * Creates a tile map with non-deterministic random number generation.
     */
    public TileMap(int[][] tiles) {
        this(tiles, Biome.MEADOWS, new Random());
    }

    /**
     * Creates a tile map with the specified random number generator.
     */
    public TileMap(int[][] tiles, Random rng) {
        this(tiles, Biome.MEADOWS, rng);
    }
    
    /**
     * Creates a tile map with the specified biome and random number generator.
     */
    public TileMap(int[][] tiles, Biome biome, Random rng) {
        this.tiles = tiles;
        this.width = tiles.length;
        this.height = tiles[0].length;
        this.biome = biome;
        this.rng = (rng != null) ? rng : new Random();
        this.wallHealth = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (isDestructibleWall(tiles[x][y])) {
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

    /**
     * Gets the map width in tiles.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the map height in tiles.
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets the biome of this tile map.
     */
    public Biome getBiome() {
        return biome;
    }

    /**
     * Checks if the specified tile is a wall.
     */
    public boolean isWall(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return true;
        int t = tiles[x][y];
        return t == WALL || t == EDGE_WALL || 
               t == WALL_FOREST_TREE || t == WALL_FOREST_LOG ||
               t == WALL_CAVE_STALAGMITE || t == WALL_CAVE_CRYSTAL_FORMATION ||
               t == WALL_DESERT_ROCK_FORMATION;
    }

    /**
     * Applies damage to a wall tile, destroying it if health reaches zero.
     */
    public boolean damageWall(int x, int y, double damage) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        if (!isDestructibleWall(tiles[x][y]) || wallHealth[x][y] <= 0) return false;
        if (wallHealth[x][y] == -1) return false; // Indestructible
        wallHealth[x][y] -= damage;
        if (wallHealth[x][y] <= 0) {
            wallHealth[x][y] = 0;
            // Convert to appropriate floor tile based on biome
            tiles[x][y] = getBiomeFloorTile();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the appropriate floor tile for the current biome when a wall is destroyed.
     */
    private int getBiomeFloorTile() {
        return switch (biome) {
            case FOREST -> FLOOR_FOREST_GROUND;
            case CAVE -> FLOOR_CAVE_STONE;
            case DESERT -> FLOOR_DESERT_SAND;
            default -> FLOOR_GRASS; // MEADOWS and fallback
        };
    }
    
    /**
     * Gets the appropriate texture for a tile based on the current biome.
     */
    private BufferedImage getBiomeTexture(int tileId, boolean isWall, int tick30) {
        if (isWall) {
            return getBiomeWallTexture(tileId);
        } else {
            return getBiomeFloorTexture(tileId, tick30);
        }
    }
    
    /**
     * Gets the appropriate wall texture for the current biome.
     */
    private BufferedImage getBiomeWallTexture(int tileId) {
        return switch (tileId) {
            case EDGE_WALL -> TextureManager.getStoneTexture(); // Always stone for edge walls
            case WALL_FOREST_TREE -> TextureManager.getForestTreeTexture();
            case WALL_FOREST_LOG -> TextureManager.getForestLogTexture();
            case WALL_CAVE_STALAGMITE -> TextureManager.getCaveStalagmiteTexture();
            case WALL_CAVE_CRYSTAL_FORMATION -> TextureManager.getCaveCrystalFormationTexture();
            case WALL_DESERT_ROCK_FORMATION -> TextureManager.getDesertRockFormationTexture();
            default -> TextureManager.getStoneTexture(); // Default stone texture
        };
    }
    
    /**
     * Gets the appropriate floor texture for the current biome.
     */
    private BufferedImage getBiomeFloorTexture(int tileId, int tick30) {
        return switch (tileId) {
            case FLOOR_DIRT -> TextureManager.getDirtTexture();
            case FLOOR_PLANTS -> TextureManager.getPlantsTexture();
            case FLOOR_FOREST_GROUND -> TextureManager.getForestGroundTexture();
            case FLOOR_FOREST_LEAVES -> TextureManager.getForestLeavesTexture();
            case FLOOR_FOREST_MUSHROOMS -> TextureManager.getForestMushroomsTexture();
            case FLOOR_CAVE_STONE -> TextureManager.getCaveStoneTexture();
            case FLOOR_CAVE_CRYSTAL -> TextureManager.getCaveCrystalTexture();
            case FLOOR_CAVE_WATER -> TextureManager.getCaveWaterTexture();
            case FLOOR_DESERT_SAND -> TextureManager.getDesertSandTexture();
            case FLOOR_DESERT_ROCK -> TextureManager.getDesertRockTexture();
            case FLOOR_DESERT_CACTUS -> TextureManager.getDesertCactusTexture();
            default -> TextureManager.getGrassTextureFrame(tick30); // Default grass texture
        };
    }

    /**
     * Draws the tile map (backward compatibility method).
     */
    public void draw(Graphics2D g, int camX, int camY, int viewW, int viewH) {
        draw(g, camX, camY, viewW, viewH, 0);
    }

    /**
     * Draws the tile map with animation support.
     */
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
                boolean isWall = isWall(x, y);

                BufferedImage tex = getBiomeTexture(t, isWall, tick30);

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

    /**
     * Finds a suitable spawn tile near the center of the map.
     */
    public int[] findSpawnTile() {
        for (int y = height / 2 - 1; y <= height / 2 + 1; y++)
            for (int x = width / 2 - 1; x <= width / 2 + 1; x++)
                if (!isWall(x, y)) return new int[]{x, y};

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (!isWall(x, y)) return new int[]{x, y};

        return new int[]{1, 1};
    }

    /**
     * Finds a random floor tile at least the specified distance from the given position.
     */
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

    /**
     * Gets a random floor tile from the map.
     */
    public int[] getRandomFloorTile() {
        for (int attempts = 0; attempts < 1000; attempts++) {
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);
            if (!isWall(x, y)) return new int[]{x, y};
        }
        return findSpawnTile();
    }

    /**
     * Gets the tile ID at the specified tile coordinates.
     */
    public int getTileAt(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return EDGE_WALL;
        return tiles[tx][ty];
    }

    /**
     * Checks if the world position is on a hiding tile.
     */
    public boolean isHidingAtWorld(double wx, double wy) {
        int tx = (int) Math.floor(wx / (double) Constants.TILE_SIZE);
        int ty = (int) Math.floor(wy / (double) Constants.TILE_SIZE);
        int t = getTileAt(tx, ty);
        return isHidingTileId(t);
    }
}
