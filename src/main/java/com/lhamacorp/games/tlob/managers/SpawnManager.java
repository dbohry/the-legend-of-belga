package com.lhamacorp.games.tlob.managers;

import com.lhamacorp.games.tlob.entities.Enemy;
import com.lhamacorp.games.tlob.entities.Player;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.weapons.Weapon;

import java.util.List;
import java.util.Random;

public class SpawnManager {

    private final Weapon enemyWeapon;
    private Random rng;

    // Back-compat ctor (non-deterministic)
    public SpawnManager(Weapon enemyWeapon) {
        this(enemyWeapon, new Random());
    }

    public SpawnManager(Weapon enemyWeapon, Random rng) {
        this.enemyWeapon = enemyWeapon;
        this.rng = (rng != null) ? rng : new Random();
    }

    /** Reseed this spawner’s RNG (used on restart / next-level for deterministic runs). */
    public void reseed(Random rng) {
        this.rng = (rng != null) ? rng : new Random();
    }

    public void spawn(TileMap map, Player player, List<Enemy> out, int completedMaps, int tileSize) {
        out.clear();
        int base = 3 + rng.nextInt(6); // 3..8
        double mult = Math.pow(1.4, completedMaps);
        int count = Math.max(1, (int) (base * mult));

        for (int i = 0; i < count; i++) {
            int[] pos = map.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * tileSize);
            if (pos == null) pos = map.getRandomFloorTile();
            if (pos != null && !map.isWall(pos[0], pos[1])) {
                out.add(new Enemy(
                    pos[0] * tileSize + tileSize / 2.0,
                    pos[1] * tileSize + tileSize / 2.0,
                    enemyWeapon
                ));
            }
        }
    }
}
