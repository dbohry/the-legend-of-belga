package com.lhamacorp.games.tlob.client.managers.renderers;

import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.weapons.Weapon;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Renders the player stats page that appears when TAB is pressed.
 */
public class StatsRenderer {

    private final Font titleFont;
    private final Font statFont;
    private final Font valueFont;
    private final Font sectionFont;

    private static final Color BACKGROUND_COLOR = new Color(20, 20, 30, 220);
    private static final Color BORDER_COLOR = new Color(100, 150, 255, 180);
    private static final Color TITLE_COLOR = new Color(255, 255, 255);
    private static final Color STAT_COLOR = new Color(200, 200, 200);
    private static final Color VALUE_COLOR = new Color(100, 255, 150);
    private static final Color SECTION_COLOR = new Color(150, 200, 255);

    /**
     * Creates a new StatsRenderer with the specified fonts.
     */
    public StatsRenderer(Font titleFont, Font statFont, Font valueFont, Font sectionFont) {
        this.titleFont = titleFont;
        this.statFont = statFont;
        this.valueFont = valueFont;
        this.sectionFont = sectionFont;
    }

    /**
     * Creates a new StatsRenderer with default fonts.
     */
    public StatsRenderer() {
        this(
            new Font("Arial", Font.BOLD, 20),
            new Font("Arial", Font.PLAIN, 14),
            new Font("Arial", Font.BOLD, 14),
            new Font("Arial", Font.BOLD, 16)
        );
    }

    /**
     * Draws the stats page for the given player.
     */
    public void draw(Graphics2D g2, Player player) {
        if (player == null) return;

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate dimensions
        int width = 380;
        int height = 420;
        Rectangle clipBounds = g.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, 800, 600); // Default fallback
        }
        int x = (clipBounds.width - width) / 2;
        int y = (clipBounds.height - height) / 2;

        // Draw background
        drawBackground(g, x, y, width, height);

        // Draw title
        drawTitle(g, x, y, width);

        // Draw stats sections
        int currentY = y + 50;
        currentY = drawHealthStats(g, x, y, width, currentY, player);
        currentY = drawCombatStats(g, x, y, width, currentY, player);
        currentY = drawMovementStats(g, x, y, width, currentY, player);

        g.dispose();
    }

    private void drawBackground(Graphics2D g, int x, int y, int width, int height) {
        // Background
        g.setColor(BACKGROUND_COLOR);
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 20, 20));

        // Border
        g.setStroke(new BasicStroke(2));
        g.setColor(BORDER_COLOR);
        g.draw(new RoundRectangle2D.Double(x, y, width, height, 20, 20));
    }

    private void drawTitle(Graphics2D g, int x, int y, int width) {
        g.setFont(titleFont);
        g.setColor(TITLE_COLOR);
        
        String title = "PLAYER STATS";
        FontMetrics fm = g.getFontMetrics();
        int titleX = x + (width - fm.stringWidth(title)) / 2;
        int titleY = y + fm.getAscent() + 20;
        
        g.drawString(title, titleX, titleY);
    }

    private int drawHealthStats(Graphics2D g, int x, int y, int width, int startY, Player player) {
        g.setFont(sectionFont);
        g.setColor(SECTION_COLOR);
        g.drawString("HEALTH & RESOURCES", x + 20, startY);
        
        int currentY = startY + 25;
        
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        
        // Max Health
        g.drawString("Max Health:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f", player.getMaxHealth()), x + 200, currentY);
        
        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Max Shield:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f", player.getMaxShield()), x + 200, currentY);
        
        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Max Stamina:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f", player.getMaxStamina()), x + 200, currentY);
        
        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Max Mana:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f", player.getMaxMana()), x + 200, currentY);
        
        return currentY + 15;
    }

    private int drawCombatStats(Graphics2D g, int x, int y, int width, int startY, Player player) {
        g.setFont(sectionFont);
        g.setColor(SECTION_COLOR);
        g.drawString("COMBAT STATS", x + 20, startY);
        
        int currentY = startY + 25;
        
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        
        // Damage multiplier
        g.drawString("Damage Multiplier:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1fx", player.getDamageMultiplier()), x + 200, currentY);
        
        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Weapon Range:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%d", player.getWeapon().getReach()), x + 200, currentY);
        
        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Weapon Width:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%d", player.getWeapon().getWidth()), x + 200, currentY);
        
        return currentY + 15;
    }

    private int drawMovementStats(Graphics2D g, int x, int y, int width, int startY, Player player) {
        g.setFont(sectionFont);
        g.setColor(SECTION_COLOR);
        g.drawString("MOVEMENT STATS", x + 20, startY);
        
        int currentY = startY + 25;
        
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        
        // Effective Speed
        g.drawString("Effective Speed:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        double effectiveSpeed = player.getSpeed() * player.getSpeedMultiplier();
        g.drawString(String.format("%.1f", effectiveSpeed), x + 200, currentY);
        
        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Dash Mana Cost:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f", player.getDashManaCost()), x + 200, currentY);
        
        return currentY + 15;
    }


}
