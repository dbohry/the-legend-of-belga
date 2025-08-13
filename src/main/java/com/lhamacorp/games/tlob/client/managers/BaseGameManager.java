package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.managers.renderers.GameOverRenderer;
import com.lhamacorp.games.tlob.client.managers.renderers.HudRenderer;
import com.lhamacorp.games.tlob.client.managers.renderers.PauseMenuRenderer;
import com.lhamacorp.games.tlob.client.managers.renderers.StatsRenderer;
import com.lhamacorp.games.tlob.client.managers.renderers.VictoryScreenRenderer;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.perks.PerkManager;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import com.lhamacorp.games.tlob.client.world.InputState;

import javax.swing.*;
import java.awt.*;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class BaseGameManager extends JPanel implements Runnable {

    public static final int TILE_SIZE = 32;
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    private static final Color BG_DARK = new Color(10, 10, 15);
    private static final float VOLUME_DB_MIN = -40.0f;
    private static final float VOLUME_DB_MAX = 0.0f;
    private static final int CHECKSUM_EVERY_TICKS = Integer.getInteger("tlob.cs.every", 120);

    protected enum GameState {PLAYING, PAUSED, VICTORY, GAME_OVER}

    protected GameState state = GameState.PLAYING;

    private Thread gameThread;
    private volatile boolean running = false;

    protected final InputState input = new InputState();
    protected int animTick30 = 0;
    protected long simTick = 0;

    private int mouseScreenX = SCREEN_WIDTH / 2;
    private int mouseScreenY = SCREEN_HEIGHT / 2;
    private boolean mouseInside = false;

    protected final KeyManager keyManager = new KeyManager();
    protected final Camera camera = new Camera();
    protected final PerkManager perkManager = new PerkManager();
    protected final HudRenderer hudRenderer = new HudRenderer(new Font("Arial", Font.PLAIN, 16));
    protected final PauseMenuRenderer pauseMenuRenderer =
        new PauseMenuRenderer(new Font("Arial", Font.BOLD, 48),
            new Font("Arial", Font.BOLD, 20),
            new Font("Arial", Font.PLAIN, 14),
            VOLUME_DB_MIN, VOLUME_DB_MAX);
    protected final VictoryScreenRenderer victoryRenderer = new VictoryScreenRenderer();
    protected final GameOverRenderer gameOverRenderer = new GameOverRenderer(new Font("Arial", Font.BOLD, 48));
    protected final StatsRenderer statsRenderer = new StatsRenderer();

    protected long worldSeed = 0L;
    private Random rootRng;
    private Random mapsRoot;
    private Random spawnsRoot;

    protected LevelManager levelManager;
    protected SpawnManager enemySpawner;

    protected Player player;
    protected final List<Entity> enemies = new ArrayList<>();
    protected int enemiesAtLevelStart = 0;

    protected boolean musicMuted = false;
    protected float musicVolumeDb = -12.0f;
    protected boolean statsPageOpen = false;

    public BaseGameManager() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        addKeyListener(keyManager);
        addMouseListener(keyManager);
        setFocusable(true);

        UIMouseHandler mouseHandler = new UIMouseHandler();
        addMouseMotionListener(mouseHandler);
        addMouseListener(mouseHandler);

        addMouseListener(new UIMouseClickHandler());
        addKeyListener(new GlobalKeyHandler());
    }

    protected static long readSeed() {
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
        final double simHz = 60.0;
        final double stepNs = 1_000_000_000.0 / simHz;
        long last = System.nanoTime();
        double acc = 0.0;

        while (running) {
            long now = System.nanoTime();
            acc += (now - last);
            last = now;

            while (acc >= stepNs) {
                update();
                acc -= stepNs;
            }
            repaint();
            Thread.yield();
        }
    }

    private void update() {
        if (keyManager.mute != musicMuted) {
            musicMuted = keyManager.mute;
            if (musicMuted) AudioManager.stopMusic();
            else AudioManager.playRandomMusic(musicVolumeDb);
        }

        if (state == GameState.PLAYING) {
            if (player == null || levelManager == null) return;

            input.up = keyManager.up;
            input.down = keyManager.down;
            input.left = keyManager.left;
            input.right = keyManager.right;
            input.attack = keyManager.attack;
            input.shift = keyManager.shift;

            // Handle I key for stats page
            if (keyManager.i && !statsPageOpen) {
                statsPageOpen = true;
            }
            if (!keyManager.i && statsPageOpen) {
                statsPageOpen = false;
            }
            


            Point aimWorld = null;
            if (mouseInside) {
                aimWorld = new Point(mouseScreenX + camera.offsetX(),
                    mouseScreenY + camera.offsetY());
            }

            updatePlaying(aimWorld);

            int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
            int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
            camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
            
            // Apply screen shake from player attacks
            camera.setShakeOffset(player.getScreenShakeOffset());

            simTick++;
            if ((simTick & 1) == 0) animTick30++;

            logChecksumIfDue();
        }
    }

    protected abstract void updatePlaying(Point aimWorld);

    /** Builds map & player. Subclasses decide how to populate enemies (SP) or remote entities (MP). */
    protected void initWorld(long seed) {
        this.worldSeed = seed;
        this.rootRng = new Random(worldSeed);
        this.mapsRoot = new Random(rootRng.nextLong());
        this.spawnsRoot = new Random(rootRng.nextLong());

        levelManager = new LevelManager(80, 60, 0.45, 2500, mapsRoot);
        enemySpawner = new SpawnManager(new Sword(2, 28, 12, 10, 16), new Random(spawnsRoot.nextLong()));

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

        pauseMenuRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT, 150, 40);
        gameOverRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);

        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (!musicMuted) AudioManager.playRandomMusic(musicVolumeDb);
        requestFocusInWindow();
        repaint();
    }

    protected void enterGameOver() {
        state = GameState.GAME_OVER;
        gameOverRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);
        // Auto-save when game ends
        autoSave();
    }

    protected void enterVictory() {
        state = GameState.VICTORY;
        perkManager.rollChoicesFor(player);
        victoryRenderer.setChoices(perkManager.getChoices());
        victoryRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT);
        AudioManager.playSound("map-complete.wav");
    }

    protected void pauseGame() {
        state = GameState.PAUSED;
        pauseMenuRenderer.layout(SCREEN_WIDTH, SCREEN_HEIGHT, 150, 40);
    }

    protected void resumeGame() {
        state = GameState.PLAYING;
        requestFocusInWindow();
    }

    protected void restartGame() {
        state = GameState.PLAYING;
        enemySpawner.reseed(new Random(new Random(worldSeed).nextLong()));
        levelManager.restart(player, enemySpawner, enemies, TILE_SIZE);
        enemiesAtLevelStart = enemies.size();

        animTick30 = 0;
        simTick = 0;

        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
        requestFocusInWindow();
    }

    protected void startNextLevel() {
        enemySpawner.reseed(new Random(new Random(worldSeed).nextLong()));
        levelManager.nextLevel(player, enemySpawner, enemies, TILE_SIZE);
        state = GameState.PLAYING;
        enemiesAtLevelStart = enemies.size();

        animTick30 = 0;
        simTick = 0;

        int mapWpx = levelManager.map().getWidth() * TILE_SIZE;
        int mapHpx = levelManager.map().getHeight() * TILE_SIZE;
        camera.follow(player.getX(), player.getY(), mapWpx, mapHpx, SCREEN_WIDTH, SCREEN_HEIGHT);
        requestFocusInWindow();
        
        // Auto-save when advancing to next level
        autoSave();
    }

    protected void applyPerkAndContinue(int index) {
        var applied = perkManager.applyChoice(index, player);
        if (applied != null) {
            // Auto-save immediately after applying perk (before starting next level)
            // This ensures the enhanced stats are captured in the save
            autoSave();
            startNextLevel();
        }
    }

    /**
     * Auto-saves the game state. This is a no-op in the base class.
     * Subclasses can override to implement actual saving.
     */
    protected void autoSave() {
        // Default implementation does nothing
    }

    /**
     * Saves the game state. This is a no-op in the base class.
     * Subclasses can override to implement actual saving.
     */
    protected void saveGame() {
        // Default implementation does nothing
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setColor(BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        drawWorld(g2);

        if (player != null) {
            hudRenderer.draw(g2, player, 8, 8);
            
            // Draw save indicator if this is a SinglePlayerGameManager
            if (this instanceof SinglePlayerGameManager) {
                SinglePlayerGameManager spManager = (SinglePlayerGameManager) this;
                if (spManager.isSaveIndicatorVisible()) {
                    hudRenderer.drawSaveIndicator(g2, getWidth(), getHeight(), spManager.getSaveIndicatorAlpha());
                }
            }
        } else {
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setColor(new Color(230, 235, 240));
            g2.drawString("Connecting...", 10, 18);
        }

        drawLevelCounters(g2);
        drawOverlays(g2);

        g2.dispose();
    }

    protected void drawWorld(Graphics2D g2) {
        if (levelManager == null || player == null) return;
        TileMap map = levelManager.map();
        map.draw(g2, camera.offsetX(), camera.offsetY(), getWidth(), getHeight(), animTick30);

        drawRemotePlayers(g2, camera.offsetX(), camera.offsetY());
        drawRemoteEnemies(g2, camera.offsetX(), camera.offsetY());

        // Create a defensive copy to avoid ConcurrentModificationException
        List<Entity> enemiesCopy = new ArrayList<>(enemies);
        for (Entity e : enemiesCopy) e.draw(g2, camera.offsetX(), camera.offsetY());
        player.draw(g2, camera.offsetX(), camera.offsetY(), enemiesCopy);
    }

    /** SP no-op; MP overrides. */
    protected void drawRemotePlayers(Graphics2D g2, int camX, int camY) {
    }

    /** SP no-op; MP overrides. */
    protected void drawRemoteEnemies(Graphics2D g2, int camX, int camY) {
    }

    private void drawOverlays(Graphics2D g2) {
        drawSeedOverlay(g2);
        
        // Draw stats page if open
        if (statsPageOpen && player != null) {
            statsRenderer.draw(g2, player);
        }
        
        switch (state) {
            case PAUSED -> pauseMenuRenderer.draw(g2, musicVolumeDb);
            case GAME_OVER -> gameOverRenderer.draw(g2);
            case VICTORY -> victoryRenderer.draw(g2);
            default -> {
            }
        }
    }

    protected void drawLevelCounters(Graphics2D g2) {
        if (levelManager == null) return;

        final int pad = 10;
        int completed = levelManager.completed();
        int left = enemies.size();
        int total = Math.max(enemiesAtLevelStart, left);

        String line1 = "Maps: " + completed;
        String line2 = "Enemies: " + left + "/" + total;
        String[] extras = topRightExtraLines();

        Font oldF = g2.getFont();
        Color oldCol = g2.getColor();
        Composite oldCmp = g2.getComposite();

        Font f = new Font("Arial", Font.PLAIN, 12);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();

        int w = Math.max(fm.stringWidth(line1), fm.stringWidth(line2));
        for (String ex : extras) w = Math.max(w, fm.stringWidth(ex));
        w += 12;

        int lines = 2 + extras.length;
        int h = fm.getHeight() * lines + 8;

        int x = getWidth() - w - pad;
        int y = pad;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(Color.BLACK);
        g2.fillRoundRect(x, y, w, h, 10, 10);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(new Color(235, 240, 245));
        int ty = y + fm.getAscent() + 4;
        g2.drawString(line1, x + 6, ty);
        ty += fm.getHeight();
        g2.drawString(line2, x + 6, ty);
        for (String ex : extras) {
            ty += fm.getHeight();
            g2.drawString(ex, x + 6, ty);
        }

        g2.setFont(oldF);
        g2.setColor(oldCol);
        g2.setComposite(oldCmp);
    }

    protected String[] topRightExtraLines() {
        return new String[0];
    }

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
            Entity e = enemies.get(i);
            h = mix(h, i);
            h = mix(h, Double.doubleToLongBits(e.getX()));
            h = mix(h, Double.doubleToLongBits(e.getY()));
            h = mix(h, e.isAlive() ? 1 : 0);
        }
        TileMap m = levelManager.map();
        int mw = m.getWidth(), mh = m.getHeight();
        for (int y = 0; y < mh; y++) for (int x = 0; x < mw; x++) h = mix(h, m.isWall(x, y) ? 1 : 0);
        return h;
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 0x100000001b3L;
        h ^= (h >>> 32);
        return h;
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
                default -> {
                }
            }
        }
    }
}
