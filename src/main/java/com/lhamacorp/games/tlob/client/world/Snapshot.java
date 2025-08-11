package com.lhamacorp.games.tlob.client.world;


import java.util.List;

/** Wire DTOs for snapshots; tiny on purpose. */
public record Snapshot(int serverTick, List<EntityState> entities) {}

