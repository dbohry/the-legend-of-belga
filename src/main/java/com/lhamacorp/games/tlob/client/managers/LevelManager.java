package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.Biome;
import com.lhamacorp.games.tlob.client.maps.MapGenerator;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.List;
import java.util.Random;

public final class LevelManager {

    private final int width, height;
    private final Random mapsRoot;

    private TileMap current;
    private int completed;
    private Biome currentBiome;

    // Back-compat ctor (non-deterministic)
    public LevelManager(int width, int height) {
        this(width, height, new Random());
    }

    public LevelManager(int width, int height, Random mapsRoot) {
        this.width = width;
        this.height = height;
        this.mapsRoot = (mapsRoot != null) ? mapsRoot : new Random();
        this.currentBiome = Biome.MEADOWS;
        this.current = buildNewMap();
        this.completed = 0;
    }

    private TileMap buildNewMap() {
        // derive independent substreams so generator and map helpers don't interfere
        Random genRng = new Random(mapsRoot.nextLong());
        Random tileRng = new Random(mapsRoot.nextLong());
        int[][] tiles = new MapGenerator(width, height, currentBiome, genRng).generate();
        return new TileMap(tiles, currentBiome, tileRng);
    }

    public TileMap map() {
        return current;
    }

    public int completed() {
        return completed;
    }
    
    public Biome getCurrentBiome() {
        return currentBiome;
    }
    
    /**
     * Gets the biome for a specific level number.
     * Biomes cycle every 3 levels.
     */
    public Biome getBiomeForLevel(int level) {
        Biome[] biomes = Biome.values();
        int biomeIndex = (level / 3) % biomes.length;
        return biomes[biomeIndex];
    }

    public void restart(Player player, SpawnManager spawner, List<Entity> enemies, int tileSize) {
        this.completed = 0;
        this.currentBiome = Biome.MEADOWS;
        this.current = buildNewMap();
        placePlayer(player, tileSize);
        player.heal();
        spawner.spawn(current, player, enemies, completed, tileSize);
    }

    public void nextLevel(Player player, SpawnManager spawner,
                         List<Entity> enemies, int tileSize) {
        this.completed++;
        this.currentBiome = getBiomeForLevel(completed);
        this.current = buildNewMap();
        placePlayer(player, tileSize);
        player.restoreAll();
        spawner.spawn(current, player, enemies, completed, tileSize);
    }

    private void placePlayer(Player player, int tileSize) {
        int[] spawn = current.findSpawnTile();
        player.setPosition(spawn[0] * tileSize + tileSize / 2.0,
            spawn[1] * tileSize + tileSize / 2.0);
    }
}
