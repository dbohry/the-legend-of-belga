package com.lhamacorp.games.tlob.client.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages game configuration settings that persist between game sessions.
 */
public class GameConfig {
    
    private static final String CONFIG_DIR = ".tlob";
    private static final String CONFIG_FILE = "config.properties";
    
    // Configuration options
    private boolean showEnemyBehaviorIndicators = false;
    private float musicVolumeDb = -10.0f;
    
    // Singleton instance
    private static GameConfig instance;
    
    private GameConfig() {
        loadConfig();
    }
    
    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }
    
    /**
     * Gets whether enemy behavior indicators should be shown.
     */
    public boolean isShowEnemyBehaviorIndicators() {
        return showEnemyBehaviorIndicators;
    }
    
    /**
     * Sets whether enemy behavior indicators should be shown.
     */
    public void setShowEnemyBehaviorIndicators(boolean show) {
        this.showEnemyBehaviorIndicators = show;
        saveConfig();
    }
    
    /**
     * Gets the music volume in decibels.
     */
    public float getMusicVolumeDb() {
        return musicVolumeDb;
    }
    
    /**
     * Sets the music volume in decibels.
     */
    public void setMusicVolumeDb(float volumeDb) {
        this.musicVolumeDb = volumeDb;
        saveConfig();
    }
    
    /**
     * Loads configuration from the config file.
     */
    private void loadConfig() {
        try {
            Path configPath = getConfigPath();
            if (Files.exists(configPath)) {
                Properties props = new Properties();
                try (FileInputStream input = new FileInputStream(configPath.toFile())) {
                    props.load(input);
                    
                    String showIndicators = props.getProperty("showEnemyBehaviorIndicators");
                    if (showIndicators != null) {
                        this.showEnemyBehaviorIndicators = Boolean.parseBoolean(showIndicators);
                    }
                    
                    String volumeDb = props.getProperty("musicVolumeDb");
                    if (volumeDb != null) {
                        this.musicVolumeDb = Float.parseFloat(volumeDb);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            // Use defaults if loading fails
        }
    }
    
    /**
     * Saves configuration to the config file.
     */
    private void saveConfig() {
        try {
            Path configDir = getConfigDir();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            Path configPath = getConfigPath();
            Properties props = new Properties();
            props.setProperty("showEnemyBehaviorIndicators", String.valueOf(showEnemyBehaviorIndicators));
            props.setProperty("musicVolumeDb", String.valueOf(musicVolumeDb));
            
            try (FileOutputStream output = new FileOutputStream(configPath.toFile())) {
                props.store(output, "The Legend of Belga Configuration");
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
    
    private Path getConfigDir() {
        return Paths.get(System.getProperty("user.home"), CONFIG_DIR);
    }
    
    private Path getConfigPath() {
        return getConfigDir().resolve(CONFIG_FILE);
    }
}
