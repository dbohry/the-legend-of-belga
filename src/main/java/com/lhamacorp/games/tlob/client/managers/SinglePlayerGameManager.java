package com.lhamacorp.games.tlob.client.managers;

import java.awt.*;

public class SinglePlayerGameManager extends BaseGameManager {

    public SinglePlayerGameManager() {
        this(readSeed());
    }

    public SinglePlayerGameManager(long seed) {
        super();
        initWorld(seed); // builds map & player
        // now populate SP enemies
        enemySpawner.spawn(levelManager.map(), player, enemies, levelManager.completed(), TILE_SIZE);
        enemiesAtLevelStart = enemies.size();
    }

    @Override
    protected void updatePlaying(Point aimWorld) {
        if (!player.isAlive()) { enterGameOver(); return; }
        if (enemies.isEmpty()) { enterVictory(); return; }

        player.update(input, levelManager.map(), enemies, aimWorld);
        for (int i = enemies.size() - 1; i >= 0; i--) {
            var e = enemies.get(i);
            e.update(player, levelManager.map());
            if (!e.isAlive()) enemies.remove(i);
        }
    }
}
