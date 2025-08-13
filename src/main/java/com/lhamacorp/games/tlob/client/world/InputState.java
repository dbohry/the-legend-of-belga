package com.lhamacorp.games.tlob.client.world;

public final class InputState {
    public boolean up, down, left, right;
    public boolean attack, shift, block;

    public void clear() {
        up = down = left = right = attack = shift = block = false;
    }
}