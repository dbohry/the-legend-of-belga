package com.lhamacorp.games.tlob.client;

import com.lhamacorp.games.tlob.client.managers.BaseGameManager;
import com.lhamacorp.games.tlob.client.managers.MultiplayerGameManager;
import com.lhamacorp.games.tlob.client.managers.SinglePlayerGameManager;

import javax.swing.*;
import java.awt.*;

public class Game {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String appName = "The Legend of Belga";
            String version = Game.class.getPackage() != null
                ? Game.class.getPackage().getImplementationVersion()
                : null;
            if (version == null || version.isBlank()) version = "dev";

            JFrame window = new JFrame(appName + " v" + version);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            window.setResizable(false);

            BaseGameManager panel = createManagerWithStartFlow(window);
            window.setContentPane(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            panel.startGameThread();
        });
    }

    private static BaseGameManager createManagerWithStartFlow(JFrame owner) {
        if (!isStartScreenEnabled()) {
            return new SinglePlayerGameManager();
        }

        String[] choices = {"Singleplayer", "Multiplayer", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            owner,
            "Choose a game mode:",
            "Start Game",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            choices,
            choices[0]
        );

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            System.exit(0);
        }

        if (choice == 0) {
            // Singleplayer
            return new SinglePlayerGameManager();
        }

        // Multiplayer
        String hero = prompt(owner, "Hero name:", "Hero");
        if (hero == null) System.exit(0);

        String hostPort = prompt(owner, "Server (host:port):", "localhost:7777");
        if (hostPort == null) System.exit(0);

        HostPort hp = parseHostPort(hostPort, 7777);
        return new MultiplayerGameManager(hp.host, hp.port, hero);
    }

    private static boolean isStartScreenEnabled() {
        String env = System.getenv("TLOB_START_SCREEN");
        return (env == null) || Boolean.parseBoolean(env);
    }

    private static String prompt(Component parent, String label, String def) {
        return (String) JOptionPane.showInputDialog(
            parent,
            label,
            "The Legend of Belga",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            def
        );
    }

    private record HostPort(String host, int port) {}

    private static HostPort parseHostPort(String s, int defPort) {
        String in = s.trim();
        if (in.isEmpty()) return new HostPort("localhost", defPort);
        int idx = in.lastIndexOf(':');
        if (idx <= 0 || idx == in.length() - 1) return new HostPort(in, defPort);
        String host = in.substring(0, idx).trim();
        String pStr = in.substring(idx + 1).trim();
        int port;
        try { port = Integer.parseInt(pStr); } catch (Exception e) { port = defPort; }
        return new HostPort(host.isEmpty() ? "localhost" : host, port);
    }
}
