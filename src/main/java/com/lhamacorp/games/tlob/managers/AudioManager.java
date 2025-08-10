package com.lhamacorp.games.tlob.managers;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioManager {

    private static Clip musicClip = null;
    private static final String SOUND_ASSETS_DIR = "/assets/sfx/";
    private static final String[] PLAYLIST = {
        "background-music-1.wav",
    };

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
                gainControl.setValue(volumeDb);
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

}
