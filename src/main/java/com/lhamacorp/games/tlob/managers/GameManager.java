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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

    // Game modes
    public enum GameMode {SINGLEPLAYER, MULTIPLAYER}

    private GameMode gameMode = GameMode.SINGLEPLAYER;
    private String serverHost = "localhost";
    private int serverPort = 7777;

    // Net UI / tickrate
    private int serverTickrate = 60;
    private boolean showNetBanner = false;
    private int netBannerTicks = 0;      // counts down each tick (~60/s)
    private String netBannerText = "";

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

    // Deterministic world RNG (now LAZY / not-final so we can use server seed)
    private long worldSeed = 0L;
    private Random rootRng;
    private Random mapsRoot;
    private Random spawnsRoot;

    // Systems that need RNG (lazy)
    private LevelManager levelManager;
    private SpawnManager enemySpawner;

    // World
    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private int enemiesAtLevelStart = 0;

    // Audio
    private boolean musicMuted = false;
    private float musicVolumeDb = -12.0f;

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
            // No start screen -> single-player with local seed immediately
            state = GameState.PLAYING;
            initWorld(readSeed());
        }
    }

    // Build the whole world from a given seed.
    private void initWorld(long seed) {
        this.worldSeed = seed;
        this.rootRng = new Random(worldSeed);
        this.mapsRoot = new Random(rootRng.nextLong());
        this.spawnsRoot = new Random(rootRng.nextLong());
        System.out.println("World seed = " + worldSeed + (gameMode == GameMode.MULTIPLAYER ? " (from server)" : ""));

        // Clear old lists if re-init (in case of switching)
        enemies.clear();

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

        try {
            if (player.getName() == null || player.getName().isEmpty()) player.setName("Hero");
        } catch (Exception ignored) {
        }

        enemySpawner.spawn(map, player, enemies, levelManager.completed(), TILE_SIZE);
        enemiesAtLevelStart = enemies.size();

        // Layout static UI
        pauseMenuRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT, 150, 40);
        gameOverRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);

        // Camera to player
        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Music
        if (!musicMuted) AudioManager.playRandomMusic(musicVolumeDb);

        repaint();
    }

    private static long readSeed() {
        String prop = System.getProperty("tlob.seed");
        String envU = System.getenv("TLOB_SEED");
        String envL = System.getenv("tlob.seed");
        String raw = (prop != null && !prop.isBlank()) ? prop
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
        final double simHz = 60.0; // (server can advertise tickrate, but we keep 60 for now)
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
                // Guard: must be initialized
                if (player == null || levelManager == null) return;

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

                // Mouse aim only if the cursor is currently inside the panel
                Point aimWorld = null;
                if (mouseInside) {
                    aimWorld = new Point(mouseScreenX + camera.offsetX(),
                        mouseScreenY + camera.offsetY());
                }

                // Simulation (player/enemies)
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
                if ((simTick & 1) == 0) animTick30++; // keep 30Hz ambience at 60Hz sim

                // net banner countdown
                if (showNetBanner) {
                    if (--netBannerTicks <= 0) {
                        showNetBanner = false;
                        netBannerTicks = 0;
                    }
                }

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
                hudRenderer.draw(g2, player, 8, 8);
                drawLevelCounters(g2);
                drawNetBanner(g2);
                drawSeedOverlay(g2);
            }
            case PAUSED -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, 8, 8);
                drawLevelCounters(g2);
                drawNetBanner(g2);
                drawSeedOverlay(g2);
                pauseMenuRenderer.draw(g2, musicVolumeDb);
            }
            case GAME_OVER -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, 8, 8);
                drawSeedOverlay(g2);
                gameOverRenderer.draw(g2);
            }
            case VICTORY -> {
                drawWorld(g2);
                hudRenderer.draw(g2, player, 8, 8);
                drawLevelCounters(g2);
                drawNetBanner(g2);
                drawSeedOverlay(g2);
                victoryRenderer.draw(g2);
            }
        }
        g2.dispose();
    }

    /** Small top-right overlay: maps completed and enemy left/total (+ net tickrate in MP). */
    private void drawLevelCounters(Graphics2D g2) {
        if (levelManager == null) return;

        final int pad = 10;
        int completed = levelManager.completed();
        int left = enemies.size();
        int total = Math.max(enemiesAtLevelStart, left);

        String line1 = "Maps: " + completed;
        String line2 = "Enemies: " + left + "/" + total;
        String line3 = (gameMode == GameMode.MULTIPLAYER) ? ("Net: " + serverTickrate + " Hz") : null;

        Font oldF = g2.getFont();
        Color oldCol = g2.getColor();
        Composite oldCmp = g2.getComposite();

        Font f = new Font("Arial", Font.PLAIN, 12);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();

        int w = Math.max(fm.stringWidth(line1), Math.max(fm.stringWidth(line2), line3 == null ? 0 : fm.stringWidth(line3))) + 12;
        int lines = (line3 != null) ? 3 : 2;
        int h = fm.getHeight() * lines + 8;

        int x = getWidth() - w - pad;
        int y = pad;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(new Color(0, 0, 0));
        g2.fillRoundRect(x, y, w, h, 10, 10);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(new Color(235, 240, 245));
        int ty = y + fm.getAscent() + 4;
        g2.drawString(line1, x + 6, ty);
        ty += fm.getHeight();
        g2.drawString(line2, x + 6, ty);
        if (line3 != null) {
            ty += fm.getHeight();
            g2.drawString(line3, x + 6, ty);
        }

        g2.setFont(oldF);
        g2.setColor(oldCol);
        g2.setComposite(oldCmp);
    }

    /** Center-top transient banner after successful MP connect. */
    private void drawNetBanner(Graphics2D g2) {
        if (!showNetBanner || netBannerText == null || netBannerText.isBlank()) return;

        int fadeTicks = Math.min(netBannerTicks, 60); // fade last ~1s
        float alpha = Math.max(0f, Math.min(1f, fadeTicks / 60f));

        Font oldF = g2.getFont();
        Color oldC = g2.getColor();
        Composite oldCmp = g2.getComposite();

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(netBannerText) + 16;
        int h = fm.getHeight() + 10;

        int x = (getWidth() - w) / 2;
        int y = 12;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f * alpha));
        g2.setColor(Color.BLACK);
        g2.fillRoundRect(x, y, w, h, 10, 10);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f * alpha));
        g2.setColor(new Color(235, 240, 245));
        g2.drawString(netBannerText, x + 8, y + 6 + fm.getAscent());

        g2.setFont(oldF);
        g2.setColor(oldC);
        g2.setComposite(oldCmp);
    }

    private void drawWorld(Graphics2D g2) {
        if (levelManager == null || player == null) return;
        TileMap map = levelManager.map();
        map.draw(g2, camera.offsetX(), camera.offsetY(), getWidth(), getHeight(), animTick30);
        for (Enemy e : enemies) e.draw(g2, camera.offsetX(), camera.offsetY());
        player.draw(g2, camera.offsetX(), camera.offsetY());
    }

    /** tiny, low-contrast seed tag bottom-right */
    private void drawSeedOverlay(Graphics2D g2) {
        if (worldSeed == 0L) return;
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
        if (player == null || levelManager == null) return 0L;

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
        enemiesAtLevelStart = enemies.size();

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
        enemiesAtLevelStart = enemies.size();

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

    // --- Start screen → begin game (reads mode/name/server) ---
    private void beginFromStartScreen() {
        StartScreenRenderer.Result res = startScreenRenderer.getResult();
        String hero = (res.heroName == null || res.heroName.isBlank()) ? "Hero" : res.heroName;
        try {
            if (player != null) player.setName(hero);
        } catch (Exception ignored) {
        }

        if (res.mode == StartScreenRenderer.Mode.MULTIPLAYER) {
            gameMode = GameMode.MULTIPLAYER;
            serverHost = res.host;
            serverPort = res.port;

            // connect in background; keep UI responsive
            new Thread(() -> {
                try {
                    SeedHandshake sh = fetchSeedFromServer(serverHost, serverPort, hero);
                    final long s = sh.seed;
                    final int stk = sh.tickrate;
                    SwingUtilities.invokeLater(() -> {
                        serverTickrate = (stk > 0 ? stk : 60);
                        netBannerText = "Connected: " + serverHost + ":" + serverPort + " • " + serverTickrate + " Hz";
                        showNetBanner = true;
                        netBannerTicks = 180; // ~3s at 60Hz

                        initWorld(s);       // build world from server seed
                        state = GameState.PLAYING;
                        requestFocusInWindow();
                    });
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "Failed to connect to server " + serverHost + ":" + serverPort +
                                "\n" + ex.getMessage(),
                            "Connection error", JOptionPane.ERROR_MESSAGE);
                        // Stay on start screen
                        requestFocusInWindow();
                        repaint();
                    });
                }
            }, "SeedHandshake").start();

        } else {
            gameMode = GameMode.SINGLEPLAYER;
            showNetBanner = false;
            netBannerTicks = 0;
            netBannerText = "";
            initWorld(readSeed());
            state = GameState.PLAYING;
            requestFocusInWindow();
        }
    }

    // --- Simple seed handshake client (blocking, called off-EDT) ---
    private static class SeedHandshake {
        final long seed;
        final int tickrate;

        SeedHandshake(long s, int t) {
            this.seed = s;
            this.tickrate = t;
        }
    }

    private SeedHandshake fetchSeedFromServer(String host, int port, String playerName) throws IOException {
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress(host, port), 4000);
            sock.setSoTimeout(5000);

            try (var out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
                 var in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {

                long seed = 0L;
                int tick = 60;

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("HELLO")) {
                        // Introduce ourselves
                        sendLine(out, "LOGIN name=" + safe(playerName));
                    } else if (line.startsWith("SEED")) {
                        // SEED <long>
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) seed = parseLong(parts[1], 0L);
                    } else if (line.startsWith("TICKRATE")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) tick = (int) parseLong(parts[1], 60L);
                    } else if (line.equals("READY")) {
                        if (seed == 0L) throw new IOException("Server didn't provide a seed.");
                        return new SeedHandshake(seed, tick);
                    }
                }
                throw new IOException("Disconnected before READY.");
            }
        }
    }

    private static void sendLine(Writer out, String s) throws IOException {
        out.write(s);
        out.write("\n");
        out.flush();
    }

    private static String safe(String s) {
        return s.replaceAll("[\\r\\n\\t]", "_");
    }

    private static long parseLong(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // --- Input handlers ---
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
                beginFromStartScreen();
            }
            repaint();
        }
    }

    /** One handler for move/drag AND enter/exit so we know if the cursor is inside. */
    private class UIMouseHandler extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            mouseInside = true;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseInside = false;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (state == GameState.PLAYING) {
                mouseScreenX = e.getX();
                mouseScreenY = e.getY();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
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
        @Override
        public void mouseClicked(MouseEvent e) {
            switch (state) {
                case START -> {
                    startScreenRenderer.handleClick(e.getPoint());
                    if (startScreenRenderer.isComplete()) {
                        beginFromStartScreen();
                    } else {
                        requestFocusInWindow();
                        repaint();
                    }
                }
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
