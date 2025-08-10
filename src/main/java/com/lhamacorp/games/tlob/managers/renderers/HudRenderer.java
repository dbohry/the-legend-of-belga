package com.lhamacorp.games.tlob.managers.renderers;

import com.lhamacorp.games.tlob.entities.Player;

import java.awt.*;

public final class HudRenderer {

    private final Font bodyFont;

    public HudRenderer(Font bodyFont) {
        this.bodyFont = bodyFont;
    }

    public void draw(Graphics2D g2, Player player, int enemiesLeft, int x, int y) {
        g2.setFont(bodyFont);
        int nextY = drawHpShield(g2, player, x, y);
        nextY = drawMana(g2, player, x, nextY);
        nextY = drawStamina(g2, player, x, nextY);
        g2.setColor(Color.WHITE);
        g2.drawString("Enemies: " + enemiesLeft, x, nextY + 16);
    }

    private int drawHpShield(Graphics2D g2, Player p, int x, int y) {
        return drawBlocks(
            g2, x, y,
            16, 12, 2,
            clamp01(p.getHealth(), 0, p.getMaxHealth()), p.getMaxHealth(),
            new Color(50, 10, 10),       // background across total slots
            new Color(200, 40, 40),      // HP color
            clamp01(p.getShield(), 0, p.getMaxShield()), p.getMaxShield(),
            new Color(100, 150, 255)     // Shield color
        );
    }

    private int drawMana(Graphics2D g2, Player p, int x, int y) {
        if (p.getMaxMana() <= 0) return y;
        return drawSingleRow(
            g2, x, y,
            16, 12, 2,
            clamp01(p.getMana(), 0, p.getMaxMana()), p.getMaxMana(),
            new Color(10, 10, 40),
            new Color(100, 150, 255)
        );
    }

    private int drawStamina(Graphics2D g2, Player p, int x, int y) {
        return drawSingleRow(
            g2, x, y,
            16, 12, 2,
            clamp01(p.getStamina(), 0, p.getMaxStamina()), p.getMaxStamina(),
            new Color(60, 60, 20),
            new Color(255, 255, 128)
        );
    }

    /**
     * Draws a combined HP + Shield row:
     * - Draws 'totalSlots' background blocks where totalSlots = maxHp + maxShield (truncated to int).
     * - Fills HP from slot 0 with full blocks and an optional half block at 0.5.
     * - Starts Shield at index ceil(hp) with full blocks and optional half block at 0.5.
     * Returns the next y position (current y + blockHeight + 6).
     */
    private int drawBlocks(
        Graphics2D g2, int x, int y,
        int blockWidth, int blockHeight, int spacing,
        double hp, double maxHp, Color bgColor, Color hpColor,
        double shield, double maxShield, Color shieldColor
    ) {
        // Guard/clamp
        maxHp = Math.max(0, maxHp);
        maxShield = Math.max(0, maxShield);
        hp = Math.max(0, Math.min(maxHp, hp));
        shield = Math.max(0, Math.min(maxShield, shield));

        int totalSlots = (int) maxHp + (int) maxShield;
        if (totalSlots <= 0) return y;

        // Background across all slots
        for (int i = 0; i < totalSlots; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.setColor(bgColor);
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }

        // HP fill
        int fullHp = (int) hp;
        boolean halfHp = (hp - fullHp) >= 0.5;

        g2.setColor(hpColor);
        for (int i = 0; i < fullHp && i < totalSlots; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }
        if (halfHp) {
            int i = Math.min(fullHp, totalSlots - 1);
            int blockX = x + i * (blockWidth + spacing);
            g2.fillRect(blockX, y, blockWidth / 2, blockHeight);
        }

        // Shield fill starts after ceil(hp)
        int shieldStart = Math.min((int) Math.ceil(hp), totalSlots);
        int fullShield = (int) shield;
        boolean halfShield = (shield - fullShield) >= 0.5;

        g2.setColor(shieldColor);
        for (int i = 0; i < fullShield; i++) {
            int idx = shieldStart + i;
            if (idx >= totalSlots) break;
            int blockX = x + idx * (blockWidth + spacing);
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }
        if (halfShield) {
            int idx = shieldStart + fullShield;
            if (idx < totalSlots) {
                int blockX = x + idx * (blockWidth + spacing);
                g2.fillRect(blockX, y, blockWidth / 2, blockHeight);
            }
        }

        return y + blockHeight + 6;
    }

    /**
     * Draws a single row (e.g., Mana or Stamina) with background blocks for max,
     * then fills full blocks plus an optional half block at 0.5.
     * Returns the next y position (current y + blockHeight + 6).
     */
    private int drawSingleRow(
        Graphics2D g2, int x, int y,
        int blockWidth, int blockHeight, int spacing,
        double value, double maxValue, Color bgColor, Color fillColor
    ) {
        maxValue = Math.max(0, maxValue);
        value = Math.max(0, Math.min(maxValue, value));

        int maxSlots = (int) maxValue;
        if (maxSlots <= 0) return y;

        // Background slots
        for (int i = 0; i < maxSlots; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.setColor(bgColor);
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }

        // Fill
        int full = (int) value;
        boolean half = (value - full) >= 0.5;

        g2.setColor(fillColor);
        for (int i = 0; i < full && i < maxSlots; i++) {
            int blockX = x + i * (blockWidth + spacing);
            g2.fillRect(blockX, y, blockWidth, blockHeight);
        }
        if (half && full < maxSlots) {
            int blockX = x + full * (blockWidth + spacing);
            g2.fillRect(blockX, y, blockWidth / 2, blockHeight);
        }

        return y + blockHeight + 6;
    }

    private static double clamp01(double v, double min, double max) {
        if (max < min) return v; // caller bug; don't alter
        return Math.max(min, Math.min(max, v));
    }
}
