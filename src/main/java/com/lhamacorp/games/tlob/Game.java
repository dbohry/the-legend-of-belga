package com.lhamacorp.games.tlob;

import javax.swing.*;

public class Game {
    public static void main(String[] args) {
        JFrame window = new JFrame("The Legend of Belga");
        GamePanel panel = new GamePanel();
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        panel.startGameThread();
        AudioManager.playRandomMusic(-8.0f);
    }
}
