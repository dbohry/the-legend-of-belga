package com.lhamacorp.games.tlob.client;

import com.lhamacorp.games.tlob.client.managers.BaseGameManager;
import com.lhamacorp.games.tlob.client.managers.SinglePlayerGameManager;
import com.lhamacorp.games.tlob.client.save.SaveManager;
import com.lhamacorp.games.tlob.client.save.SaveState;
import com.lhamacorp.games.tlob.client.save.ActivePerks;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class Game {

    private static final String APP_NAME = "The Legend of Belga";
    private static final String DEFAULT_VERSION = "dev";
    private static final int DIALOG_PADDING = 12;
    private static final int BUTTON_SPACING = 10;
    private static final int DEFAULT_PORT = 7777;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String version = getVersion();
            JFrame window = createMainWindow(version);
            BaseGameManager panel = createManagerWithStartFlow(window);
            
            window.setContentPane(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            panel.startGameThread();
        });
    }

    private static String getVersion() {
        String version = Game.class.getPackage() != null
            ? Game.class.getPackage().getImplementationVersion()
            : null;
        return (version == null || version.isBlank()) ? DEFAULT_VERSION : version;
    }

    private static JFrame createMainWindow(String version) {
        JFrame window = new JFrame(APP_NAME + " v" + version);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setResizable(false);
        return window;
    }

    private static BaseGameManager createManagerWithStartFlow(JFrame owner) {
        if (!isStartScreenEnabled()) {
            return new SinglePlayerGameManager();
        }

        // Build a simple modal dialog that mimics JOptionPane but lets us disable a button
        final JDialog dialog = new JDialog(owner, "Start Game", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(DIALOG_PADDING, DIALOG_PADDING));

        JLabel msg = new JLabel("Choose a game mode:");
        msg.setBorder(BorderFactory.createEmptyBorder(DIALOG_PADDING, DIALOG_PADDING, 0, DIALOG_PADDING));
        dialog.add(msg, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, BUTTON_SPACING, BUTTON_SPACING));
        JButton btnSingle = new JButton("Singleplayer");
        JButton btnMulti  = new JButton("Multiplayer");
        JButton btnCancel = new JButton("Cancel");

        // Disable Multiplayer
        btnMulti.setEnabled(false);

        buttons.add(btnSingle);
        buttons.add(btnMulti);
        buttons.add(btnCancel);
        dialog.add(buttons, BorderLayout.SOUTH);

        // Make Singleplayer the default button (Enter key)
        dialog.getRootPane().setDefaultButton(btnSingle);

        // Result holder: 0 = Singleplayer, 2 = Cancel/Close
        final int[] result = {-1};

        btnSingle.addActionListener(e -> { result[0] = 0; dialog.dispose(); });
        btnCancel.addActionListener(e -> { result[0] = 2; dialog.dispose(); });

        // If user closes the window (X), treat as cancel
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { result[0] = 2; }
        });

        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        int choice = result[0];
        if (choice == 2 || choice == -1) {
            System.exit(0);
        }

        // Singleplayer flow - show second menu
        return showSinglePlayerMenu(owner);

        // NOTE: Multiplayer path intentionally unreachable because the button is disabled.
        // If you re-enable it in the future, reinstate:
        // String hero = prompt(owner, "Hero name:", "Hero"); ...
        // HostPort hp = parseHostPort(hostPort, DEFAULT_PORT);
        // return new MultiplayerGameManager(hp.host, hp.port, hero);
    }

    /**
     * Shows the single player menu with options to resume, start new game, or cancel.
     * @param owner the parent window
     * @return the appropriate game manager based on user choice
     */
    private static BaseGameManager showSinglePlayerMenu(JFrame owner) {
        SaveManager saveManager = new SaveManager();
        boolean hasSave = saveManager.hasSave();
        
        // Build the single player menu dialog
        final JDialog dialog = new JDialog(owner, "Single Player", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(DIALOG_PADDING, DIALOG_PADDING));

        // Create message panel with save info if available
        JPanel msgPanel = new JPanel(new BorderLayout());
        JLabel msg = new JLabel("Choose an option:");
        msg.setBorder(BorderFactory.createEmptyBorder(DIALOG_PADDING, DIALOG_PADDING, 0, DIALOG_PADDING));
        msgPanel.add(msg, BorderLayout.NORTH);
        
        // Add save information if available
        if (hasSave) {
            SaveState existingSave = saveManager.loadGame();
            if (existingSave != null) {
                // Build simple save info
                String saveInfoText = "Save found:\n" +
                    "Seed: " + existingSave.getWorldSeed() + "\n" +
                    "Completed maps: " + existingSave.getCompletedMaps();
                
                JTextArea saveInfo = new JTextArea(saveInfoText);
                saveInfo.setEditable(false);
                saveInfo.setBackground(dialog.getBackground());
                saveInfo.setBorder(BorderFactory.createEmptyBorder(0, DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING));
                msgPanel.add(saveInfo, BorderLayout.CENTER);
            }
        }
        
        dialog.add(msgPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, BUTTON_SPACING, BUTTON_SPACING));
        
        JButton btnResume = new JButton("Resume");
        JButton btnNewGame = new JButton("New Game");
        JButton btnCancel = new JButton("Cancel");
        
        // Disable Resume button if no save exists
        btnResume.setEnabled(hasSave);
        
        buttons.add(btnResume);
        buttons.add(btnNewGame);
        buttons.add(btnCancel);
        dialog.add(buttons, BorderLayout.SOUTH);

        // Make New Game the default button (Enter key) if no save, otherwise Resume
        if (hasSave) {
            dialog.getRootPane().setDefaultButton(btnResume);
        } else {
            dialog.getRootPane().setDefaultButton(btnNewGame);
        }

        // Result holder: 0 = Resume, 1 = New Game, 2 = Cancel/Close
        final int[] result = {-1};

        btnResume.addActionListener(e -> { result[0] = 0; dialog.dispose(); });
        btnNewGame.addActionListener(e -> { result[0] = 1; dialog.dispose(); });
        btnCancel.addActionListener(e -> { result[0] = 2; dialog.dispose(); });

        // If user closes the window (X), treat as cancel
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { result[0] = 2; }
        });

        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        int choice = result[0];
        if (choice == 2 || choice == -1) {
            // User cancelled, go back to main menu
            return createManagerWithStartFlow(owner);
        }
        
        if (choice == 0) {
            // Resume existing game
            SaveState existingSave = saveManager.loadGame();
            if (existingSave != null) {
                return new SinglePlayerGameManager(existingSave);
            }
        }
        
        // New Game - delete old save if it exists
        if (hasSave) {
            saveManager.deleteSave();
        }
        
        // Prompt for world seed
        long defSeed = new Random().nextLong();
        String seedStr = prompt(owner, "World seed:", Long.toString(defSeed));
        if (seedStr == null) {
            // User cancelled seed input, go back to main menu
            return createManagerWithStartFlow(owner);
        }
        
        long seed;
        try {
            seed = Long.parseLong(seedStr.trim());
        } catch (Exception e) {
            seed = defSeed;
        }
        return new SinglePlayerGameManager(seed);
    }

    private static boolean isStartScreenEnabled() {
        String env = System.getenv("TLOB_START_SCREEN");
        return (env == null) || Boolean.parseBoolean(env);
    }

    private static String prompt(Component parent, String label, String def) {
        return (String) JOptionPane.showInputDialog(
            parent,
            label,
            APP_NAME,
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
