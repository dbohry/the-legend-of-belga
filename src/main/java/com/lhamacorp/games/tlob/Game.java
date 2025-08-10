package com.lhamacorp.games.tlob;

import com.lhamacorp.games.tlob.managers.AudioManager;
import com.lhamacorp.games.tlob.managers.GameManager;

import javax.swing.*;

public class Game {
    public static void main(String[] args) {
        String appName = "The Legend of Belga";
        String version = Game.class.getPackage().getImplementationVersion();
        if (version == null) version = "dev";

        JFrame window = new JFrame(appName + " v" + version);
        GameManager panel = new GameManager();
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        panel.startGameThread();
    }
}