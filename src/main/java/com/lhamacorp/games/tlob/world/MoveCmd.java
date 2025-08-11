package com.lhamacorp.games.tlob.world;

/** Per-tick movement, with sprint flag. dx/dy in {-1,0,1}. */
public record MoveCmd(int dx, int dy, boolean sprint) implements InputCommand {}
