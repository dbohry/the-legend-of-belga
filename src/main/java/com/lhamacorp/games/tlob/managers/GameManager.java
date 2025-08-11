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
    private static final int CHECKSUM_EVERY_TICKS = Integer.getInteger("tlob.cs.every", 120);

    private enum GameState {START, PLAYING, PAUSED, VICTORY, GAME_OVER}
    private GameState state;

    private Thread gameThread;
    private volatile boolean running = false;

    private final InputState input = new InputState();
    private int animTick30 = 0;
    private long simTick = 0;

    // latest cursor in SCREEN coords + inside flag
    private int mouseScreenX = SCREEN_WIDTH / 2;
    private int mouseScreenY = SCREEN_HEIGHT / 2;
    private boolean mouseInside = false;

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

    private final boolean startScreenEnabled;

    public GameManager() { this(!Boolean.getBoolean("tlob.skipStart")); }

    public GameManager(boolean enableStartScreen) {
        this.startScreenEnabled = enableStartScreen;

        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        keyManager = new KeyManager();
        addKeyListener(keyManager);
        addMouseListener(keyManager);
        setFocusable(true);

        // unified mouse handler (moved/dragged + entered/exited)
        UIMouseHandler mouseHandler = new UIMouseHandler();
        addMouseMotionListener(mouseHandler);
        addMouseListener(mouseHandler);

        addMouseListener(new UIMouseClickHandler());
        addKeyListener(new GlobalKeyHandler());
        if (startScreenEnabled) {
            addKeyListener(new StartScreenKeyHandler());
            state = GameState.START;
        } else {
            state = GameState.PLAYING;
        }

        // Seed & RNG
        this.worldSeed = readSeed();
        this.rootRng = new Random(worldSeed);
        this.mapsRoot = new Random(rootRng.nextLong());
        this.spawnsRoot = new Random(rootRng.nextLong());
        System.out.println("World seed = " + worldSeed);

        // RNG-aware systems
        levelManager = new LevelManager(80, 60, 0.45, 2500, mapsRoot);
        enemySpawner = new SpawnManager(new Sword(2, 28, 12, 10, 16), new Random(spawnsRoot.nextLong()));

        // World init
        TileMap map = levelManager.map();
        int[] spawn = map.findSpawnTile();
        Weapon sword = new Sword(2, 28, 10, 10, 16);
        player = new Player(
            spawn[0] * TILE_SIZE + TILE_SIZE / 2.0,
            spawn[1] * TILE_SIZE + TILE_SIZE / 2.0,
            sword
        );

        if (!startScreenEnabled) {
            try {
                if (player.getName() == null || player.getName().isEmpty()) player.setName("Hero");
            } catch (Exception ignored) {}
        }

        enemySpawner.spawn(map, player, enemies, levelManager.completed(), TILE_SIZE);

        pauseMenuRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT, 150, 40);
        gameOverRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);

        AudioManager.playRandomMusic(musicVolumeDb);
    }

    private static long readSeed() {
        String prop = System.getProperty("tlob.seed");
        String envU = System.getenv("TLOB_SEED");
        String envL = System.getenv("tlob.seed");
        String raw = (prop != null && !prop.isBlank()) ? prop
            : (envU != null && !envU.isBlank()) ? envU
            : (envL != null && !envL.isBlank()) ? envL
            : null;
        try { return (raw != null) ? Long.parseLong(raw.trim()) : System.currentTimeMillis(); }
        catch (NumberFormatException e) { return System.currentTimeMillis(); }
    }

    public void startGameThread() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        final double simHz = 60.0;
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
            case START, GAME_OVER, VICTORY, PAUSED -> { return; }
            case PLAYING -> {
                // Input snapshot
                input.up = keyManager.up;
                input.down = keyManager.down;
                input.left = keyManager.left;
                input.right = keyManager.right;
                input.attack = keyManager.attack;
                input.shift = keyManager.shift;

                if (!player.isAlive()) { enterGameOver(); return; }
                if (enemies.isEmpty()) { enterVictory(); return; }

                // Mouse aim only if the cursor is currently inside the panel
                Point aimWorld = null;
                if (mouseInside) {
                    aimWorld = new Point(mouseScreenX + camera.offsetX(),
                        mouseScreenY + camera.offsetY());
                }

                // Simulation (30 Hz)
                player.update(input, levelManager.map(), enemies, aimWorld);
                for (int i = enemies.size() - 1; i >= 0; i--) {
                    Enemy e = enemies.get(i);
                    e.update(player, levelManager.map());
                    if (!e.isAlive()) enemies.remove(i);
                }

                // Camera
                int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
                int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
                camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);

                // ticks
                simTick++;
                if ( (simTick & 1) == 0 ) animTick30++;
                logChecksumIfDue();
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
                drawSeedOverlay(g2);
            }
            case PAUSED -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
                drawSeedOverlay(g2);
                pauseMenuRenderer.draw(g2, musicVolumeDb);
            }
            case GAME_OVER -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
                drawSeedOverlay(g2);
                gameOverRenderer.draw(g2);
            }
            case VICTORY -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, enemies.size(), 8, 8);
                drawSeedOverlay(g2);
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

    private void drawSeedOverlay(Graphics2D g2) {
        String text = "seed: " + worldSeed;
        Font oldF = g2.getFont();
        Composite oldC = g2.getComposite();
        Color oldCol = g2.getColor();

        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int pad = 6;
        int w = fm.stringWidth(text);

        int x = getWidth() - w - pad;
        int y = getHeight() - pad;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 1, y + 1);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g2.setColor(new Color(230, 235, 240));
        g2.drawString(text, x, y);

        g2.setFont(oldF);
        g2.setComposite(oldC);
        g2.setColor(oldCol);
    }

    private void logChecksumIfDue() {
        if (CHECKSUM_EVERY_TICKS <= 0) return;
        if ((simTick % CHECKSUM_EVERY_TICKS) != 0) return;

        long cs = computeChecksum();
        System.out.printf("tick=%d checksum=%016x enemies=%d%n", simTick, cs, enemies.size());
    }

    private long computeChecksum() {
        long h = 0xcbf29ce484222325L;

        h = mix(h, Double.doubleToLongBits(player.getX()));
        h = mix(h, Double.doubleToLongBits(player.getY()));

        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            h = mix(h, i);
            h = mix(h, Double.doubleToLongBits(e.getX()));
            h = mix(h, Double.doubleToLongBits(e.getY()));
            h = mix(h, e.isAlive() ? 1 : 0);
        }

        TileMap m = levelManager.map();
        int mw = m.getWidth(), mh = m.getHeight();
        for (int y = 0; y < mh; y++) {
            for (int x = 0; x < mw; x++) {
                h = mix(h, m.isWall(x, y) ? 1 : 0);
            }
        }
        return h;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 0x100000001b3L;
        h ^= (h >>> 32);
        return h;
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
        enemySpawner.reseed(new Random(spawnsRoot.nextLong()));
        levelManager.restart(player, enemySpawner, enemies, TILE_SIZE);
        animTick30 = 0;
        simTick = 0;

        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
        requestFocusInWindow();
    }

    private void startNextLevel() {
        enemySpawner.reseed(new Random(spawnsRoot.nextLong()));
        levelManager.nextLevel(player, enemySpawner, enemies, TILE_SIZE);
        state = GameState.PLAYING;
        animTick30 = 0;
        simTick = 0;

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
        @Override public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (state == GameState.PLAYING) pauseGame();
                else if (state == GameState.PAUSED) resumeGame();
            }
        }
    }

    private class StartScreenKeyHandler extends KeyAdapter {
        @Override public void keyTyped(KeyEvent e) {
            if (state != GameState.START) return;
            startScreenRenderer.keyTyped(e.getKeyChar());
            if (startScreenRenderer.isComplete()) {
                try { player.setName(startScreenRenderer.getHeroName()); } catch (Exception ignored) {}
                state = GameState.PLAYING;
            }
            repaint();
        }
    }

    /** One handler for move/drag AND enter/exit so we know if the cursor is inside. */
    private class UIMouseHandler extends MouseAdapter {
        @Override public void mouseEntered(MouseEvent e) { mouseInside = true; }
        @Override public void mouseExited(MouseEvent e)  { mouseInside = false; }
        @Override public void mouseMoved(MouseEvent e) {
            if (state == GameState.PLAYING) {
                mouseScreenX = e.getX();
                mouseScreenY = e.getY();
            }
        }
        @Override public void mouseDragged(MouseEvent e) {
            if (state == GameState.PAUSED) {
                float maybeDb = pauseMenuRenderer.dbFromPoint(e.getPoint());
                if (!Float.isNaN(maybeDb)) {
                    musicVolumeDb = maybeDb;
                    if (!musicMuted) AudioManager.setMusicVolume(musicVolumeDb);
                }
            } else if (state == GameState.PLAYING) {
                mouseScreenX = e.getX();
                mouseScreenY = e.getY();
            }
        }
    }

    private class UIMouseClickHandler extends MouseAdapter {
        @Override public void mouseClicked(MouseEvent e) {
            switch (state) {
                case START -> requestFocusInWindow();
                case GAME_OVER -> { if (gameOverRenderer.hitTryAgain(e.getPoint())) restartGame(); }
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
