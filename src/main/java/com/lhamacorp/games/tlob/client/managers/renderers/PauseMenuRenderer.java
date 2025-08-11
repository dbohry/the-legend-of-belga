package com.lhamacorp.games.tlob.client.managers.renderers;

import java.awt.*;

public final class PauseMenuRenderer {

    private final Font title, button, label;
    private Rectangle resume, restart, exit, volumeRect;
    private final float volMinDb, volMaxDb;

    public PauseMenuRenderer(Font title, Font button, Font label, float volMinDb, float volMaxDb) {
        this.title = title;
        this.button = button;
        this.label = label;
        this.volMinDb = volMinDb;
        this.volMaxDb = volMaxDb;
    }

    /**
     * Call this whenever the screen size changes or you enter the pause state.
     */
    public void layout(int screenW, int screenH, int btnW, int btnH) {
        int x = (screenW - btnW) / 2, y = screenH / 2;
        resume = new Rectangle(x, y - 60, btnW, btnH);
        restart = new Rectangle(x, y, btnW, btnH);
        exit = new Rectangle(x, y + 60, btnW, btnH);

        // Volume slider above the Resume button
        int barW = 220, barH = 10;
        int barX = (screenW - barW) / 2;
        int resumeTop = resume.y;
        int barY = (resumeTop - 12) - barH - 6;     // keep a comfortable gap
        volumeRect = new Rectangle(barX, barY - 6, barW, barH + 12); // include padding for easier clicks
    }

    /**
     * Draw the dim overlay, centered title, buttons, and the slider filled to currentDb.
     */
    public void draw(Graphics2D g2, float currentDb) {
        // Resolve screen size from the current clip (works regardless of panel size)
        Rectangle clip = g2.getClipBounds();
        int screenW = (clip != null) ? clip.width : 1280;
        int screenH = (clip != null) ? clip.height : 720;

        // Smooth text for nicer UI
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dim backdrop
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, screenW, screenH);

        // Title centered horizontally, placed safely above the resume button (or mid-screen fallback)
        int titleY = (resume != null ? Math.max(32, resume.y - 120) : screenH / 2 - 120);
        drawCentered(g2, "PAUSED", title, titleY, Color.WHITE, screenW);

        // Slider (track, fill, knob, label)
        drawSlider(g2, currentDb);

        // Buttons
        drawButton(g2, resume, "Resume", new Color(60, 120, 60));
        drawButton(g2, restart, "Restart", new Color(120, 120, 60));
        drawButton(g2, exit, "Exit", new Color(120, 60, 60));

        // restore AA hint
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
    }

    public boolean hitResume(Point p) {
        return resume != null && resume.contains(p);
    }

    public boolean hitRestart(Point p) {
        return restart != null && restart.contains(p);
    }

    public boolean hitExit(Point p) {
        return exit != null && exit.contains(p);
    }

    /**
     * Returns NaN if the point is outside the slider; else the mapped dB value.
     */
    public float dbFromPoint(Point p) {
        if (volumeRect == null || !volumeRect.contains(p)) return Float.NaN;
        float t = (float) (p.x - volumeRect.x) / (float) volumeRect.width;
        t = Math.max(0f, Math.min(1f, t));
        return volMinDb + t * (volMaxDb - volMinDb);
    }

    /* ======================== internal drawing helpers ======================== */

    private void drawButton(Graphics2D g2, Rectangle r, String text, Color fill) {
        if (r == null) return;
        g2.setColor(fill);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(r.x, r.y, r.width, r.height);

        g2.setFont(button);
        int tw = g2.getFontMetrics().stringWidth(text);
        int tx = r.x + (r.width - tw) / 2;
        int ty = r.y + (r.height + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
        g2.drawString(text, tx, ty);
    }

    private void drawSlider(Graphics2D g2, float currentDb) {
        if (volumeRect == null) return;

        // Map dB to [0..1]
        float clampedDb = Math.max(volMinDb, Math.min(volMaxDb, currentDb));
        float t = (volMaxDb == volMinDb) ? 0f : (clampedDb - volMinDb) / (volMaxDb - volMinDb);

        // Real track inside the padded clickable rect
        int trackX = volumeRect.x;
        int trackY = volumeRect.y + 6;
        int trackW = volumeRect.width;
        int trackH = volumeRect.height - 12;

        // Track
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(trackX, trackY, trackW, trackH);
        g2.setColor(Color.WHITE);
        g2.drawRect(trackX, trackY, trackW, trackH);

        // Fill
        int fillW = Math.round(t * trackW);
        g2.setColor(new Color(100, 150, 255));
        g2.fillRect(trackX, trackY, fillW, trackH);

        // Knob
        int knobX = trackX + fillW - 4;
        int knobY = trackY - 4;
        g2.setColor(Color.WHITE);
        g2.fillRect(knobX, knobY, 8, trackH + 8);

        // Label centered above the slider
        g2.setFont(label);
        String volLabel = String.format("Music Volume: %.0f dB", clampedDb);
        int labelW = g2.getFontMetrics().stringWidth(volLabel);
        int lx = trackX + (trackW - labelW) / 2;
        int ly = trackY - 10;
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString(volLabel, lx, ly);
    }

    private static void drawCentered(Graphics2D g2, String text, Font font, int baselineY, Color color, int screenW) {
        g2.setFont(font);
        g2.setColor(color);
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (screenW - tw) / 2, baselineY);
    }
}