package com.lhamacorp.games.tlob.client.managers.renderers;

import com.lhamacorp.games.tlob.client.perks.Perk;
import com.lhamacorp.games.tlob.client.perks.PerkManager.Rarity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Victory overlay with perk selection cards.
 * Title and subtitle are centered and placed above the cards.
 */
public class VictoryScreenRenderer {

    private final List<Perk> choices = new ArrayList<>();
    private final Rectangle[] perkRects = new Rectangle[3];
    private String perkChoiceReason = "Choose ONE perk";
    private int playerXP = 0;
    private int playerLevel = 1;
    private int xpToNextLevel = 100;

    private final Font titleFont = new Font("Arial", Font.BOLD, 48);
    private final Font subtitleFont = new Font("Arial", Font.PLAIN, 18);
    private final Font cardTitleFont = new Font("Arial", Font.BOLD, 16);
    private final Font cardBodyFont = new Font("Arial", Font.PLAIN, 14);
    private final Font xpFont = new Font("Arial", Font.PLAIN, 14);

    // Rarity color scheme
    private static final Color COMMON_COLOR = new Color(100, 150, 255);      // Blue
    private static final Color UNCOMMON_COLOR = new Color(150, 200, 255);    // Light Blue
    private static final Color RARE_COLOR = new Color(255, 255, 100);        // Yellow
    private static final Color EPIC_COLOR = new Color(255, 150, 100);        // Orange
    private static final Color LEGENDARY_COLOR = new Color(255, 100, 100);   // Red

    // Cached layout
    private int screenW, screenH;
    private int titleBaselineY;
    private int subtitleBaselineY;

    public void setChoices(List<Perk> perks) {
        choices.clear();
        if (perks != null) choices.addAll(perks);
    }

    /**
     * Sets the reason why perks are being shown.
     * @param reason the reason text
     */
    public void setPerkChoiceReason(String reason) {
        this.perkChoiceReason = reason != null ? reason : "Choose ONE perk";
    }

    /**
     * Sets the player's XP and level information for display.
     * @param xp current XP
     * @param level current level
     * @param xpToNext XP required for next level
     */
    public void setPlayerProgress(int xp, int level, int xpToNext) {
        this.playerXP = xp;
        this.playerLevel = level;
        this.xpToNextLevel = xpToNext;
    }

    /** Compute title/subtitle and perk card rectangles for the current screen size. */
    public void layout(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;

        // Vertical layout:
        // [Title]           (centered around mid - offset)
        // [Subtitle]        (a bit below title)
        // [Perk cards row]  (below subtitle)

        titleBaselineY = screenH / 2 - 100;     // centered-ish, safely above cards
        subtitleBaselineY = titleBaselineY + 40;

        // Cards
        int cardW = 180, cardH = 100, gap = 20;
        int totalW = cardW * 3 + gap * 2;
        int startX = (screenW - totalW) / 2;
        int cardsTopY = subtitleBaselineY + 30; // ensure cards are *below* the message

        for (int i = 0; i < 3; i++) {
            int x = startX + i * (cardW + gap);
            perkRects[i] = new Rectangle(x, cardsTopY, cardW, cardH);
        }
    }

    /** Draws the overlay, centered title + subtitle, then the three perk cards below. */
    public void draw(Graphics2D g2) {
        // Dim background
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, screenW, screenH);

        // Title + subtitle (centered horizontally)
        drawCenteredString(g2, "VICTORY!", titleFont, titleBaselineY, Color.GREEN, screenW);
        drawCenteredString(g2, perkChoiceReason, subtitleFont, subtitleBaselineY, Color.WHITE, screenW);

        // Draw XP and level information
        drawPlayerProgress(g2);

        // Cards
        for (int i = 0; i < Math.min(3, choices.size()); i++) {
            Rectangle r = perkRects[i];
            if (r == null) continue;
            Perk p = choices.get(i);

            // Card background
            g2.setColor(new Color(30, 30, 30, 230));
            g2.fillRect(r.x, r.y, r.width, r.height);
            
            // Rarity border
            Color rarityColor = getRarityColor(p.rarity);
            g2.setColor(rarityColor);
            g2.setStroke(new BasicStroke(3)); // Thicker border for rarity
            g2.drawRect(r.x, r.y, r.width, r.height);

            // Title
            g2.setFont(cardTitleFont);
            g2.setColor(Color.WHITE);
            g2.drawString(p.name, r.x + 10, r.y + 22);

            // Body
            g2.setFont(cardBodyFont);
            g2.setColor(Color.LIGHT_GRAY);
            drawWrapped(g2, p.description, r.x + 10, r.y + 42, r.width - 20, 18);
        }
    }

    /** Handle click; return selected perk index [0..2] or -1 if none hit. */
    public int handleClick(Point p) {
        for (int i = 0; i < perkRects.length; i++) {
            Rectangle r = perkRects[i];
            if (r != null && r.contains(p)) return i;
        }
        return -1;
    }

    // Helpers
    private static void drawCenteredString(Graphics2D g2, String text, Font font, int baselineY, Color color, int screenW) {
        g2.setFont(font);
        g2.setColor(color);
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (screenW - tw) / 2, baselineY);
    }

    private static void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null) return;
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

    /**
     * Draws the player's XP and level progress information.
     */
    private void drawPlayerProgress(Graphics2D g2) {
        g2.setFont(xpFont);
        g2.setColor(Color.CYAN);
        
        // Calculate XP progress
        int xpForCurrentLevel = getXPRequiredForLevel(playerLevel);
        int xpProgress = playerXP - xpForCurrentLevel;
        int xpNeeded = xpToNextLevel - xpForCurrentLevel;
        double progress = xpNeeded > 0 ? (double) xpProgress / xpNeeded : 0.0;
        
        // XP text
        String xpText = String.format("Level %d - XP: %d/%d (%.1f%%)", 
                                     playerLevel, xpProgress, xpNeeded, progress * 100);
        int xpTextWidth = g2.getFontMetrics().stringWidth(xpText);
        int xpX = (screenW - xpTextWidth) / 2;
        int xpY = subtitleBaselineY + 15;
        g2.drawString(xpText, xpX, xpY);
        
        // XP progress bar
        int barWidth = 200;
        int barHeight = 8;
        int barX = (screenW - barWidth) / 2;
        int barY = xpY + 5;
        
        // Background bar
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(barX, barY, barWidth, barHeight);
        
        // Progress bar
        g2.setColor(Color.CYAN);
        int progressWidth = (int) (barWidth * progress);
        g2.fillRect(barX, barY, progressWidth, barHeight);
        
        // Border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(barX, barY, barWidth, barHeight);
    }

    /**
     * Gets the total XP required to reach a specific level.
     * This matches the calculation in the Player class.
     */
    private int getXPRequiredForLevel(int level) {
        if (level <= 1) return 0;
        
        int totalXP = 0;
        for (int i = 2; i <= level; i++) {
            totalXP += (int) (100 * Math.pow(1.5, i - 2));
        }
        return totalXP;
    }

    /**
     * Returns the color associated with a perk rarity.
     */
    private Color getRarityColor(Rarity rarity) {
        if (rarity == null) return Color.GRAY;
        
        switch (rarity) {
            case COMMON: return COMMON_COLOR;
            case UNCOMMON: return UNCOMMON_COLOR;
            case RARE: return RARE_COLOR;
            case EPIC: return EPIC_COLOR;
            case LEGENDARY: return LEGENDARY_COLOR;
            default: return Color.GRAY;
        }
    }
}
