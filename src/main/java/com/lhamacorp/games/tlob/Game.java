package com.lhamacorp.games.tlob;

import com.lhamacorp.games.tlob.managers.AudioManager;
import com.lhamacorp.games.tlob.managers.GameManager;

import javax.swing.*;

public class Game {
    public static void main(String[] args) {
        JFrame window = new JFrame("The Legend of Belga");
        GameManager panel = new GameManager();
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        panel.startGameThread();
        AudioManager.playRandomMusic(-10.0f);
    }
}
