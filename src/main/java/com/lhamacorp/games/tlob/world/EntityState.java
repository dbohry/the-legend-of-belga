package com.lhamacorp.games.tlob.world;

import com.lhamacorp.games.tlob.entities.Entity;

public record EntityState(int id, byte type, float x, float y, float hp, float shield, float stamina) {

    public static final byte PLAYER = 1;
    public static final byte ENEMY = 2;

    public static EntityState of(int id, byte type, Entity e) {
        return new EntityState(id, type, (float) e.getX(), (float) e.getY(),
            (float) e.getHealth(), (float) e.getShield(), (float) e.getStamina());
    }

    public boolean isPlayer() {
        return type == PLAYER;
    }

    public boolean isEnemy() {
        return type == ENEMY;
    }
}
