package com.lhamacorp.games.tlob.client.world;

public interface PlayerInputView {
    boolean left();

    boolean right();

    boolean up();

    boolean down();

    boolean sprint();

    boolean attack();
    
    boolean defense();
}
