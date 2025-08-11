// GameManagerMultiplayer.java
package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.common.net.Protocol;
import com.lhamacorp.games.tlob.common.net.Protocol.EnemySnap;
import com.lhamacorp.games.tlob.common.net.Protocol.PlayerSnap;
import com.lhamacorp.games.tlob.common.net.Protocol.Snapshot;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class MultiplayerGameManager extends BaseGameManager {

    private final String host;
    private final int port;
    private final String heroName;

    private int serverTickrate = 60;
    private boolean showNetBanner = false;
    private int netBannerTicks = 0;
    private String netBannerText = "";

    private NetConn net;
    private volatile int myNetId = -1;
    private volatile double meTargetX = Double.NaN, meTargetY = Double.NaN;

    // Remote views driven by snapshots
    private final ConcurrentHashMap<Integer, RemotePlayerView> remotePlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, RemoteEnemyView> remoteEnemies = new ConcurrentHashMap<>();

    private boolean prevAttack = false;
    private int lastFacingOct = 0;

    // simple local animation clock for remote sprites
    private static final int TICK_MS = 1000 / 60;

    public MultiplayerGameManager(String host, int port, String heroName) {
        super();
        this.host = host;
        this.port = port;
        this.heroName = (heroName == null || heroName.isBlank()) ? "Hero" : heroName;
        connectAsync();
    }

    // ---------- Net connect -----------

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
                    initWorld(nc.seed);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this, "Failed to connect to " + host + ":" + port + "\n" + ex.getMessage(),
                    "Connection error", JOptionPane.ERROR_MESSAGE));
            }
        }, "NetConnect").start();
    }

    private NetConn connectToServer(String host, int port, String playerName) throws IOException {
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(host, port), 4000);
        sock.setSoTimeout(8000);
        sock.setTcpNoDelay(true);

        var out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
        var in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));

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

        try {
            sock.setSoTimeout(0);
        } catch (Exception ignored) {
        }
        return nc;
    }


    // ---------- Snapshot reader ----------

    private void snapshotReaderLoop(NetConn nc) {
        try {
            String line;
            while (nc.running && (line = nc.in.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("SNAPSHOT")) continue;

                // Let Protocol read the full block (until END)
                Snapshot snap = Protocol.readSnapshot(nc.in, line);
                if (snap == null) continue;

                // --- my player: set authoritative target only; smooth in update loop
                PlayerSnap me = snap.players.get(nc.myId);
                if (me != null) {
                    meTargetX = me.x;
                    meTargetY = me.y;
                    if (player != null) {
                        player.setHealth(me.hp);
                        player.setStamina(me.st);
                        player.setShield(me.sh);
                        player.setFacingOctant(me.facing);
                    }
                }

                // --- remote players
                java.util.Set<Integer> seenP =
                    java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
                for (PlayerSnap ps : snap.players.values()) {
                    if (ps.id == nc.myId) continue;
                    seenP.add(ps.id);
                    remotePlayers.compute(ps.id, (id, prev) -> {
                        if (prev == null) prev = new RemotePlayerView(id);
                        prev.apply(ps);   // sets new target; smoothing happens in tick()
                        return prev;
                    });
                }
                remotePlayers.keySet().removeIf(id -> !seenP.contains(id));

                // --- remote enemies
                java.util.Set<Integer> seenE =
                    java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
                for (EnemySnap es : snap.enemies.values()) {
                    seenE.add(es.id);
                    if (!es.alive) {
                        remoteEnemies.remove(es.id);
                        continue;
                    }
                    remoteEnemies.compute(es.id, (id, prev) -> {
                        if (prev == null) prev = new RemoteEnemyView(id);
                        prev.apply(es);   // sets new target; smoothing happens in tick()
                        return prev;
                    });
                }
                remoteEnemies.keySet().removeIf(id -> !seenE.contains(id));
            }
        } catch (IOException ignored) {
        } finally {
            nc.running = false;
            try {
                nc.sock.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ---------- Game loop integration ----------

    @Override
    protected void updatePlaying(Point aimWorld) {
        // 1) send input each tick
        sendInputToServer(aimWorld);

        // 2) smooth my local player toward last server target, but NEVER through walls
        if (player != null && !Double.isNaN(meTargetX)) {
            final double gain = 0.18; // smoothing factor
            double cx = player.getX();
            double cy = player.getY();

            double nx = cx + (meTargetX - cx) * gain;
            double ny = cy + (meTargetY - cy) * gain;

            if (!collidesPlayerBox(nx, ny)) {
                player.setPosition(nx, ny);
            } else {
                // try sliding on each axis
                double sx = (!collidesPlayerBox(nx, cy)) ? nx : cx;
                double sy = (!collidesPlayerBox(cx, ny)) ? ny : cy;
                player.setPosition(sx, sy);
            }
        }

        // 3) advance remote anim clocks + smooth remotes toward their targets
        remotePlayers.values().forEach(RemotePlayerView::tick);
        remoteEnemies.values().forEach(RemoteEnemyView::tick);

        // 4) local cosmetic animation for my sprite
        boolean movingKeys = input.left || input.right || input.up || input.down;
        if (player != null) player.tickClientAnimations(movingKeys);

        // net banner timeout
        if (showNetBanner && --netBannerTicks <= 0) {
            showNetBanner = false;
            netBannerTicks = 0;
        }
    }

    @Override
    protected String[] topRightExtraLines() {
        return new String[]{"Net: " + serverTickrate + " Hz"};
    }

    // ---------- Drawing hooks (textures!) ----------

    @Override
    protected void drawRemotePlayers(Graphics2D g2, int camX, int camY) {
        for (RemotePlayerView p : remotePlayers.values()) {
            p.draw(g2, camX, camY);
        }
    }

    @Override
    protected void drawRemoteEnemies(Graphics2D g2, int camX, int camY) {
        for (RemoteEnemyView e : remoteEnemies.values()) {
            e.draw(g2, camX, camY);
        }
    }

    // ---------- Input -> server ----------

    private void sendInputToServer(Point aimWorld) {
        if (net == null || !net.running) return;

        int dx = (input.right ? 1 : 0) - (input.left ? 1 : 0);
        int dy = (input.down ? 1 : 0) - (input.up ? 1 : 0);
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

    // ---------- Helpers/inner types ----------

    /** Client-side AABB vs tile grid for my player (must match server PLAYER_HALF=11). */
    private boolean collidesPlayerBox(double cx, double cy) {
        if (levelManager == null) return false;
        TileMap m = levelManager.map();
        if (m == null) return false;

        final int half = 11;
        final int ts = TILE_SIZE;

        int left = (int) Math.floor((cx - half) / ts);
        int right = (int) Math.floor((cx + half - 1) / ts);
        int top = (int) Math.floor((cy - half) / ts);
        int bot = (int) Math.floor((cy + half - 1) / ts);

        for (int ty = top; ty <= bot; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (m.isWall(tx, ty)) return true;
            }
        }
        return false;
    }


    private static int angleToOctant(double ang) {
        int o = (int) Math.round(ang / (Math.PI / 4.0));
        if (o < 0) o += 8;
        return o & 7;
    }

    private static TextureManager.Direction octantToCardinal(int o) {
        // 0→R, 1/2/3→DOWN, 4→LEFT, 5/6/7→UP
        switch (o & 7) {
            case 0:
                return TextureManager.Direction.RIGHT;
            case 4:
                return TextureManager.Direction.LEFT;
            case 1:
            case 2:
            case 3:
                return TextureManager.Direction.DOWN;
            default:
                return TextureManager.Direction.UP;
        }
    }

    private static final class RemotePlayerView {
        static final int SIZE = 22;

        final int id;
        String name;
        // rendered position
        double x, y;
        // server target position (latest snapshot)
        double tx, ty;
        int facingOct;
        boolean alive;
        long animMs;
        boolean initialized = false;

        RemotePlayerView(int id) {
            this.id = id;
        }

        void apply(PlayerSnap ps) {
            this.name = ps.name;
            this.facingOct = ps.facing;
            this.alive = ps.alive;
            this.tx = ps.x;
            this.ty = ps.y;
            if (!initialized) { // snap to first sample to avoid slide-in from (0,0)
                this.x = tx;
                this.y = ty;
                initialized = true;
            }
        }

        void tick() {
            animMs += TICK_MS;
            if (!initialized) return;
            // smooth toward server target
            final double gain = 0.20;
            x += (tx - x) * gain;
            y += (ty - y) * gain;
        }

        void draw(Graphics2D g2, int camX, int camY) {
            if (!alive) return;

            boolean moving = (Math.abs(tx - x) + Math.abs(ty - y)) > 0.1;
            TextureManager.Direction dir = octantToCardinal(facingOct);
            TextureManager.Motion motion = moving ? TextureManager.Motion.WALK : TextureManager.Motion.IDLE;

            TextureManager.SpriteAnimation anim = TextureManager.getPlayerAnimation(dir, motion);
            BufferedImage tex = (anim != null && anim.length() > 0) ? anim.frameAt(animMs)
                : TextureManager.getPlayerTexture();

            int px = (int) Math.round(x - SIZE / 2.0) - camX;
            int py = (int) Math.round(y - SIZE / 2.0) - camY;

            if (tex != null) g2.drawImage(tex, px, py, SIZE, SIZE, null);
            else {
                g2.setColor(new Color(60, 160, 255, 220));
                g2.fillOval(px, py, SIZE, SIZE);
            }

            if (name != null && !name.isBlank()) {
                g2.setColor(new Color(240, 240, 255, 220));
                g2.setFont(new Font("Arial", Font.PLAIN, 11));
                g2.drawString(name, px - 12, py - 4);
            }
        }
    }

    private static final class RemoteEnemyView {
        static final int SIZE = 20;

        final int id;
        // rendered position
        double x, y;
        // server target
        double tx, ty;
        double hp;
        boolean alive;
        long animMs;
        boolean initialized = false;

        RemoteEnemyView(int id) { this.id = id; }

        void apply(EnemySnap es) {
            boolean wasAlive = this.alive;

            this.tx = es.x;
            this.ty = es.y;
            this.hp = es.hp;
            this.alive = es.alive;

            if (!initialized) { this.x = tx; this.y = ty; initialized = true; }

            // play death sound once on transition alive -> dead
            if (wasAlive && !this.alive) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        // pick the name you actually use in SP; keep a fallback chain
                        AudioManager.playSound("enemy-death.wav");
                    } catch (Throwable ignored) { }
                });
            }
        }

        void tick() {
            animMs += TICK_MS;
            if (!initialized) return;
            final double gain = 0.22;
            x += (tx - x) * gain;
            y += (ty - y) * gain;
        }

        void draw(Graphics2D g2, int camX, int camY) {
            if (!alive) return;
            int px = (int) Math.round(x - SIZE / 2.0) - camX;
            int py = (int) Math.round(y - SIZE / 2.0) - camY;

            BufferedImage tex = null;
            try { tex = TextureManager.getEnemyTexture(); } catch (Throwable ignored) {}
            if (tex != null) g2.drawImage(tex, px, py, SIZE, SIZE, null);
            else {
                g2.setColor(new Color(200, 70, 70, 220));
                g2.fillOval(px, py, SIZE, SIZE);
            }
        }
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
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
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
