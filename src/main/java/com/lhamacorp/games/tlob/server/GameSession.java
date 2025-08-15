package com.lhamacorp.games.tlob.server;

import com.lhamacorp.games.tlob.core.Constants;
import com.lhamacorp.games.tlob.core.math.Dir8;
import com.lhamacorp.games.tlob.core.net.Protocol;
import com.lhamacorp.games.tlob.core.net.Protocol.EnemySnap;
import com.lhamacorp.games.tlob.core.net.Protocol.Input;
import com.lhamacorp.games.tlob.core.net.Protocol.PlayerSnap;
import com.lhamacorp.games.tlob.core.net.Protocol.Snapshot;
import com.lhamacorp.games.tlob.core.world.GridMap;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Authoritative session (single map, fixed tick).
 * - Accepts clients, reads INPUT, simulates players+enemies with tile collisions,
 *   and broadcasts SNAPSHOTs using the shared Protocol.
 */
public final class GameSession implements Runnable {

    // ----- Config -----
    final long seed;
    final int tickrate;
    private static final Path SAVE_FILE = Path.of("save.json");

    // Map + physics (match client defaults)
    private static final int MAP_W = 80, MAP_H = 60;
    private static final int PLAYER_HALF = 11;
    private static final int ENEMY_HALF = 10;

    // Sword + enemy tuning (simple, deterministic)
    private static final double SWORD_REACH = 30, SWORD_WIDTH = 16.0, SWORD_DMG = 2.0;
    private static final int SWORD_COOLDOWN_TICKS = 10, SWORD_DURATION_TICKS = 16;

    private static final double ENEMY_SPEED = 55.0;
    private static final double ENEMY_MELEE_RANGE = 14.0;
    private static final double ENEMY_DMG_PER_SEC = 1.0;

    // ----- State -----
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
    private final List<EnemyState> enemies = new CopyOnWriteArrayList<>();
    private final BlockingQueue<ClientPacket> inputQ = new LinkedBlockingQueue<>();
    private final List<ClientConn> conns = new CopyOnWriteArrayList<>();

    private static int lcgNext(int s) {
        return s * 1664525 + 1013904223;
    }

    private static double lcg01(int s) {
        return ((s >>> 8) & 0xFFFFFF) / (double) (1 << 24);
    }

    private final GridMap grid;

    volatile boolean running = true;
    int tick = 0;

    public GameSession(long seed, int tickrate) {
        this.seed = seed;
        this.tickrate = (tickrate <= 0) ? 60 : tickrate;
        this.grid = new GridMap(MAP_W, MAP_H, seed);
        spawnInitialEnemies(seed);
        try {
            loadStateIfPresent();
        } catch (Exception ignored) {
        }
    }

    // ----- Public API -----

    /** Adds a client, performs HELLO/READY handshake, starts its reader thread. */
    int addClient(Socket socket, String name, String version) throws IOException {
        socket.setTcpNoDelay(true);

        int id = nextId.getAndIncrement();
        String safeName = (name == null || name.isBlank()) ? ("P" + id) : sanitize(name);

        // Spawn on a floor tile not too close to center (deterministic per id)
        Random r = new Random(seed ^ (id * 0x9E3779B97F4A7C15L));
        int[] t = grid.randomFloorTileFarFrom(
            MAP_W * Constants.TILE_SIZE / 2.0,
            MAP_H * Constants.TILE_SIZE / 2.0,
            200, r);
        double x = t[0] * Constants.TILE_SIZE + Constants.TILE_SIZE / 2.0;
        double y = t[1] * Constants.TILE_SIZE + Constants.TILE_SIZE / 2.0;

        PlayerState ps = players.computeIfAbsent(id, k -> new PlayerState(k, safeName, x, y));
        ps.version = version;

        ClientConn conn = new ClientConn(id, socket);
        conns.add(conn);

        // Handshake
        conn.send("HELLO proto=1");
        conn.send("SEED " + seed);
        conn.send("TICKRATE " + tickrate);

        // Send authoritative MAP before READY so client draws & collides against the same grid
        sendMap(conn);

        conn.send("YOU id=" + id);
        conn.send("READY");
        conn.send("WELCOME " + Protocol.urlEnc(safeName));

        // Reader
        conn.startReader(line -> onClientLine(conn, line));

        System.out.printf("[Session] Client #%d '%s' joined (%s)%n",
            id, safeName, socket.getRemoteSocketAddress());
        return id;
    }


