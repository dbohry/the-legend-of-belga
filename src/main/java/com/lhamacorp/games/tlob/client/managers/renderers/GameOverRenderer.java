package com.lhamacorp.games.tlob.client.managers.renderers;

import java.awt.*;

/**
 * Game over overlay with a centered "Try Again" button.
 */
public class GameOverRenderer {
    private final Font titleFont;
    private final Font buttonFont = new Font("Arial", Font.BOLD, 20);

    private Rectangle tryAgainButton;
    private int screenW, screenH;

    public GameOverRenderer(Font titleFont) {
        this.titleFont = titleFont;
    }

    /** Compute button placement for the current screen size. */
    public void layout(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;

        int btnW = 150, btnH = 40;
        int x = (screenW - btnW) / 2;
        int y = screenH / 2 + 50; // button sits beneath centered title
        tryAgainButton = new Rectangle(x, y, btnW, btnH);
    }

    public void draw(Graphics2D g2) {
        // Dim background
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, screenW, screenH);

        // Title centered horizontally and vertically
        drawCenteredString(g2, "GAME OVER", titleFont, screenH / 2 - 20, Color.RED, screenW);

        // Button
        if (tryAgainButton != null) {
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(tryAgainButton.x, tryAgainButton.y, tryAgainButton.width, tryAgainButton.height);
            g2.setColor(Color.WHITE);
            g2.drawRect(tryAgainButton.x, tryAgainButton.y, tryAgainButton.width, tryAgainButton.height);

            g2.setFont(buttonFont);
            String txt = "Try Again";
            int tw = g2.getFontMetrics().stringWidth(txt);
            int tx = tryAgainButton.x + (tryAgainButton.width - tw) / 2;
            int ty = tryAgainButton.y + (tryAgainButton.height + 15) / 2;
            g2.drawString(txt, tx, ty);
        }
    }

    public boolean hitTryAgain(Point p) {
        return tryAgainButton != null && tryAgainButton.contains(p);
    }

    private static void drawCenteredString(Graphics2D g2, String text, Font font, int baselineY, Color color, int screenW) {
        g2.setFont(font);
        g2.setColor(color);
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (screenW - tw) / 2, baselineY);
    }
}
