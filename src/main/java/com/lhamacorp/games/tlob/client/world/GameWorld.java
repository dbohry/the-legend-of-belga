package com.lhamacorp.games.tlob.client.world;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.TileMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Authoritative, UI-free simulation. One call to tick() = one sim step. */
public final class GameWorld {

    /** 30 Hz fixed tick for the sim. Rendering can stay at 60 FPS. */
    public static final int TICK_RATE = 30;

    private final TileMap map;
    private final Player player;
    private final List<Entity> enemies;
    private final Random rng;
    private final IdAllocator ids = new IdAllocator();
    private int tick;

    /** Commands collected for the current tick (usually 0–2 items). */
    public void tick(List<InputCommand> commands) {
        // Collapse commands into a simple per-tick input snapshot for the player
        var input = VirtualInput.from(commands);

        // Player update (uses only sim data; no Swing)
        player.update(input, map, enemies);

        // Enemies
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Entity e = enemies.get(i);
            e.update(player, map);
            if (!e.isAlive()) enemies.remove(i);
        }
        tick++;
    }

    public Snapshot snapshot() {
        List<EntityState> states = new ArrayList<>(1 + enemies.size());
        states.add(EntityState.of(ids.id(player), EntityState.PLAYER, player));
        for (Entity e : enemies) states.add(EntityState.of(ids.id(e), EntityState.ENEMY, e));
        return new Snapshot(tick, states);
    }

    /* ------------ accessors (renderer can keep using your existing objects) ------------ */
    public TileMap map() {
        return map;
    }

    public Player player() {
        return player;
    }

    public List<Entity> enemies() {
        return enemies;
    }

    public int tick() {
        return tick;
    }

    public Random rng() {
        return rng;
    }

    public GameWorld(TileMap map, Player player, List<Entity> enemies, long seed) {
        this.map = Objects.requireNonNull(map);
        this.player = Objects.requireNonNull(player);
        this.enemies = Objects.requireNonNull(enemies);
        this.rng = new Random(seed);
    }

    /** Simple ID allocator without touching entity classes. */
    public static final class IdAllocator {
        private final IdentityHashMap<Object, Integer> ids = new IdentityHashMap<>();
        private final AtomicInteger next = new AtomicInteger(1);

        public int id(Object o) {
            return ids.computeIfAbsent(o, k -> next.getAndIncrement());
        }
    }
}
