package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.save.SaveManager;
import com.lhamacorp.games.tlob.client.save.SaveState;
import com.lhamacorp.games.tlob.client.save.ActivePerks;
import com.lhamacorp.games.tlob.client.save.AppliedPerk;
import com.lhamacorp.games.tlob.client.perks.Perk;

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
            for (AppliedPerk appliedPerk : saveState.getActivePerks().getAppliedPerks()) {
                this.activePerks.addPerk(appliedPerk);
            }
            applyActivePerksToPlayer();
        }
        
        // Spawn enemies for the current level
        enemySpawner.spawn(levelManager.map(), player, enemies, levelManager.completed(), TILE_SIZE);
        enemiesAtLevelStart = enemies.size();
    }

    /**
     * Applies all active perks to the player.
     * This method recreates the perk effects by applying them in sequence with their exact values.
     */
    private void applyActivePerksToPlayer() {
        for (AppliedPerk appliedPerk : activePerks.getAppliedPerks()) {
            applyPerkByTypeAndValue(appliedPerk.getPerkType(), appliedPerk.getValue());
        }
    }



    @Override
    protected void updatePlaying(Point aimWorld) {
        if (!player.isAlive()) { enterGameOver(); return; }
        if (enemies.isEmpty()) { enterVictory(); return; }

        player.update(input, levelManager.map(), enemies, aimWorld);
        for (int i = enemies.size() - 1; i >= 0; i--) {
            var e = enemies.get(i);
            e.update(player, levelManager.map(), enemies);
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



    @Override
    protected void autoSave() {
        saveGameState();
    }



    @Override
    protected void applyPerkAndContinue(int index) {
        var applied = perkManager.applyChoice(index, player);
        if (applied != null) {
            // Get the perk type and extract the actual value that was applied
            String perkType = getPerkType(applied);
            Double perkValue = extractPerkValue(applied);
            
            if (perkType != null && perkValue != null) {
                // Create an AppliedPerk with the exact value that was applied
                AppliedPerk appliedPerk = new AppliedPerk(perkType, perkValue);
                activePerks.addPerk(appliedPerk);
            }
            
            // Auto-save immediately after applying perk (before starting next level)
            // This ensures the active perks are captured in the save
            autoSave();
            startNextLevel();
        }
    }

    /**
     * Determines the perk type based on the applied perk.
     * @param perk the perk that was applied
     * @return the perk type identifier, or null if unknown
     */
    private String getPerkType(Perk perk) {
        String name = perk.name;
        if (name.contains("Max Life")) return "MAX_HEALTH";
        if (name.contains("Max Stamina")) return "MAX_STAMINA";
        if (name.contains("Max Mana")) return "MAX_MANA";
        if (name.contains("Shield")) return "SHIELD";
        if (name.contains("Weapon Width")) return "WEAPON_WIDTH";
        if (name.contains("Movement Speed")) return "MOVE_SPEED";
        if (name.contains("Stamina Regen")) return "STAMINA_REGEN";
        if (name.contains("Mana Regen")) return "MANA_REGEN";
        if (name.contains("Weapon Damage")) return "WEAPON_DAMAGE";
        if (name.contains("Weapon Range")) return "WEAPON_RANGE";
        return null;
    }
    
    /**
     * Extracts the actual value that was applied by the perk.
     * This method parses the perk description to find the percentage or value.
     * @param perk the perk that was applied
     * @return the actual value that was applied, or null if cannot be determined
     */
    private Double extractPerkValue(Perk perk) {
        String desc = perk.description;
        
        // Handle percentage-based perks
        if (desc.contains("%")) {
            // Extract percentage from description like "Increases maximum life (+15%)"
            int start = desc.indexOf("(+");
            int end = desc.indexOf("%");
            if (start != -1 && end != -1) {
                try {
                    String percentStr = desc.substring(start + 2, end);
                    return Double.parseDouble(percentStr) / 100.0; // Convert to decimal
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse percentage from: " + desc);
                }
            }
        }
        
        // Handle fixed value perks by checking both name and description
        String name = perk.name;
        if (name.contains("Shield")) return 1.0;
        if (name.contains("Weapon Width")) return 1.0;
        
        // Also check description for +1 perks
        if (desc.contains("+1")) {
            if (desc.contains("shield") || desc.contains("Shield")) return 1.0;
        }
        
        // Default values for perks where we can't extract the exact value
        // These should match the default values used in PerkManager
        if (name.contains("Max Life")) return 0.15; // Default 15%
        if (name.contains("Max Stamina")) return 0.15; // Default 15%
        if (name.contains("Max Mana")) return 0.15; // Default 15%
        if (name.contains("Movement Speed")) return 0.075; // Default 7.5%
        if (name.contains("Stamina Regen")) return 0.075; // Default 7.5%
        if (name.contains("Mana Regen")) return 0.15; // Default 15%
        if (name.contains("Weapon Damage")) return 0.15; // Default 15%
        if (name.contains("Weapon Range")) return 0.075; // Default 7.5%
        
        return null;
    }
    
    /**
     * Applies a specific perk by its type and value.
     * This method applies the exact value that was originally applied.
     * @param perkType the type of perk to apply
     * @param value the exact value to apply
     */
    private void applyPerkByTypeAndValue(String perkType, double value) {
        switch (perkType) {
            case "MAX_HEALTH":
                player.increaseMaxHealthByPercent(value);
                break;
            case "MAX_STAMINA":
                player.increaseMaxStaminaByPercent(value);
                break;
            case "MAX_MANA":
                player.increaseMaxManaByPercent(value);
                break;
            case "SHIELD":
                // Shield is always +1, so we can ignore the value
                player.increaseMaxShield(1.0);
                break;
            case "WEAPON_WIDTH":
                // Weapon width is always +1, so we can ignore the value
                player.increaseWeaponWidth(1);
                break;
            case "STAMINA_REGEN":
                player.increaseStaminaRegenByPercent(value);
                break;
            case "MANA_REGEN":
                player.increaseManaRegenByPercent(value);
                break;
            case "MOVE_SPEED":
                player.increaseMoveSpeedByPercent(value);
                break;
            case "WEAPON_DAMAGE":
                player.increaseAttackDamageByPercent(value);
                break;
            case "WEAPON_RANGE":
                player.increaseWeaponRangeByPercent(value);
                break;
            default:
                System.out.println("Unknown perk type: " + perkType);
                break;
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

    @Override
    protected void reloadSaveGame() {
        // Reload the save game to restore player progress and perks
        SaveManager saveManager = new SaveManager();
        SaveState saveState = saveManager.loadGame();
        
        if (saveState != null) {
            // The perks are already applied from the save, so we just need to restore current stats
            player.heal(); // Restore health
            player.restoreAll(); // Restore stamina, mana, shield
            
            // Now handle the level progression
            // We need to advance to the correct level based on completed maps
            int targetCompletedMaps = saveState.getCompletedMaps();
            
            // Start from level 0 and advance to the target
            levelManager.restart(player, enemySpawner, enemies, TILE_SIZE);
            
            // Advance through all completed maps to reach the target level
            for (int i = 0; i < targetCompletedMaps; i++) {
                levelManager.nextLevel(player, enemySpawner, enemies, TILE_SIZE);
            }
            
            // Reset game state
            state = GameState.PLAYING;
            enemiesAtLevelStart = enemies.size();
            animTick30 = 0;
            simTick = 0;
            
            // Update camera
            int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
            int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
            camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
            requestFocusInWindow();
        } else {
            // If no save exists, fall back to regular restart
            super.restartGame();
        }
    }
}
