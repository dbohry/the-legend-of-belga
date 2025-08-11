package com.lhamacorp.games.tlob.managers;

import com.lhamacorp.games.tlob.entities.Enemy;
import com.lhamacorp.games.tlob.entities.Player;
import com.lhamacorp.games.tlob.managers.renderers.*;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.perks.PerkManager;
import com.lhamacorp.games.tlob.weapons.Sword;
import com.lhamacorp.games.tlob.weapons.Weapon;
import com.lhamacorp.games.tlob.world.InputState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager extends JPanel implements Runnable {

    public static final int TILE_SIZE = 32;
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    private static final Color BG_DARK = new Color(10, 10, 15);
    private static final float VOLUME_DB_MIN = -40.0f;
    private static final float VOLUME_DB_MAX = 0.0f;

    private enum GameState {START, PLAYING, PAUSED, VICTORY, GAME_OVER}

    private GameState state;

    private Thread gameThread;
    private volatile boolean running = false;

    // Input snapshot (30 Hz sim)
    private final InputState input = new InputState();

    // 30 Hz tile/ambient tick
    private int animTick30 = 0;

    // Core systems
    private final KeyManager keyManager;
    private final Camera camera = new Camera();
    private final PerkManager perkManager = new PerkManager();
    private final HudRenderer hudRenderer = new HudRenderer(new Font("Arial", Font.PLAIN, 16));
    private final PauseMenuRenderer pauseMenuRenderer =
        new PauseMenuRenderer(new Font("Arial", Font.BOLD, 48),
            new Font("Arial", Font.BOLD, 20),
            new Font("Arial", Font.PLAIN, 14),
            VOLUME_DB_MIN, VOLUME_DB_MAX);
    private final StartScreenRenderer startScreenRenderer =
        new StartScreenRenderer("The Legend of the Belga",
            new Font("Arial", Font.BOLD, 48),
            new Font("Arial", Font.PLAIN, 24));
    private final VictoryScreenRenderer victoryRenderer = new VictoryScreenRenderer();
    private final GameOverRenderer gameOverRenderer = new GameOverRenderer(new Font("Arial", Font.BOLD, 48));

    // Deterministic world RNG
    private final long worldSeed;
    private final Random rootRng;
    private final Random mapsRoot;
    private final Random spawnsRoot;

    // Systems that need RNG
    private final LevelManager levelManager;
    private final SpawnManager enemySpawner;

    // World
    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();

    // Audio
    private boolean musicMuted = false;
    private float musicVolumeDb = -12.0f;

    // Start screen toggle
    private final boolean startScreenEnabled;

    public GameManager() {
        this(!Boolean.getBoolean("tlob.skipStart"));
    }

    public GameManager(boolean enableStartScreen) {
        this.startScreenEnabled = enableStartScreen;

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        keyManager = new KeyManager();
        addKeyListener(keyManager);
        addMouseListener(keyManager);
        setFocusable(true);

        addMouseMotionListener(new UIMouseMotionHandler());
        addMouseListener(new UIMouseClickHandler());
        addKeyListener(new GlobalKeyHandler());
        if (startScreenEnabled) {
            addKeyListener(new StartScreenKeyHandler());
            state = GameState.START;
        } else {
            state = GameState.PLAYING;
        }

        // Seed & RNG (property takes precedence, then env; otherwise time)
        this.worldSeed = readSeed();
        this.rootRng   = new Random(worldSeed);
        this.mapsRoot  = new Random(rootRng.nextLong());   // independent stream for maps
        this.spawnsRoot= new Random(rootRng.nextLong());   // independent stream for spawns
        System.out.println("World seed = " + worldSeed);

        // RNG-aware systems (each gets its own substream)
        levelManager = new LevelManager(80, 60, 0.45, 2500, mapsRoot);
        enemySpawner = new SpawnManager(new Sword(2, 28, 12, 10, 16),
            new Random(spawnsRoot.nextLong()));

        // World init
        TileMap map = levelManager.map();
        int[] spawn = map.findSpawnTile();
        Weapon sword = new Sword(2, 28, 10, 10, 16);
        player = new Player(spawn[0] * TILE_SIZE + TILE_SIZE / 2.0,
            spawn[1] * TILE_SIZE + TILE_SIZE / 2.0,
            sword);

        if (!startScreenEnabled) {
            try {
                if (player.getName() == null || player.getName().isEmpty()) player.setName("Hero");
            } catch (Exception ignored) {
            }
        }

        enemySpawner.spawn(map, player, enemies, levelManager.completed(), TILE_SIZE);

        // Layout static UI
        pauseMenuRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT, 150, 40);
        gameOverRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);

        AudioManager.playRandomMusic(musicVolumeDb);
    }

    private static long readSeed() {
        String prop = System.getProperty("tlob.seed");
        String envU = System.getenv("TLOB_SEED");
        String envL = System.getenv("tlob.seed");
        String raw  = (prop != null && !prop.isBlank()) ? prop
            : (envU != null && !envU.isBlank()) ? envU
            : (envL != null && !envL.isBlank()) ? envL
            : null;
        try {
            return (raw != null) ? Long.parseLong(raw.trim()) : System.currentTimeMillis();
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    public void startGameThread() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        final double simHz = 30.0;
        final double simStepNs = 1_000_000_000.0 / simHz;

        long last = System.nanoTime();
        double acc = 0.0;

        while (running) {
            long now = System.nanoTime();
            acc += (now - last);
            last = now;

            while (acc >= simStepNs) {
                update();
                acc -= simStepNs;
            }

            repaint();
            Thread.yield();
        }
    }

    private void update() {
        // Mute toggle
        if (keyManager.mute != musicMuted) {
            musicMuted = keyManager.mute;
            if (musicMuted) AudioManager.stopMusic();
            else AudioManager.playRandomMusic(musicVolumeDb);
        }

        switch (state) {
            case START, GAME_OVER, VICTORY, PAUSED -> {
                return;
            }
            case PLAYING -> {
                // Input snapshot
                input.up = keyManager.up;
                input.down = keyManager.down;
                input.left = keyManager.left;
                input.right = keyManager.right;
                input.attack = keyManager.attack;
                input.shift = keyManager.shift;

                if (!player.isAlive()) {
                    enterGameOver();
                    return;
                }
                if (enemies.isEmpty()) {
                    enterVictory();
                    return;
                }

                // Simulation (30 Hz)
                player.update(input, levelManager.map(), enemies);
                for (int i = enemies.size() - 1; i >= 0; i--) {
                    Enemy e = enemies.get(i);
                    e.update(player, levelManager.map());
                    if (!e.isAlive()) enemies.remove(i);
                }

                // Camera
                int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
                int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
                camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);

                // 30 Hz ambient tick
                animTick30++;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setColor(BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        switch (state) {
            case START -> startScreenRenderer.draw(g2);
            case PLAYING -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
            }
            case PAUSED -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
                pauseMenuRenderer.draw(g2, musicVolumeDb);
            }
            case GAME_OVER -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
                gameOverRenderer.draw(g2);
            }
            case VICTORY -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
                victoryRenderer.draw(g2);
            }
        }
        g2.dispose();
    }

    private void drawWorld(Graphics2D g2) {
        TileMap map = levelManager.map();
        map.draw(g2, camera.offsetX(), camera.offsetY(), getWidth(), getHeight(), animTick30);
        for (Enemy e : enemies) e.draw(g2, camera.offsetX(), camera.offsetY());
        player.draw(g2, camera.offsetX(), camera.offsetY());
    }

    private void enterGameOver() {
        state = GameState.GAME_OVER;
        gameOverRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    private void enterVictory() {
        state = GameState.VICTORY;
        perkManager.rollChoicesFor(player);
        victoryRenderer.setChoices(perkManager.getChoices());
        victoryRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);
        AudioManager.playSound("map-complete.wav");
    }

    private void pauseGame() {
        state = GameState.PAUSED;
        pauseMenuRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT, 150, 40);
    }

    private void resumeGame() {
        state = GameState.PLAYING;
        requestFocusInWindow();
    }

    private void restartGame() {
        state = GameState.PLAYING;

        // reseed spawns for deterministic restart
        enemySpawner.reseed(new Random(spawnsRoot.nextLong()));

        levelManager.restart(player, enemySpawner, enemies, TILE_SIZE);
        animTick30 = 0;

        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
        requestFocusInWindow();
    }

    private void startNextLevel() {
        // reseed spawns for the next level
        enemySpawner.reseed(new Random(spawnsRoot.nextLong()));

        levelManager.nextLevel(player, enemySpawner, enemies, TILE_SIZE);
        state = GameState.PLAYING;
        animTick30 = 0;

        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
        requestFocusInWindow();
    }

    private void applyPerkAndContinue(int index) {
        var applied = perkManager.applyChoice(index, player);
        if (applied == null) return;
        startNextLevel();
    }

    private class GlobalKeyHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (state == GameState.PLAYING) pauseGame();
                else if (state == GameState.PAUSED) resumeGame();
            }
        }
    }

    private class StartScreenKeyHandler extends KeyAdapter {
        @Override
        public void keyTyped(KeyEvent e) {
            if (state != GameState.START) return;
            startScreenRenderer.keyTyped(e.getKeyChar());
            if (startScreenRenderer.isComplete()) {
                try {
                    player.setName(startScreenRenderer.getHeroName());
                } catch (Exception ignored) {
                }
                state = GameState.PLAYING;
            }
            repaint();
        }
    }

    private class UIMouseMotionHandler extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (state == GameState.PAUSED) {
                float maybeDb = pauseMenuRenderer.dbFromPoint(e.getPoint());
                if (!Float.isNaN(maybeDb)) {
                    musicVolumeDb = maybeDb;
                    if (!musicMuted) AudioManager.setMusicVolume(musicVolumeDb);
                }
            }
        }
    }

    private class UIMouseClickHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            switch (state) {
                case START -> requestFocusInWindow();
                case GAME_OVER -> {
                    if (gameOverRenderer.hitTryAgain(e.getPoint())) restartGame();
                }
                case PAUSED -> {
                    float maybeDb = pauseMenuRenderer.dbFromPoint(e.getPoint());
                    if (!Float.isNaN(maybeDb)) {
                        musicVolumeDb = maybeDb;
                        if (!musicMuted) AudioManager.setMusicVolume(musicVolumeDb);
                        return;
                    }
                    if (pauseMenuRenderer.hitResume(e.getPoint())) resumeGame();
                    else if (pauseMenuRenderer.hitRestart(e.getPoint())) restartGame();
                    else if (pauseMenuRenderer.hitExit(e.getPoint())) System.exit(0);
                }
                case VICTORY -> {
                    int idx = victoryRenderer.handleClick(e.getPoint());
                    if (idx >= 0) applyPerkAndContinue(idx);
                }
                case PLAYING -> { /* gameplay clicks handled by KeyManager */ }
            }
        }
    }
}
