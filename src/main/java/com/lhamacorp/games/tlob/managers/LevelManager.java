package com.lhamacorp.games.tlob.managers;

import com.lhamacorp.games.tlob.entities.Enemy;
import com.lhamacorp.games.tlob.entities.Player;
import com.lhamacorp.games.tlob.maps.MapGenerator;
import com.lhamacorp.games.tlob.maps.TileMap;

import java.util.List;

public final class LevelManager {
    private final int width, height;
    private final double density;
    private final int carveSteps;
    private TileMap current;
    private int completed;

    public LevelManager(int width, int height, double density, int carveSteps) {
        this.width = width;
        this.height = height;
        this.density = density;
        this.carveSteps = carveSteps;
        this.current = new TileMap(new MapGenerator(width, height, density, carveSteps).generate());
        this.completed = 0;
    }

    public TileMap map() {
        return current;
    }

    public int completed() {
        return completed;
    }

    public void restart(Player player, SpawnManager spawner, List<Enemy> enemies, int tileSize) {
        this.completed = 0;
        this.current = new TileMap(new MapGenerator(width, height, density, carveSteps).generate());
        placePlayer(player, tileSize);
        player.heal();
        spawner.spawn(current, player, enemies, completed, tileSize);
    }

    public void nextLevel(Player player, SpawnManager spawner, List<Enemy> enemies, int tileSize) {
        this.completed++;
        this.current = new TileMap(new MapGenerator(width, height, density, carveSteps).generate());
        placePlayer(player, tileSize);
        player.restoreAll();
        spawner.spawn(current, player, enemies, completed, tileSize);
    }

    private void placePlayer(Player player, int tileSize) {
        int[] spawn = current.findSpawnTile();
        player.setPosition(spawn[0] * tileSize + tileSize / 2.0, spawn[1] * tileSize + tileSize / 2.0);
    }
}
