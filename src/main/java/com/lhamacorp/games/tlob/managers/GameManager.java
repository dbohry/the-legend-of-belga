package com.lhamacorp.games.tlob.managers;

import com.lhamacorp.games.tlob.entities.Enemy;
import com.lhamacorp.games.tlob.entities.Player;
import com.lhamacorp.games.tlob.maps.MapGenerator;
import com.lhamacorp.games.tlob.maps.TileMap;
import com.lhamacorp.games.tlob.perks.Perk;
import com.lhamacorp.games.tlob.perks.PerkManager;
import com.lhamacorp.games.tlob.weapons.Sword;
import com.lhamacorp.games.tlob.weapons.Weapon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameManager extends JPanel implements Runnable {
    public static final int TILE_SIZE = 32;
    public static final int SCREEN_WIDTH = 640;
    public static final int SCREEN_HEIGHT = 480;

    private Thread gameThread;
    private volatile boolean running = false;

    private final KeyManager keyManager;
    private TileMap tileMap;
    private final Player player;
    private final List<Enemy> enemies;

    private int cameraOffsetX;
    private int cameraOffsetY;

    private boolean gameOver = false;
    private boolean victory = false;
    private boolean paused = false;
    private boolean musicMuted = false;
    private static final float MUSIC_VOL_DB = -12.0f;
    private Rectangle tryAgainButton;
    private Rectangle nextLevelButton;
    private Rectangle resumeButton;
    private Rectangle restartButton;
    private Rectangle exitButton;
    private int completedMaps = 0;

    // Perk selection
    private boolean choosingPerk = false;
    private final Rectangle[] perkRects = new Rectangle[3];
    private final PerkManager perkManager = new PerkManager();

    private boolean victorySoundPlayed = false;

    public GameManager() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        keyManager = new KeyManager();
        addKeyListener(keyManager);
        addMouseListener(keyManager);
        setFocusable(true);

        AudioManager.playRandomMusic(MUSIC_VOL_DB);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Perk selection takes priority on victory screen
                if (victory && choosingPerk) {
                    Point p = e.getPoint();
                    for (int i = 0; i < perkRects.length; i++) {
                        Rectangle r = perkRects[i];
                        if (r != null && r.contains(p)) {
                            applyPerkAndContinue(i);
                            return;
                        }
                    }
                    return;
                }

                if (gameOver && tryAgainButton != null && tryAgainButton.contains(e.getPoint())) {
                    restartGame();
                } else if (paused) {
                    if (resumeButton != null && resumeButton.contains(e.getPoint())) {
                        resumeGame();
                    } else if (restartButton != null && restartButton.contains(e.getPoint())) {
                        restartGame();
                    } else if (exitButton != null && exitButton.contains(e.getPoint())) {
                        System.exit(0);
                    }
                }
            }
        });

        MapGenerator generator = new MapGenerator(80, 60, 0.45, 2500);
        this.tileMap = new TileMap(generator.generate());

        int[] spawn = tileMap.findSpawnTile();
        Weapon sword = new Sword(2, 28, 12, 10, 16);
        player = new Player(spawn[0] * TILE_SIZE + TILE_SIZE / 2.0, spawn[1] * TILE_SIZE + TILE_SIZE / 2.0, sword);

        enemies = new ArrayList<>();
        spawnEnemies();
    }

    public void startGameThread() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        final double targetFps = 60.0;
        final double optimalTimeNs = 1_000_000_000.0 / targetFps;

        long lastTime = System.nanoTime();
        double delta = 0.0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / optimalTimeNs;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta -= 1;
            }

            repaint();

            long sleepNs = (long) (optimalTimeNs - (System.nanoTime() - now));
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            } else {
                Thread.yield();
            }
        }
    }

    private void update() {
        if (keyManager.mute != musicMuted) {
            musicMuted = keyManager.mute;
            if (musicMuted) {
                AudioManager.stopMusic();
            } else {
                AudioManager.playRandomMusic(MUSIC_VOL_DB);
            }
        }

        // Pause toggle
        if (keyManager.consumeEscapePressed() && !gameOver && !victory) {
            if (!paused) pauseGame();
            else resumeGame();
            return;
        }

        // Enter key on end screens:
        // - If gameOver -> restart
        // - If victory and NOT choosingPerk -> next level
        // - If victory and choosingPerk -> ignore (must pick a perk)
        if ((gameOver || victory) && keyManager.consumeEnterPressed()) {
            if (gameOver) restartGame();
            else if (victory && !choosingPerk) startNextLevel();
            return;
        }

        // Game over detection
        if (!player.isAlive() && !gameOver) {
            gameOver = true;
            int buttonWidth = 150;
            int buttonHeight = 40;
            int buttonX = (SCREEN_WIDTH - buttonWidth) / 2;
            int buttonY = SCREEN_HEIGHT / 2 + 50;
            tryAgainButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        }

        // Victory detection -> go into perk selection FIRST
        if (enemies.isEmpty() && !victory && !gameOver) {
            victory = true;
            choosingPerk = true;
            perkManager.rollChoices();
            layoutPerkRects();
        }

        if (!gameOver && !victory && !paused) {
            player.update(keyManager, tileMap, enemies);
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                e.update(player, tileMap);
                if (!e.isAlive()) enemies.remove(i);
            }
            updateCamera();
        }
    }

    private void layoutPerkRects() {
        int cardW = 180, cardH = 100;
        int gap = 20;
        int totalW = cardW * 3 + gap * 2;
        int startX = (SCREEN_WIDTH - totalW) / 2;
        int y = SCREEN_HEIGHT / 2 - cardH / 2 + 30;

        for (int i = 0; i < 3; i++) {
            int x = startX + i * (cardW + gap);
            perkRects[i] = new Rectangle(x, y, cardW, cardH);
        }
    }

    private void applyPerkAndContinue(int index) {
        var applied = perkManager.applyChoice(index, player);
        if (applied == null) return;

        // Clear perk UI state
        choosingPerk = false;
        perkManager.clearChoices();
        Arrays.fill(perkRects, null);

        // Now show the Next Level button (or auto-advance)
        int buttonWidth = 150;
        int buttonHeight = 40;
        int buttonX = (SCREEN_WIDTH - buttonWidth) / 2;
        int buttonY = SCREEN_HEIGHT / 2 + 50;
        nextLevelButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        startNextLevel();
    }

    private void updateCamera() {
        int mapPixelWidth = tileMap.getWidth() * TILE_SIZE;
        int mapPixelHeight = tileMap.getHeight() * TILE_SIZE;

        int targetX = (int) Math.round(player.getX() - SCREEN_WIDTH / 2.0);
        int targetY = (int) Math.round(player.getY() - SCREEN_HEIGHT / 2.0);

        cameraOffsetX = clamp(targetX, 0, Math.max(0, mapPixelWidth - SCREEN_WIDTH));
        cameraOffsetY = clamp(targetY, 0, Math.max(0, mapPixelHeight - SCREEN_HEIGHT));
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Clear background
        g2.setColor(new Color(10, 10, 15));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw map
        tileMap.draw(g2, cameraOffsetX, cameraOffsetY, getWidth(), getHeight());

        // Draw entities
        for (Enemy e : enemies) e.draw(g2, cameraOffsetX, cameraOffsetY);
        player.draw(g2, cameraOffsetX, cameraOffsetY);

        // HUD
        drawHud(g2);

        // Overlays
        if (gameOver) drawGameOverScreen(g2);
        if (victory) drawVictoryScreen(g2);
        if (paused) drawPauseMenu(g2);

        g2.dispose();
    }

    private void drawHud(Graphics2D g2) {
        int pad = 8;
        int blockWidth = 16;
        int blockHeight = 12;
        int spacing = 2;
        int x = pad;
        int y = pad;

        // === HP + Shield (blue adds on top of health) ===
        double hp = player.getHealth();
        double sh = player.getShield();
        double maxHp = player.getMaxHealth();
        double maxSh = player.getMaxShield();

        int fullHp = (int) hp;
        boolean hasHalfHp = hp - fullHp >= 0.5;

        int fullSh = (int) sh;
        boolean hasHalfSh = sh - fullSh >= 0.5;

        int totalSlots = (int) maxHp + (int) maxSh;

        for (int i = 0; i < totalSlots; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.setColor(new Color(50, 10, 10));
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }

        for (int i = 0; i < fullHp && i < totalSlots; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.setColor(new Color(200, 40, 40));
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }
        if (hasHalfHp) {
            int i = Math.min(fullHp, totalSlots - 1);
            int blockX = x + i * (blockWidth + spacing);
            g2.setColor(new Color(200, 40, 40));
            g2.fillRect(blockX, y, blockWidth / 2, blockHeight);
        }

        int shieldStart = Math.min((int) Math.ceil(hp), totalSlots);

        for (int i = 0; i < fullSh; i++) {
            int idx = shieldStart + i;
            if (idx >= totalSlots) break;
            int blockX = x + idx * (blockWidth + spacing);
            g2.setColor(new Color(100, 150, 255));
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }

        if (hasHalfSh) {
            int idx = shieldStart + fullSh;
            if (idx < totalSlots) {
                int blockX = x + idx * (blockWidth + spacing);
                g2.setColor(new Color(100, 150, 255));
                g2.fillRect(blockX, y, blockWidth / 2, blockHeight);
            }
        }

        // Prepare next row position under HP/Shield
        int nextRowY = y + blockHeight + 6;

        // === Mana (only if maxMana > 0) ===
        double maxMana = player.getMaxMana();
        int staminaY;
        if (maxMana > 0) {
            double mana = player.getMana();
            int fullMana = (int) mana;
            boolean hasHalfMana = mana - fullMana >= 0.5;

            int y2 = nextRowY;

            for (int i = 0; i < (int) maxMana; i++) {
                int blockX = x + i * (blockWidth + spacing);
                g2.setColor(new Color(10, 10, 40));
                g2.fillRect(blockX, y2, blockWidth, blockHeight);

                if (i < fullMana) {
                    g2.setColor(new Color(100, 150, 255));
                    g2.fillRect(blockX, y2, blockWidth, blockHeight);
                } else if (i == fullMana && hasHalfMana) {
                    g2.setColor(new Color(100, 150, 255));
                    g2.fillRect(blockX, y2, blockWidth / 2, blockHeight);
                }
            }

            staminaY = y2 + blockHeight + 6;
        } else {
            staminaY = nextRowY;
        }

        // === Stamina ===
        double stamina = player.getStamina();
        double maxStamina = player.getMaxStamina();
        int fullStamina = (int) stamina;
        boolean hasHalfStamina = stamina - fullStamina >= 0.5;

        int y3 = staminaY;

        for (int i = 0; i < (int) maxStamina; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.setColor(new Color(60, 60, 20));
            g2.fillRect(blockX, y3, blockWidth, blockHeight);

            if (i < fullStamina) {
                g2.setColor(new Color(255, 255, 128));
                g2.fillRect(blockX, y3, blockWidth, blockHeight);
            } else if (i == fullStamina && hasHalfStamina) {
                g2.setColor(new Color(255, 255, 128));
                g2.fillRect(blockX, y3, blockWidth / 2, blockHeight);
            }
        }

        // === Enemies Left ===
        g2.setColor(Color.WHITE);
        g2.drawString("Enemies: " + enemies.size(), x, y3 + blockHeight + 16);
    }

    private void drawGameOverScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(Color.RED);
        String gameOverText = "GAME OVER";
        int textWidth = g2.getFontMetrics().stringWidth(gameOverText);
        int textX = (SCREEN_WIDTH - textWidth) / 2;
        int textY = SCREEN_HEIGHT / 2 - 20;
        g2.drawString(gameOverText, textX, textY);

        if (tryAgainButton != null) {
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(tryAgainButton.x, tryAgainButton.y, tryAgainButton.width, tryAgainButton.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(tryAgainButton.x, tryAgainButton.y, tryAgainButton.width, tryAgainButton.height);

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String buttonText = "Try Again";
            int buttonTextWidth = g2.getFontMetrics().stringWidth(buttonText);
            int buttonTextX = tryAgainButton.x + (tryAgainButton.width - buttonTextWidth) / 2;
            int buttonTextY = tryAgainButton.y + (tryAgainButton.height + 15) / 2;
            g2.drawString(buttonText, buttonTextX, buttonTextY);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);
    }

    private void drawVictoryScreen(Graphics2D g2) {
        if (!victorySoundPlayed) {
            AudioManager.playSound("map-complete.wav");
            victorySoundPlayed = true;
        }

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(Color.GREEN);
        String victoryText = "VICTORY!";
        int textWidth = g2.getFontMetrics().stringWidth(victoryText);
        int textX = (SCREEN_WIDTH - textWidth) / 2;
        int textY = SCREEN_HEIGHT / 2 - 40;
        g2.drawString(victoryText, textX, textY);

        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        g2.setColor(Color.WHITE);
        String levelText = "Level " + (completedMaps + 1) + " Complete!";
        int levelTextWidth = g2.getFontMetrics().stringWidth(levelText);
        int levelTextX = (SCREEN_WIDTH - levelTextWidth) / 2;
        int levelTextY = SCREEN_HEIGHT / 2;
        g2.drawString(levelText, levelTextX, levelTextY);

        // Perk selection UI
        if (choosingPerk) {
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            String choose = "Choose ONE perk";
            int w = g2.getFontMetrics().stringWidth(choose);
            g2.drawString(choose, (SCREEN_WIDTH - w) / 2, levelTextY + 30);

            var choices = perkManager.getChoices();
            for (int i = 0; i < choices.size(); i++) {
                Rectangle r = perkRects[i];
                Perk po = choices.get(i);

                g2.setColor(new Color(30, 30, 30, 230));
                g2.fillRect(r.x, r.y, r.width, r.height);
                g2.setColor(Color.WHITE);
                g2.drawRect(r.x, r.y, r.width, r.height);

                g2.setFont(new Font("Arial", Font.BOLD, 16));
                g2.drawString(po.name, r.x + 10, r.y + 24);

                g2.setFont(new Font("Arial", Font.PLAIN, 14));
                drawWrapped(g2, po.description, r.x + 10, r.y + 46, r.width - 20, 18);
            }
        } else if (nextLevelButton != null) {
            // Next Level button (only after a perk has been chosen)
            g2.setColor(new Color(60, 120, 60));
            g2.fillRect(nextLevelButton.x, nextLevelButton.y, nextLevelButton.width, nextLevelButton.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(nextLevelButton.x, nextLevelButton.y, nextLevelButton.width, nextLevelButton.height);

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String buttonText = "Next Level";
            int buttonTextWidth = g2.getFontMetrics().stringWidth(buttonText);
            int buttonTextX = nextLevelButton.x + (nextLevelButton.width - buttonTextWidth) / 2;
            int buttonTextY = nextLevelButton.y + (nextLevelButton.height + 15) / 2;
            g2.drawString(buttonText, buttonTextX, buttonTextY);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        String line = "";
        int yy = y;
        for (String w : words) {
            String test = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(test) > maxWidth) {
                g2.drawString(line, x, yy);
                yy += lineHeight;
                line = w;
            } else {
                line = test;
            }
        }
        if (!line.isEmpty()) g2.drawString(line, x, yy);
    }

    private void restartGame() {
        gameOver = false;
        paused = false;
        victory = false;
        choosingPerk = false;
        victorySoundPlayed = false;

        // clear end-screen UI
        tryAgainButton = null;
        nextLevelButton = null;
        perkManager.clearChoices();
        Arrays.fill(perkRects, null);

        // Reset player to spawn & heal
        int[] spawn = tileMap.findSpawnTile();
        player.setPosition(spawn[0] * TILE_SIZE + TILE_SIZE / 2.0, spawn[1] * TILE_SIZE + TILE_SIZE / 2.0);
        player.heal();

        // Reset completed maps counter
        completedMaps = 0;

        // Spawn enemies
        spawnEnemies();

        // Reset camera
        updateCamera();

        requestFocusInWindow();
    }

    private void startNextLevel() {
        completedMaps++;
        victory = false;
        paused = false;
        choosingPerk = false;
        victorySoundPlayed = false;

        // clear victory UI
        nextLevelButton = null;
        perkManager.clearChoices();
        Arrays.fill(perkRects, null);

        // Generate new map
        MapGenerator generator = new MapGenerator(80, 60, 0.45, 2500);
        tileMap = new TileMap(generator.generate());

        // Reset player to new spawn and fill to new maxes
        int[] spawn = tileMap.findSpawnTile();
        player.setPosition(spawn[0] * TILE_SIZE + TILE_SIZE / 2.0, spawn[1] * TILE_SIZE + TILE_SIZE / 2.0);
        player.restoreAll();

        // Spawn new enemies
        spawnEnemies();

        // Reset camera
        updateCamera();

        requestFocusInWindow();
    }

    private void spawnEnemies() {
        enemies.clear();
        // Count scales by completed maps
        int baseEnemies = 3 + (int) (Math.random() * 6);
        double multiplier = Math.pow(1.4, completedMaps);
        int enemiesToSpawn = Math.max(1, (int) (baseEnemies * multiplier));
        Weapon enemySword = new Sword(2, 28, 12, 10, 16);

        for (int i = 0; i < enemiesToSpawn; i++) {
            int[] pos = tileMap.randomFloorTileFarFrom(player.getX(), player.getY(), 12 * TILE_SIZE);
            if (pos != null) {
                if (!tileMap.isWall(pos[0], pos[1])) {
                    enemies.add(new Enemy(pos[0] * TILE_SIZE + TILE_SIZE / 2.0, pos[1] * TILE_SIZE + TILE_SIZE / 2.0, enemySword));
                }
            } else {
                int[] fallbackPos = tileMap.getRandomFloorTile();
                if (fallbackPos != null && !tileMap.isWall(fallbackPos[0], fallbackPos[1])) {
                    enemies.add(new Enemy(fallbackPos[0] * TILE_SIZE + TILE_SIZE / 2.0, fallbackPos[1] * TILE_SIZE + TILE_SIZE / 2.0, enemySword));
                }
            }
        }
    }

    private void drawPauseMenu(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(Color.WHITE);
        String pauseText = "PAUSED";
        int textWidth = g2.getFontMetrics().stringWidth(pauseText);
        int textX = (SCREEN_WIDTH - textWidth) / 2;
        int textY = SCREEN_HEIGHT / 2 - 120;
        g2.drawString(pauseText, textX, textY);

        if (resumeButton != null) {
            g2.setColor(new Color(60, 120, 60));
            g2.fillRect(resumeButton.x, resumeButton.y, resumeButton.width, resumeButton.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(resumeButton.x, resumeButton.y, resumeButton.width, resumeButton.height);

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String buttonText = "Resume";
            int buttonTextWidth = g2.getFontMetrics().stringWidth(buttonText);
            int buttonTextX = resumeButton.x + (resumeButton.width - buttonTextWidth) / 2;
            int buttonTextY = resumeButton.y + (resumeButton.height + 15) / 2;
            g2.drawString(buttonText, buttonTextX, buttonTextY);
        }

        if (restartButton != null) {
            g2.setColor(new Color(120, 120, 60));
            g2.fillRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height);

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String buttonText = "Restart";
            int buttonTextWidth = g2.getFontMetrics().stringWidth(buttonText);
            int buttonTextX = restartButton.x + (restartButton.width - buttonTextWidth) / 2;
            int buttonTextY = restartButton.y + (restartButton.height + 15) / 2;
            g2.drawString(buttonText, buttonTextX, buttonTextY);
        }

        if (exitButton != null) {
            g2.setColor(new Color(120, 60, 60));
            g2.fillRect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String buttonText = "Exit";
            int buttonTextWidth = g2.getFontMetrics().stringWidth(buttonText);
            int buttonTextX = exitButton.x + (exitButton.width - buttonTextWidth) / 2;
            int buttonTextY = exitButton.y + (exitButton.height + 15) / 2;
            g2.drawString(buttonText, buttonTextX, buttonTextY);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);
        String instructionText = "Press ESC to resume";
        int instructionWidth = g2.getFontMetrics().stringWidth(instructionText);
        int instructionX = (SCREEN_WIDTH - instructionWidth) / 2;
        int instructionY = SCREEN_HEIGHT / 2 + 120;
        g2.drawString(instructionText, instructionX, instructionY);
    }

    private void pauseGame() {
        paused = true;

        int buttonWidth = 150;
        int buttonHeight = 40;
        int buttonX = (SCREEN_WIDTH - buttonWidth) / 2;
        int buttonY = SCREEN_HEIGHT / 2;

        resumeButton = new Rectangle(buttonX, buttonY - 60, buttonWidth, buttonHeight);
        restartButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        exitButton = new Rectangle(buttonX, buttonY + 60, buttonWidth, buttonHeight);
    }

    private void resumeGame() {
        paused = false;
        resumeButton = null;
        restartButton = null;
        exitButton = null;
        requestFocusInWindow();
    }

}