    /** Graceful stop and autosave. */
    public void shutdown() {
        running = false;
        for (ClientConn c : conns) c.close();
        try {
            saveState();
        } catch (Exception ignored) {
        }
    }

    // ----- Main loop -----

    @Override
    public void run() {
        final long stepNs = 1_000_000_000L / tickrate;
        final int broadcastDiv = Math.max(1, tickrate / 30); // ~30 Hz snapshots (maintained for network efficiency)
        long last = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            if (now - last < stepNs) {
                Thread.onSpinWait();
                continue;
            }
            last += stepNs;

            // 1) inputs -> pending
            List<ClientPacket> batch = new ArrayList<>(128);
            inputQ.drainTo(batch);
            for (ClientPacket cp : batch) {
                PlayerState ps = players.get(cp.playerId);
                if (ps != null && ps.alive) ps.pending = cp.msg;
            }

            // 2) simulate
            updatePlayers();
            updateEnemies();

            // 3) broadcast snapshots at ~30 Hz (maintained for network efficiency)
            if ((tick % broadcastDiv) == 0) {
                broadcastSnapshot();
            }

            // 4) autosave every 10s
            if ((tick % (tickrate * 10)) == 0) {
                try {
                    saveState();
                } catch (Exception ignored) {
                }
            }

            tick++;
        }
    }

    // ----- Per-tick: players -----

    private void updatePlayers() {
        final double dt = 1.0 / tickrate;

        for (PlayerState ps : players.values()) {
            if (!ps.alive) continue;  // <- do not update dead players

            Input ci = ps.pending;
            ps.pending = null;

            int dx = 0, dy = 0, facing = ps.facing;
            boolean sprint = false, attack = false;

            if (ci != null) {
                dx = clamp(ci.dx, -1, 1);
                dy = clamp(ci.dy, -1, 1);
                sprint = ci.sprint;
                attack = ci.attack;
                if (ci.facing >= 0) facing = ci.facing;
            }

            // normalize diagonal
            double vx = dx, vy = dy;
            if (vx != 0 && vy != 0) {
                double inv = 1.0 / Math.sqrt(2.0);
                vx *= inv;
                vy *= inv;
            }

            double speed = ps.speedPps;
            if (sprint && ps.stamina >= 1.0) {
                ps.stamina = Math.max(0.0, ps.stamina - (1.0 * dt)); // cost while sprinting
                speed *= 2.0;
                ps.sprinting = true;
            } else {
                ps.sprinting = false;
                ps.stamina = Math.min(ps.maxStamina, ps.stamina + (0.5 * dt)); // regen
            }

            // move with collisions
            ps.x = moveAxis(ps.x, ps.y, vx * speed * dt, true, PLAYER_HALF);
            ps.y = moveAxis(ps.x, ps.y, vy * speed * dt, false, PLAYER_HALF);
            ps.facing = facing;

            // timers
            if (ps.attackCooldown > 0) ps.attackCooldown--;
            if (ps.attackTimer > 0) ps.attackTimer--;

            // swing is active -> keep checking hits
            if (ps.attackTimer > 0) {
                applySwordHits(ps);
            }

            // start a new swing if allowed
            if (attack && ps.attackCooldown == 0 && ps.attackTimer == 0 && ps.stamina > 0.0) {
                ps.stamina -= 0.5;
                ps.attackTimer = SWORD_DURATION_TICKS;
                ps.attackCooldown = SWORD_COOLDOWN_TICKS;
                ps.swingSeq++;
                applySwordHits(ps);
            }

            // final clamp to avoid drift
            if (ps.stamina < 0.0) ps.stamina = 0.0;
            else if (ps.stamina > ps.maxStamina) ps.stamina = ps.maxStamina;
        }
    }

    private void applySwordHits(PlayerState ps) {
        // forward unit from 8-way facing
        double ang = Dir8.octantToAngle(ps.facing);
        double ux = Math.cos(ang), uy = Math.sin(ang);

        // segment from player center to forward reach
        double x0 = ps.x, y0 = ps.y;
        double x1 = ps.x + ux * SWORD_REACH;
        double y1 = ps.y + uy * SWORD_REACH;

        // capsule radius (blade thickness + enemy radius)
        double r = (SWORD_WIDTH * 0.5) + ENEMY_HALF;
        double r2 = r * r;

        long swingTag = (((long) ps.id) << 32) | (ps.swingSeq & 0xFFFFFFFFL);

        for (EnemyState e : enemies) {
            if (!e.alive) continue;
            if (e.lastSwingTag == swingTag) continue;

            double vx = x1 - x0, vy = y1 - y0;
            double wx = e.x - x0, wy = e.y - y0;
            double vv = vx * vx + vy * vy;
            double t = (vv <= 1e-9) ? 0.0 : (wx * vx + wy * vy) / vv;
            if (t < 0.0) t = 0.0;
            else if (t > 1.0) t = 1.0;
            double cx = x0 + t * vx, cy = y0 + t * vy;
            double dx = e.x - cx, dy = e.y - cy;
            double d2 = dx * dx + dy * dy;

            if (d2 <= r2) {
                e.lastSwingTag = swingTag;
                e.hp -= SWORD_DMG;
                if (e.hp <= 0) {
                    e.hp = 0;
                    e.alive = false;
                }

                // small knockback along the swing direction
                double kb = 4.0, nx = e.x + ux * kb, ny = e.y + uy * kb;
                if (!grid.collidesBox(nx, e.y, ENEMY_HALF)) e.x = nx;
                if (!grid.collidesBox(e.x, ny, ENEMY_HALF)) e.y = ny;
            }
        }
    }

    // ----- Per-tick: enemies -----

    private void updateEnemies() {
        final double dt = 1.0 / tickrate;

        for (EnemyState e : enemies) {
            if (!e.alive) continue;

            // find closest living player
            PlayerState target = null;
            double best = Double.POSITIVE_INFINITY;
            for (PlayerState ps : players.values()) {
                if (!ps.alive) continue;
                double d = Math.hypot(ps.x - e.x, ps.y - e.y);
                if (d < best) {
                    best = d;
                    target = ps;
                }
            }
            if (target == null) continue;

            if (best > e.aggroRadius) {
                // --- wander when far (match SP feel) ---
                if (--e.wanderTimer <= 0) pickNewWanderDir(e);
                double vx = e.wanderDx * (0.6 * ENEMY_SPEED);
                double vy = e.wanderDy * (0.6 * ENEMY_SPEED);
                e.x = moveAxis(e.x, e.y, vx * dt, true, ENEMY_HALF);
                e.y = moveAxis(e.x, e.y, vy * dt, false, ENEMY_HALF);
                continue;
            }

            // --- engaged: melee or chase ---
            if (best <= ENEMY_MELEE_RANGE) {
                target.hp = Math.max(0.0, target.hp - ENEMY_DMG_PER_SEC * dt);
                if (target.hp == 0.0) target.alive = false;
                continue;
            }

            double dx = target.x - e.x, dy = target.y - e.y, len = Math.hypot(dx, dy);
            if (len > 1e-6) {
                dx /= len;
                dy /= len;
            }
            e.x = moveAxis(e.x, e.y, dx * ENEMY_SPEED * dt, true, ENEMY_HALF);
            e.y = moveAxis(e.x, e.y, dy * ENEMY_SPEED * dt, false, ENEMY_HALF);
        }
    }

    // ----- Net out -----

    private void sendMap(ClientConn conn) {
        try (StringWriter sw = new StringWriter()) {
            Protocol.writeMap(MAP_W, MAP_H, grid::isWallTile, sw);
            conn.send(sw.toString().trim());
        } catch (IOException ignored) {
        }
    }

    private void broadcastSnapshot() {
        Snapshot snap = new Snapshot();
        snap.tick = tick;

        for (PlayerState ps : players.values()) {
            PlayerSnap p = new PlayerSnap();
            p.id = ps.id;
            p.x = ps.x;
            p.y = ps.y;
            p.hp = ps.hp;
            p.st = ps.stamina;
            p.sh = ps.shield;
            p.facing = ps.facing;
            p.alive = ps.alive;
            p.name = ps.name;
            snap.players.put(p.id, p);
        }

        for (EnemyState e : enemies) {
            EnemySnap es = new EnemySnap();
            es.id = e.id;
            es.x = e.x;
            es.y = e.y;
            es.hp = e.hp;
            es.alive = e.alive;
            snap.enemies.put(es.id, es);
        }

        for (ClientConn c : conns) {
            try (StringWriter sw = new StringWriter()) {
                Protocol.writeSnapshot(snap, sw);
                c.send(sw.toString().trim());
            } catch (IOException ignored) {
            }
        }
    }

    // ----- Helpers -----

    private void pickNewWanderDir(EnemyState e) {
        e.lcg = lcgNext(e.lcg);
        int span = 15 + (int) Math.floor(lcg01(e.lcg) * 21.0); // 15..35 ticks (~0.25–0.6s @60Hz feel)
        e.wanderTimer = span;

        e.lcg = lcgNext(e.lcg);
        double ang = lcg01(e.lcg) * Math.PI * 2.0;
        e.wanderDx = Math.cos(ang);
        e.wanderDy = Math.sin(ang);
    }

    /** Move one axis with tile collision; returns new coordinate for that axis. */
    private double moveAxis(double x, double y, double delta, boolean xAxis, int half) {
        if (delta == 0) return xAxis ? x : y;

        double next = (xAxis ? x : y) + delta;
        double tx = xAxis ? next : x;
        double ty = xAxis ? y : next;

        if (!grid.collidesBox(tx, ty, half)) return next;

        int step = (int) Math.signum(delta);
        while (step != 0) {
            double test = (xAxis ? x : y) + step;
            tx = xAxis ? test : x;
            ty = xAxis ? y : test;
            if (!grid.collidesBox(tx, ty, half)) {
                if (xAxis) x = test;
                else y = test;
            } else break;
        }
        return xAxis ? x : y;
    }

    private void spawnInitialEnemies(long seed) {
        Random r = new Random(seed ^ 0xBADC0FFEE123L);
        for (int i = 0; i < 16; i++) {
            int[] t = grid.randomFloorTileFarFrom(
                MAP_W * Constants.TILE_SIZE / 2.0,
                MAP_H * Constants.TILE_SIZE / 2.0,
                250, r);
            EnemyState e = new EnemyState();
            e.id = i + 1;
            e.x = t[0] * Constants.TILE_SIZE + Constants.TILE_SIZE / 2.0;
            e.y = t[1] * Constants.TILE_SIZE + Constants.TILE_SIZE / 2.0;
            e.hp = 1.0;           // <<< match Single Player
            e.alive = true;
            // personalize aggro + wander (used in section C)
            e.aggroRadius = 220 + 140 * r.nextDouble(); // 220..360 px like SP
            // per-enemy tiny RNG seed (LCG); deterministic
            e.lcg = (int) ((Double.doubleToLongBits(e.x) * 31 + Double.doubleToLongBits(e.y)) ^ 0x9E3779B9);
            if (e.lcg == 0) e.lcg = 1;
            pickNewWanderDir(e); // initialize wander
            enemies.add(e);
        }
    }


    private void onClientLine(ClientConn conn, String line) {
        if (line == null) {
            conns.remove(conn);
            conn.close();
            System.out.printf("[Session] Client #%d disconnected%n", conn.playerId);
            return;
        }
        Protocol.Input msg = Protocol.parseInputLine(line);
        if (msg != null) {
            inputQ.offer(new ClientPacket(conn.playerId, msg));
            return;
        }
        System.out.println("[Session] <-#" + conn.playerId + " " + line);
    }

    // ----- Persistence (lightweight JSON, seed-gated) -----

    private void saveState() throws IOException {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"seed\":").append(seed).append(",\"tick\":").append(tick).append(",\"players\":[");
        boolean first = true;
        for (PlayerState ps : players.values()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{")
                .append("\"id\":").append(ps.id).append(',')
                .append("\"name\":").append(json(ps.name)).append(',')
                .append("\"x\":").append(fmt(ps.x)).append(',')
                .append("\"y\":").append(fmt(ps.y)).append(',')
                .append("\"hp\":").append(fmt(ps.hp)).append(',')
                .append("\"stamina\":").append(fmt(ps.stamina)).append(',')
                .append("\"shield\":").append(fmt(ps.shield)).append(',')
                .append("\"facing\":").append(ps.facing).append(',')
                .append("\"alive\":").append(ps.alive)
                .append("}");
        }
        sb.append("]}");

        Files.writeString(SAVE_FILE, sb.toString(),
            StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void loadStateIfPresent() throws IOException {
        if (!Files.exists(SAVE_FILE)) return;
        String json = Files.readString(SAVE_FILE, StandardCharsets.UTF_8);
        if (!json.contains("\"seed\":" + seed)) return; // ignore other sessions

        for (String obj : json.split("\\{")) {
            if (!obj.contains("\"id\":")) continue;
            int id = parseInt(extract(obj, "\"id\":(\\d+)"), -1);
            if (id <= 0) continue;

            String name = unjson(extract(obj, "\"name\":(\".*?\")"));
            double x = parseDouble(extract(obj, "\"x\":([0-9eE+\\-\\.]+)"), 100);
            double y = parseDouble(extract(obj, "\"y\":([0-9eE+\\-\\.]+)"), 100);
            double hp = parseDouble(extract(obj, "\"hp\":([0-9eE+\\-\\.]+)"), 6);
            double st = parseDouble(extract(obj, "\"stamina\":([0-9eE+\\-\\.]+)"), 6);
            double sh = parseDouble(extract(obj, "\"shield\":([0-9eE+\\-\\.]+)"), 0);
            int facing = parseInt(extract(obj, "\"facing\":(\\d+)"), 0);
            boolean alive = "true".equalsIgnoreCase(extract(obj, "\"alive\":(true|false)"));

            PlayerState ps = new PlayerState(id, (name == null ? ("P" + id) : name), x, y);
            ps.hp = hp;
            ps.stamina = st;
            ps.shield = sh;
            ps.facing = facing;
            ps.alive = alive;
            players.put(id, ps);
            nextId.updateAndGet(n -> Math.max(n, id + 1));
        }
        System.out.printf("[Session] Loaded %d players from save%n", players.size());
    }

    // ----- Connection wrapper -----

    private static final class ClientConn {
        final int playerId;
        final Socket socket;
        final BufferedReader in;
        final BufferedWriter out;
        final Thread reader;
        volatile boolean open = true;
        private Consumer<String> onLine;

        ClientConn(int playerId, Socket socket) throws IOException {
            this.playerId = playerId;
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.reader = new Thread(this::pump, "ClientReader-" + playerId);
            this.reader.setDaemon(true);
        }

        void startReader(Consumer<String> onLine) {
            this.onLine = onLine;
            reader.start();
        }

        private void pump() {
            try (socket; in; out) {
                String line;
                while (open && (line = in.readLine()) != null) {
                    if (onLine != null) onLine.accept(line);
                }
            } catch (IOException ignored) {
            } finally {
                open = false;
            }
        }

        void send(String s) {
            if (!open) return;
            try {
                out.write(s);
                out.write("\n");
                out.flush();
            } catch (IOException e) {
                open = false;
            }
        }

        void close() {
            open = false;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ----- Types -----

    private record ClientPacket(int playerId, Input msg) {
    }

    private static final class PlayerState {
        final int id;
        final String name;
        String version;
        double x, y;
        double hp = 6.0, stamina = 6.0, shield = 0.0, maxStamina = 6.0, speedPps = 90.0;
        boolean alive = true, sprinting = false;
        int facing = 0, attackTimer = 0, attackCooldown = 0;
        int swingSeq = 0;
        transient Input pending;

        PlayerState(int id, String name, double x, double y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    private static final class EnemyState {
        int id;
        double x, y, hp;
        boolean alive = true;
        long lastSwingTag = 0L;
        double aggroRadius;
        int wanderTimer;
        double wanderDx, wanderDy;
        int lcg;
    }

    // ----- Small utils -----

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.3f", d);
    }

    private static String sanitize(String s) {
        return s.replaceAll("[\\r\\n\\t]", "_");
    }

    private static String json(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String extract(String text, String regex) {
        var m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String unjson(String quoted) {
        if (quoted == null) return null;
        if (quoted.length() >= 2 && quoted.startsWith("\"") && quoted.endsWith("\"")) {
            String body = quoted.substring(1, quoted.length() - 1);
            return body.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return quoted;
    }

    /** True if the given tile (tx,ty) is a wall, inferred via collision at tile center. */
    private boolean isWallTile(int tx, int ty) {
        final int ts = Constants.TILE_SIZE;
        final double cx = tx * ts + ts / 2.0;
        final double cy = ty * ts + ts / 2.0;
        // probe with a box that fits inside the tile: "half" ~ half tile minus 1px
        final int half = (ts / 2) - 1;
        return grid.collidesBox(cx, cy, half);
    }

}
