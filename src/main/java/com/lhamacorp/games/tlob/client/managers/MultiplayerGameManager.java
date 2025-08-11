package com.lhamacorp.games.tlob.client.managers;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.lhamacorp.games.tlob.common.math.Dir8;
import com.lhamacorp.games.tlob.common.net.Protocol;
import com.lhamacorp.games.tlob.common.net.Protocol.Snapshot;
import com.lhamacorp.games.tlob.common.net.Protocol.PlayerSnap;
import com.lhamacorp.games.tlob.common.net.Protocol.EnemySnap;

public class MultiplayerGameManager extends BaseGameManager {

    private final String host;
    private final int port;
    private final String heroName;

    private int serverTickrate = 60;
    private boolean showNetBanner = false;
    private int netBannerTicks = 0;
    private String netBannerText = "";

    private NetConn net;
    private final ConcurrentHashMap<Integer, NetPlayer> netPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, NetEnemy> netEnemies = new ConcurrentHashMap<>();
    private volatile int myNetId = -1;

    private boolean prevAttack = false;
    private int lastFacingOct = 0;

    public MultiplayerGameManager(String host, int port, String heroName) {
        super();
        this.host = host;
        this.port = port;
        this.heroName = (heroName == null || heroName.isBlank()) ? "Hero" : heroName;
        connectAsync();
    }

    private void connectAsync() {
        new Thread(() -> {
            try {
                NetConn nc = connectToServer(host, port, heroName);
                this.net = nc;
                this.serverTickrate = nc.tickrate;
                this.myNetId = nc.myId;

                SwingUtilities.invokeLater(() -> {
                    netBannerText = "Connected: " + host + ":" + port + " • " + serverTickrate + " Hz";
                    showNetBanner = true;
                    netBannerTicks = 180;
                    initWorld(nc.seed); // map & player only; enemies come from server
                    enemies.clear();
                    netEnemies.clear();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Failed to connect to " + host + ":" + port + "\n" + ex.getMessage(),
                        "Connection error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "NetConnect").start();
    }

    @Override
    protected void updatePlaying(Point aimWorld) {
        sendInputToServer(aimWorld);
        boolean movingKeys = input.left || input.right || input.up || input.down;
        if (player != null) player.tickClientAnimations(movingKeys);

        if (showNetBanner && --netBannerTicks <= 0) {
            showNetBanner = false;
            netBannerTicks = 0;
        }
    }

    @Override
    protected void drawRemotePlayers(Graphics2D g2, int camX, int camY) {
        // Draw other players
        for (NetPlayer np : netPlayers.values()) {
            if (np.id == myNetId) continue;
            int px = (int) Math.round(np.x) - camX;
            int py = (int) Math.round(np.y) - camY;
            g2.setColor(new Color(60, 160, 255, 220));
            g2.fillOval(px - 8, py - 8, 16, 16);
            if (np.name != null) {
                g2.setColor(new Color(240, 240, 255, 220));
                g2.setFont(new Font("Arial", Font.PLAIN, 11));
                g2.drawString(np.name, px - 12, py - 12);
            }
        }

        // Draw server enemies
        for (NetEnemy ne : netEnemies.values()) {
            if (!ne.alive) continue;
            int ex = (int) Math.round(ne.x) - camX;
            int ey = (int) Math.round(ne.y) - camY;
            g2.setColor(new Color(220, 70, 70, 230));
            g2.fillOval(ex - 10, ey - 10, 20, 20);
            g2.setColor(new Color(255,255,255,180));
            g2.drawOval(ex - 10, ey - 10, 20, 20);
        }
    }

    @Override
    protected void drawRemoteEnemies(Graphics2D g2, int camX, int camY) {
        for (NetEnemy ne : netEnemies.values()) {
            if (!ne.alive) continue;
            int px = (int) Math.round(ne.x) - camX;
            int py = (int) Math.round(ne.y) - camY;

            g2.setColor(new Color(200, 60, 60, 220));
            g2.fillOval(px - 9, py - 9, 18, 18);
        }
    }

    @Override
    protected String[] topRightExtraLines() {
        return new String[]{"Net: " + serverTickrate + " Hz"};
    }

    // Draw connection banner on top of base paint
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!showNetBanner || netBannerText == null || netBannerText.isBlank()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        int fadeTicks = Math.min(netBannerTicks, 60);
        float alpha = Math.max(0f, Math.min(1f, fadeTicks / 60f));

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(netBannerText) + 16;
        int h = fm.getHeight() + 10;

        int x = (getWidth() - w) / 2;
        int y = 12;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f * alpha));
        g2.setColor(Color.BLACK);
        g2.fillRoundRect(x, y, w, h, 10, 10);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f * alpha));
        g2.setColor(new Color(235, 240, 245));
        g2.drawString(netBannerText, x + 8, y + 6 + fm.getAscent());

        g2.dispose();
    }
    // --- networking ---

    private NetConn connectToServer(String host, int port, String playerName) throws IOException {
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(host, port), 4000);
        sock.setSoTimeout(8000);

