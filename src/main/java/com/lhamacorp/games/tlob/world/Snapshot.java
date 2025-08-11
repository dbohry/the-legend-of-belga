package com.lhamacorp.games.tlob.world;


import java.util.List;

/** Wire DTOs for snapshots; tiny on purpose. */
public record Snapshot(int serverTick, List<EntityState> entities) {}

