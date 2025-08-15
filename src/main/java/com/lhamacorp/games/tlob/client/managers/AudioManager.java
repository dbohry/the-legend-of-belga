package com.lhamacorp.games.tlob.client.managers;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioManager {

    private static Clip musicClip = null;
    private static final String SOUND_ASSETS_DIR = "/assets/sfx/";
    private static final String[] PLAYLIST = {
        "music-adventurer.wav",
        "music-adventurer2.wav",
        // "music-bossfight.wav",
        "music-upbeat.wav"
    };
    
    // Mute functionality
    private static float originalVolumeDb = -10.0f; // Default volume
    private static boolean isMuted = false;
    private static final float MUTED_VOLUME_DB = -80.0f; // Very quiet when muted

    public static void playRandomMusic(float volumeDb) {
        int index = (int) (Math.random() * PLAYLIST.length);
        playMusic(PLAYLIST[index], volumeDb);
    }

    public static void playMusic(String filename, float volumeDb) {
        stopMusic();

        try (InputStream audioStream = AudioManager.class.getResourceAsStream(SOUND_ASSETS_DIR + filename)) {

            if (audioStream == null) {
                System.err.println("Audio file not found in JAR: " + filename);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new BufferedInputStream(audioStream));
            musicClip = AudioSystem.getClip();
            musicClip.open(audioIn);

            if (musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                // Store the original volume and apply it (or muted volume if currently muted)
                originalVolumeDb = volumeDb;
                float volumeToApply = isMuted ? MUTED_VOLUME_DB : volumeDb;
                gainControl.setValue(volumeToApply);
            }

            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicClip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing music: " + filename);
        }
    }

    public static void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
    }
    
    /**
     * Toggles mute state. When muted, volume is set very low.
     * When unmuted, original volume is restored.
     */
    public static void toggleMute() {
        isMuted = !isMuted;
        if (musicClip != null && musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            float volumeToApply = isMuted ? MUTED_VOLUME_DB : originalVolumeDb;
            gain.setValue(volumeToApply);
        }
    }
    
    /**
     * Gets the current mute state.
     */
    public static boolean isMuted() {
        return isMuted;
    }
    
    /**
     * Gets the original volume (before muting).
     */
    public static float getOriginalVolume() {
        return originalVolumeDb;
    }

    public static void playSound(String filename) {
        playSound(filename, -10.0f);
    }

    public static void playSound(String filename, float volumeDb) {
        try (InputStream audioStream = AudioManager.class.getResourceAsStream(SOUND_ASSETS_DIR + filename)) {

            if (audioStream == null) {
                System.err.println("Audio file not found in JAR: " + filename);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new BufferedInputStream(audioStream));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volumeDb);
            }

            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing sound: " + filename);
        }
    }

    public static void setMusicVolume(float volumeDb) {
        // Always update the stored volume
        originalVolumeDb = volumeDb;
        
        // Apply the volume to the active music clip if it exists
        if (musicClip != null && musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), volumeDb));
            float volumeToApply = isMuted ? MUTED_VOLUME_DB : clamped;
            gain.setValue(volumeToApply);
        }
    }
}
