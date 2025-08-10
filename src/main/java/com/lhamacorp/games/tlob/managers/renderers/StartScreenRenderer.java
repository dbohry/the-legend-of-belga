package com.lhamacorp.games.tlob.managers.renderers;

import java.awt.*;

/**
 * Simple start screen with a title and hero-name input.
 */
public class StartScreenRenderer {
    private static final int HERO_NAME_MAX_LEN = 12;

    private final String title;
    private final Font titleFont;
    private final Font subtitleFont;

    private String heroName = "";
    private boolean complete = false;

    public StartScreenRenderer(String title, Font titleFont, Font subtitleFont) {
        this.title = title;
        this.titleFont = titleFont;
        this.subtitleFont = subtitleFont;
    }

    /**
     * Call from paint when in START state. Uses the current component size for centering.
     */
    public void draw(Graphics2D g2) {
        int w = g2.getDeviceConfiguration().getBounds().width;
        int h = g2.getDeviceConfiguration().getBounds().height;

        // Dim background
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, w, h);

        // Title
        drawCenteredString(g2, title, titleFont, h / 3, Color.WHITE);

        // Prompt
        drawCenteredString(g2, "Enter your hero's name:", subtitleFont, h / 2 - 20, Color.WHITE);

        // Name line
        g2.setFont(subtitleFont);
        g2.setColor(new Color(255, 215, 0));
        String nameDisplay = heroName.isEmpty() ? "_" : heroName;
        int nameW = g2.getFontMetrics().stringWidth(nameDisplay);
        g2.drawString(nameDisplay, (w - nameW) / 2, h / 2 + 20);

        // Hint
        drawCenteredString(g2, "Press ENTER to start",
            new Font("Arial", Font.ITALIC, 16), h / 2 + 60, Color.LIGHT_GRAY);
    }

    /**
     * Route typed chars here while in START state.
     */
    public void keyTyped(char c) {
        if (complete) return;

        if (c == '\n' || c == '\r') {
            if (!heroName.trim().isEmpty()) {
                complete = true;
            }
            return;
        }
        if (c == '\b') { // backspace
            if (!heroName.isEmpty()) heroName = heroName.substring(0, heroName.length() - 1);
            return;
        }
        if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
            if (heroName.length() < HERO_NAME_MAX_LEN) {
                heroName += c;
            }
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public String getHeroName() {
        return heroName.trim();
    }

    private static void drawCenteredString(Graphics2D g2, String text, Font font, int y, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        int w = g2.getDeviceConfiguration().getBounds().width;
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (w - tw) / 2, y);
    }
}