        var out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
        var in  = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));

        long seed = 0L;
        int tick = 60;
        int myId = -1;
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("HELLO")) {
                sendLine(out, "LOGIN name=" + safe(playerName));
            } else if (line.startsWith("SEED")) {
                String[] p = line.split("\\s+");
                if (p.length >= 2) seed = parseLong(p[1], 0L);
            } else if (line.startsWith("TICKRATE")) {
                String[] p = line.split("\\s+");
                if (p.length >= 2) tick = (int) parseLong(p[1], 60L);
            } else if (line.startsWith("YOU")) {
                int eq = line.indexOf("id=");
                if (eq >= 0) myId = (int) parseLong(line.substring(eq + 3).trim(), -1);
            } else if (line.equals("READY")) {
                break;
            }
        }
        if (seed == 0L) throw new IOException("Server did not provide seed.");

        NetConn nc = new NetConn();
        nc.sock = sock;
        nc.in = in;
        nc.out = out;
        nc.seed = seed;
        nc.tickrate = tick;
        nc.myId = myId;

        nc.reader = new Thread(() -> snapshotReaderLoop(nc), "NetReader");
        nc.reader.setDaemon(true);
        nc.reader.start();

        try { sock.setSoTimeout(0); } catch (Exception ignored) {}
        return nc;
    }

    // Add to GameManagerMultiplayer
    private void snapshotReaderLoop(NetConn nc) {
        try {
            String line;
            while (nc.running && (line = nc.in.readLine()) != null) {
                if (line.startsWith("SNAPSHOT")) {
                    Snapshot s = Protocol.readSnapshot(nc.in, line);

                    if (nc.myId > 0 && player != null) {
                        PlayerSnap me = s.players.get(nc.myId);
                        if (me != null) {
                            player.setPosition(me.x, me.y);
                            player.setHealth(me.hp);
                            player.setStamina(me.st);
                            player.setShield(me.sh);
                            player.setFacingOctant(me.facing);
                        }
                    }

                    // publish for drawing
                    netPlayers.clear();
                    for (PlayerSnap p : s.players.values()) {
                        NetPlayer np = new NetPlayer();
                        np.id = p.id; np.x = p.x; np.y = p.y; np.hp = p.hp; np.st = p.st; np.sh = p.sh;
                        np.facing = p.facing; np.alive = p.alive; np.name = p.name;
                        netPlayers.put(np.id, np);
                    }
                    netEnemies.clear();
                    for (EnemySnap e : s.enemies.values()) {
                        NetEnemy ne = new NetEnemy();
                        ne.id = e.id; ne.x = e.x; ne.y = e.y; ne.hp = e.hp; ne.alive = e.alive;
                        netEnemies.put(ne.id, ne);
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            nc.running = false;
            try { nc.sock.close(); } catch (IOException ignored) {}
        }
    }

    private void sendInputToServer(Point aimWorld) {
        if (net == null || !net.running) return;

        int dx = (input.right ? 1 : 0) - (input.left ? 1 : 0);
        int dy = (input.down  ? 1 : 0) - (input.up   ? 1 : 0);
        boolean atk = input.attack;

        int facing = lastFacingOct;
        if (aimWorld != null && player != null) {
            double ax = aimWorld.x - player.getX();
            double ay = aimWorld.y - player.getY();
            facing = angleToOctant(Math.atan2(ay, ax));
        } else if (dx != 0 || dy != 0) {
            facing = angleToOctant(Math.atan2(dy, dx));
        }
        lastFacingOct = facing;

        String msg = "INPUT t=" + simTick +
            " dx=" + dx +
            " dy=" + dy +
            " sprint=" + (input.shift ? 1 : 0) +
            " attack=" + (atk ? 1 : 0) +
            " facing=" + facing;

        try {
            net.out.write(msg);
            net.out.write("\n");
            net.out.flush();
        } catch (IOException e) {
            net.running = false;
        }

        if (player != null) player.setFacingOctant(facing);
        if (atk && !prevAttack && player != null) {
            player.triggerLocalAttackFx();
            AudioManager.playSound("slash-clean.wav");
        }
        prevAttack = atk;
    }

    private static int angleToOctant(double ang) {
        int o = (int) Math.round(ang / (Math.PI / 4.0));
        if (o < 0) o += 8;
        return o & 7;
    }

    private static void sendLine(Writer out, String s) throws IOException {
        out.write(s);
        out.write("\n");
        out.flush();
    }

    private static String safe(String s) {
        return s.replaceAll("[\\r\\n\\t]", "_");
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private static final class NetPlayer {
        int id, facing;
        double x, y, hp, st, sh;
        boolean alive;
        String name;
    }

    private static final class NetEnemy {
        int id;
        double x, y, hp;
        boolean alive;
    }

    private static final class NetConn {
        Socket sock;
        BufferedReader in;
        BufferedWriter out;
        Thread reader;
        volatile boolean running = true;
        long seed;
        int tickrate = 60;
        int myId = -1;
    }
}
