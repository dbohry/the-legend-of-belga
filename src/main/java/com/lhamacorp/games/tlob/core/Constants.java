package com.lhamacorp.games.tlob.core;

public final class Constants {
    private Constants() {}

    public static final int TILE_SIZE = 32;
    // The next two are client-only, but harmless to keep here for now.
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;
    
    // Configuration flags - these will be managed by GameConfig
    // Keeping for backward compatibility, but GameConfig takes precedence
    public static final boolean SHOW_ENEMY_BEHAVIOR_INDICATORS_DEFAULT = 
        Boolean.getBoolean("tlob.show.enemy.behavior");
}
