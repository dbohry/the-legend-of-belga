package com.lhamacorp.games.tlob.client.world;

public final class InputState {
    public boolean up, down, left, right;
    public boolean attack, shift, defense;

    public void clear() {
        up = down = left = right = attack = shift = defense = false;
    }
}