package com.lhamacorp.games.tlob.client.save;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages saving and loading of game state for single player games.
 * Saves are stored in the user's home directory under .tlob/save.dat
 */
public class SaveManager {
    private static final String SAVE_DIR = ".tlob";
    private static final String SAVE_FILE = "save.dat";
    
    private final Path savePath;
    
    public SaveManager() {
        String userHome = System.getProperty("user.home");
        Path homePath = Paths.get(userHome);
        this.savePath = homePath.resolve(SAVE_DIR).resolve(SAVE_FILE);
    }
    
    /**
     * Saves the current game state to disk.
     * @param saveState the save state to persist
     * @return true if save was successful, false otherwise
     */
    public boolean saveGame(SaveState saveState) {
        try {
            // Ensure the save directory exists
            Files.createDirectories(savePath.getParent());
            
            // Write the save state
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(savePath)))) {
                out.writeObject(saveState);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Loads the saved game state from disk.
     * @return the loaded save state, or null if no save exists or loading fails
     */
    public SaveState loadGame() {
        if (!Files.exists(savePath)) {
            return null;
        }
        
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(savePath)))) {
            return (SaveState) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load game: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Checks if a save file exists.
     * @return true if a save file exists, false otherwise
     */
    public boolean hasSave() {
        return Files.exists(savePath);
    }
    
    /**
     * Deletes the save file.
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteSave() {
        try {
            if (Files.exists(savePath)) {
                Files.delete(savePath);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete save: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the path to the save file for display purposes.
     * @return the save file path as a string
     */
    public String getSavePath() {
        return savePath.toString();
    }
}
