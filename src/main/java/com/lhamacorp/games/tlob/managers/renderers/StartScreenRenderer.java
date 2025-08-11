package com.lhamacorp.games.tlob.managers.renderers;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

public final class StartScreenRenderer {

    public enum Mode {SINGLEPLAYER, MULTIPLAYER}

    public static final class Result {
        public final Mode mode;
        public final String heroName;
        public final String host;
        public final int port;

        public Result(Mode mode, String heroName, String host, int port) {
            this.mode = mode;
            this.heroName = heroName;
            this.host = host;
            this.port = port;
        }
    }

    private final String title;
    private final Font titleFont;
    private final Font bodyFont;

    private final StringBuilder nameBuf = new StringBuilder();
    private final StringBuilder serverBuf = new StringBuilder("localhost:7777");

    private Rectangle btnSingle = new Rectangle();
    private Rectangle btnMulti = new Rectangle();
    private Rectangle nameField = new Rectangle();
    private Rectangle serverField = new Rectangle();

    private boolean complete = false;
    private Mode selected = Mode.SINGLEPLAYER;
    private Focus focus = Focus.NAME;

    private enum Focus {NAME, SERVER}

    public StartScreenRenderer(String title, Font titleFont, Font bodyFont) {
        this.title = Objects.requireNonNull(title);
        this.titleFont = Objects.requireNonNull(titleFont);
        this.bodyFont = Objects.requireNonNull(bodyFont);
    }

    // ---- Public API ----
    public boolean isComplete() {
        return complete;
    }

    public Result getResult() {
        String hero = (nameBuf.length() == 0) ? "Hero" : nameBuf.toString().trim();
        String host = "localhost";
        int port = 7777;
        String raw = serverBuf.toString().trim();
        if (!raw.isEmpty()) {
            int idx = raw.lastIndexOf(':');
            if (idx > 0 && idx < raw.length() - 1) {
                host = raw.substring(0, idx).trim();
                try {
                    port = Integer.parseInt(raw.substring(idx + 1).trim());
                } catch (Exception ignored) {
                }
            } else {
                host = raw;
            }
        }
        return new Result(selected, hero, host.isEmpty() ? "localhost" : host, port);
    }

    // Call from GameManager.paint START case
    public void draw(Graphics2D g2) {
        Rectangle vb = g2.getClipBounds();
        if (vb == null) vb = new Rectangle(0, 0, 1280, 720);

        // Layout
        int cx = vb.x + vb.width / 2;
        int y = vb.y + 120;

        // Title
        Font old = g2.getFont();
        Object aaOld = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(titleFont);
        String t = title;
        int tw = g2.getFontMetrics().stringWidth(t);
        drawSoftText(g2, t, cx - tw / 2, y);
        y += g2.getFontMetrics().getHeight() + 20;

        g2.setFont(bodyFont);

        // Name field
        String nameLbl = "Hero name:";
        int nameLblW = g2.getFontMetrics().stringWidth(nameLbl);
        drawSoftText(g2, nameLbl, cx - 280, y);
        nameField.setBounds(cx - 280 + nameLblW + 10, y - g2.getFontMetrics().getAscent(), 300, g2.getFontMetrics().getHeight() + 6);
        drawInput(g2, nameField, nameBuf.toString(), focus == Focus.NAME);
        y += g2.getFontMetrics().getHeight() + 24;

        // Buttons
        int btnW = 190, btnH = 44, gap = 16;
        btnSingle.setBounds(cx - btnW - gap / 2, y, btnW, btnH);
        btnMulti.setBounds(cx + gap / 2, y, btnW, btnH);
        drawButton(g2, btnSingle, "Single Player", selected == Mode.SINGLEPLAYER);
        drawButton(g2, btnMulti, "Multiplayer", selected == Mode.MULTIPLAYER);
        y += btnH + 16;

        // Server field (only visible when MP)
        if (selected == Mode.MULTIPLAYER) {
            String srvLbl = "Server (host:port):";
            int srvLblW = g2.getFontMetrics().stringWidth(srvLbl);
            drawSoftText(g2, srvLbl, cx - 280, y);
            serverField.setBounds(cx - 280 + srvLblW + 10, y - g2.getFontMetrics().getAscent(), 300, g2.getFontMetrics().getHeight() + 6);
            drawInput(g2, serverField, serverBuf.toString(), focus == Focus.SERVER);
            y += g2.getFontMetrics().getHeight() + 10;
        }

        // Hint
        String hint = "Press Enter to start";
        int hw = g2.getFontMetrics().stringWidth(hint);
        drawSoftTextDim(g2, hint, cx - hw / 2, y + 18);

        g2.setFont(old);
        if (aaOld != null) g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aaOld);
    }

    // Call from GameManager.StartScreenKeyHandler.keyTyped
    public void keyTyped(char ch) {
        if (ch == KeyEvent.VK_ENTER) {
            // Complete if valid (MP requires some server text)
            if (selected == Mode.SINGLEPLAYER || serverBuf.length() > 0) {
                complete = true;
            }
            return;
        }
        if (ch == '\t') {
            // toggle focus (and show server field if switching to it)
            focus = (focus == Focus.NAME) ? Focus.SERVER : Focus.NAME;
            if (selected != Mode.MULTIPLAYER && focus == Focus.SERVER) selected = Mode.MULTIPLAYER;
            return;
        }
        if (ch == KeyEvent.VK_BACK_SPACE) {
            StringBuilder buf = (focus == Focus.NAME) ? nameBuf : serverBuf;
            if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
            return;
        }
        if (Character.isISOControl(ch)) return;

        StringBuilder buf = (focus == Focus.NAME) ? nameBuf : serverBuf;
        if (buf.length() < 40) buf.append(ch);
    }

    // Call from GameManager mouse handler in START state
    public void handleClick(Point p) {
        if (btnSingle.contains(p)) {
            selected = Mode.SINGLEPLAYER;
            complete = true;
        } else if (btnMulti.contains(p)) {
            selected = Mode.MULTIPLAYER;
            // clicking MP without server typed is fine; user can press Enter later
        } else if (nameField.contains(p)) {
            focus = Focus.NAME;
        } else if (serverField.contains(p)) {
            selected = Mode.MULTIPLAYER;
            focus = Focus.SERVER;
        }
    }

    // ---- drawing helpers ----
    private static void drawButton(Graphics2D g2, Rectangle r, String text, boolean selected) {
        Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selected ? 0.95f : 0.75f));
        g2.setColor(selected ? new Color(50, 140, 220) : new Color(40, 40, 50));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);
        g2.setComposite(oc);
    }

    private static void drawInput(Graphics2D g2, Rectangle r, String text, boolean focused) {
        Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(new Color(20, 20, 25));
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g2.setColor(focused ? new Color(90, 170, 255) : new Color(80, 80, 95));
        g2.drawRect(r.x, r.y, r.width, r.height);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        g2.setColor(new Color(235, 235, 240));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + 6;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);
        g2.setComposite(oc);
    }

    private static void drawSoftText(Graphics2D g2, String s, int x, int y) {
        Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(Color.BLACK);
        g2.drawString(s, x + 1, y + 1);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        g2.setColor(new Color(235, 235, 240));
        g2.drawString(s, x, y);
        g2.setComposite(oc);
    }

    private static void drawSoftTextDim(Graphics2D g2, String s, int x, int y) {
        Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2.setColor(Color.BLACK);
        g2.drawString(s, x + 1, y + 1);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(new Color(220, 225, 230));
        g2.drawString(s, x, y);
        g2.setComposite(oc);
    }
}