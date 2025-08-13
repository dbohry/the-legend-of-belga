package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.save.SaveManager;
import com.lhamacorp.games.tlob.client.save.SaveState;
import com.lhamacorp.games.tlob.client.save.ActivePerks;

import java.awt.*;

public class SinglePlayerGameManager extends BaseGameManager {

    private final SaveManager saveManager;
    private final ActivePerks activePerks;
    private int saveIndicatorTicks = 0;
    private static final int SAVE_INDICATOR_DURATION = 120; // 2 seconds at 60 FPS

    public SinglePlayerGameManager() {
        this(readSeed());
    }

    public SinglePlayerGameManager(long seed) {
        super();
        this.saveManager = new SaveManager();
        this.activePerks = new ActivePerks();
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
        this.activePerks = new ActivePerks();
        initWorld(saveState.getWorldSeed());
        
        // Set the completed maps count from save
        for (int i = 0; i < saveState.getCompletedMaps(); i++) {
            levelManager.nextLevel(player, enemySpawner, enemies, TILE_SIZE);
        }
        
        // Restore active perks from save and apply them to the player
        if (saveState.getActivePerks() != null) {
            this.activePerks.clear();
            for (String perkId : saveState.getActivePerks().getPerkIds()) {
                this.activePerks.addPerk(perkId);
            }
            applyActivePerksToPlayer();
        }
        
        // Spawn enemies for the current level
        enemySpawner.spawn(levelManager.map(), player, enemies, levelManager.completed(), TILE_SIZE);
        enemiesAtLevelStart = enemies.size();
    }

    /**
     * Applies all active perks to the player.
     * This method recreates the perk effects by applying them in sequence.
     */
    private void applyActivePerksToPlayer() {
        for (String perkId : activePerks.getPerkIds()) {
            applyPerkById(perkId);
        }
    }

    /**
     * Applies a specific perk by its ID.
     * @param perkId the identifier of the perk to apply
     */
    private void applyPerkById(String perkId) {
        switch (perkId) {
            case "MAX_HEALTH":
                player.increaseMaxHealthByPercent(0.15); // Default 15% increase
                break;
            case "MAX_STAMINA":
                player.increaseMaxStaminaByPercent(0.15); // Default 15% increase
                break;
            case "MAX_MANA":
                player.increaseMaxManaByPercent(0.15); // Default 15% increase
                break;
            case "SHIELD":
                player.increaseShield();
                break;
            case "WEAPON_WIDTH":
                player.increaseWeaponWidth();
                break;
            // Add more perk types as needed
            default:
                System.out.println("Unknown perk ID: " + perkId);
                break;
        }
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
        SaveState saveState = new SaveState(worldSeed, levelManager.completed(), activePerks);
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

    @Override
    protected void applyPerkAndContinue(int index) {
        var applied = perkManager.applyChoice(index, player);
        if (applied != null) {
            // Add the perk to active perks list
            String perkId = getPerkId(applied);
            if (perkId != null) {
                activePerks.addPerk(perkId);
            }
            
            // Auto-save immediately after applying perk (before starting next level)
            // This ensures the active perks are captured in the save
            autoSave();
            startNextLevel();
        }
    }

    /**
     * Determines the perk ID based on the applied perk.
     * @param perk the perk that was applied
     * @return the perk identifier, or null if unknown
     */
    private String getPerkId(com.lhamacorp.games.tlob.client.perks.Perk perk) {
        String name = perk.name;
        if (name.contains("Max Life")) return "MAX_HEALTH";
        if (name.contains("Max Stamina")) return "MAX_STAMINA";
        if (name.contains("Max Mana")) return "MAX_MANA";
        if (name.contains("Shield")) return "SHIELD";
        if (name.contains("Weapon Width")) return "WEAPON_WIDTH";
        if (name.contains("Movement Speed")) return "MOVE_SPEED";
        if (name.contains("Stamina Regeneration")) return "STAMINA_REGEN";
        if (name.contains("Mana Regeneration")) return "MANA_REGEN";
        if (name.contains("Weapon Damage")) return "WEAPON_DAMAGE";
        if (name.contains("Weapon Range")) return "WEAPON_RANGE";
        return null;
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
