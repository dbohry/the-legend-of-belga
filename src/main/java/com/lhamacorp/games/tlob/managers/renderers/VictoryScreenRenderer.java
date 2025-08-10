package com.lhamacorp.games.tlob.managers.renderers;

import com.lhamacorp.games.tlob.perks.Perk;

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

    private final Font titleFont = new Font("Arial", Font.BOLD, 48);
    private final Font subtitleFont = new Font("Arial", Font.PLAIN, 18);
    private final Font cardTitleFont = new Font("Arial", Font.BOLD, 16);
    private final Font cardBodyFont = new Font("Arial", Font.PLAIN, 14);

    // Cached layout
    private int screenW, screenH;
    private int titleBaselineY;
    private int subtitleBaselineY;

    public void setChoices(List<Perk> perks) {
        choices.clear();
        if (perks != null) choices.addAll(perks);
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
        drawCenteredString(g2, "Choose ONE perk", subtitleFont, subtitleBaselineY, Color.WHITE, screenW);

        // Cards
        for (int i = 0; i < Math.min(3, choices.size()); i++) {
            Rectangle r = perkRects[i];
            if (r == null) continue;
            Perk p = choices.get(i);

            // Card
            g2.setColor(new Color(30, 30, 30, 230));
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor(Color.WHITE);
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
}
