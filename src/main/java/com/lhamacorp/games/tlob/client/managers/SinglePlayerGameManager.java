package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.save.SaveManager;
import com.lhamacorp.games.tlob.client.save.SaveState;

import java.awt.*;

public class SinglePlayerGameManager extends BaseGameManager {

    private final SaveManager saveManager;
    private int saveIndicatorTicks = 0;
    private static final int SAVE_INDICATOR_DURATION = 120; // 2 seconds at 60 FPS

    public SinglePlayerGameManager() {
        this(readSeed());
    }

    public SinglePlayerGameManager(long seed) {
        super();
        this.saveManager = new SaveManager();
        initWorld(seed); // builds map & player
        // now populate SP enemies
        enemySpawner.spawn(levelManager.map(), player, enemies, levelManager.completed(), TILE_SIZE);
        enemiesAtLevelStart = enemies.size();
    }

    /**
     * Constructor for loading from a save state.
     * @param saveState the save state to load from
     */
    public SinglePlayerGameManager(SaveState saveState) {
        super();
        this.saveManager = new SaveManager();
        initWorld(saveState.getWorldSeed());
        
        // Set the completed maps count from save
        for (int i = 0; i < saveState.getCompletedMaps(); i++) {
            levelManager.nextLevel(player, enemySpawner, enemies, TILE_SIZE);
        }
        
        // Spawn enemies for the current level
        enemySpawner.spawn(levelManager.map(), player, enemies, levelManager.completed(), TILE_SIZE);
        enemiesAtLevelStart = enemies.size();
    }

    @Override
    protected void updatePlaying(Point aimWorld) {
        if (!player.isAlive()) { enterGameOver(); return; }
        if (enemies.isEmpty()) { enterVictory(); return; }

        player.update(input, levelManager.map(), enemies, aimWorld);
        for (int i = enemies.size() - 1; i >= 0; i--) {
            var e = enemies.get(i);
            e.update(player, levelManager.map());
            if (!e.isAlive()) enemies.remove(i);
        }
        
        // Update save indicator
        if (saveIndicatorTicks > 0) {
            saveIndicatorTicks--;
        }
    }

    /**
     * Saves the current game state.
     * @return true if save was successful, false otherwise
     */
    public boolean saveGameState() {
        SaveState saveState = new SaveState(worldSeed, levelManager.completed());
        boolean success = saveManager.saveGame(saveState);
        if (success) {
            saveIndicatorTicks = SAVE_INDICATOR_DURATION;
        }
        return success;
    }

    /**
     * Gets the save manager instance.
     * @return the save manager
     */
    public SaveManager getSaveManager() {
        return saveManager;
    }

    @Override
    protected void autoSave() {
        saveGameState();
    }

    @Override
    protected void saveGame() {
        if (saveGameState()) {
            // Visual feedback is handled by saveIndicatorTicks
        } else {
            System.err.println("Failed to save game!");
        }
    }

    /**
     * Checks if the save indicator should be shown.
     * @return true if the save indicator should be visible
     */
    public boolean isSaveIndicatorVisible() {
        return saveIndicatorTicks > 0;
    }

    /**
     * Gets the save indicator alpha value for fading effect.
     * @return alpha value between 0.0 and 1.0
     */
    public float getSaveIndicatorAlpha() {
        if (saveIndicatorTicks <= 0) return 0.0f;
        if (saveIndicatorTicks > SAVE_INDICATOR_DURATION * 0.8f) return 1.0f;
        return (float) saveIndicatorTicks / (SAVE_INDICATOR_DURATION * 0.8f);
    }
}
