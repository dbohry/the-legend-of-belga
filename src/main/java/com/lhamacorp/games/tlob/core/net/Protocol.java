package com.lhamacorp.games.tlob.core.net;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Protocol {

    private Protocol() {
    }

    // ---------- Messages ----------
    public static final class Input {
        public int tick, dx, dy, facing = -1;
        public boolean sprint, attack;
    }

    public static final class PlayerSnap {
        public int id, facing;
        public double x, y, hp, st, sh;
        public boolean alive;
        public String name;
    }

    public static final class EnemySnap {
        public int id;
        public double x, y, hp;
        public boolean alive;
    }

    public static final class Snapshot {
        public int tick;
        public Map<Integer, PlayerSnap> players = new HashMap<>();
        public Map<Integer, EnemySnap> enemies = new HashMap<>();
    }

    // ---------- Parsing ----------

    /** Parses "INPUT ..." line. Returns null if malformed. */
    public static Input parseInputLine(String line) {
        if (line == null) return null;
        line = line.trim();
        if (!line.startsWith("INPUT")) return null;

        Input ci = new Input();
        String[] parts = line.substring("INPUT".length()).trim().split("\\s+");
        for (String p : parts) {
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String k = p.substring(0, eq);
            String v = p.substring(eq + 1);
            try {
                switch (k) {
                    case "t" -> ci.tick = parseInt(v, 0);
                    case "dx" -> ci.dx = parseInt(v, 0);
                    case "dy" -> ci.dy = parseInt(v, 0);
                    case "sprint" -> ci.sprint = parseInt(v, 0) != 0;
                    case "attack" -> ci.attack = parseInt(v, 0) != 0;
                    case "facing" -> ci.facing = parseInt(v, -1);
                }
            } catch (Exception ignored) {
            }
        }
        return ci;
    }

    public static Snapshot readSnapshot(java.io.Reader r) throws IOException {
        java.io.BufferedReader br = (r instanceof java.io.BufferedReader)
            ? (java.io.BufferedReader) r
            : new java.io.BufferedReader(r);
        String first = br.readLine();
        return readSnapshot(br, first);
    }

    /** Reads a full SNAPSHOT block (starting at "SNAPSHOT..." line) from a reader. */
    public static Snapshot readSnapshot(BufferedReader in, String firstLine) throws IOException {
        if (firstLine == null) return null;
        String head = firstLine.trim();
        if (!head.startsWith("SNAPSHOT")) return null;

        Snapshot s = new Snapshot();
        for (String token : head.split("\\s+")) {
            if (token.startsWith("tick=")) {
                s.tick = parseInt(token.substring(5), 0);
            }
        }

        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.equals("END")) break;
            if (line.isEmpty()) continue;

            if (line.startsWith("P ")) {
                PlayerSnap ps = new PlayerSnap();
                parseKeyVals(line.substring(2), (k, v) -> {
                    switch (k) {
                        case "id" -> ps.id = parseInt(v, 0);
                        case "x" -> ps.x = parseDouble(v, 0);
                        case "y" -> ps.y = parseDouble(v, 0);
                        case "hp" -> ps.hp = parseDouble(v, 0);
                        case "st" -> ps.st = parseDouble(v, 0);
                        case "sh" -> ps.sh = parseDouble(v, 0);
                        case "facing" -> ps.facing = parseInt(v, 0);
                        case "alive" -> ps.alive = !"0".equals(v);
                        case "name" -> ps.name = urlEnc(v);
                    }
                });
                if (ps.id > 0) s.players.put(ps.id, ps);
            } else if (line.startsWith("E ")) {
                EnemySnap es = new EnemySnap();
                parseKeyVals(line.substring(2), (k, v) -> {
                    switch (k) {
                        case "id" -> es.id = parseInt(v, 0);
                        case "x" -> es.x = parseDouble(v, 0);
                        case "y" -> es.y = parseDouble(v, 0);
                        case "hp" -> es.hp = parseDouble(v, 0);
                        case "alive" -> es.alive = !"0".equals(v);
                    }
                });
                if (es.id > 0) s.enemies.put(es.id, es);
            }
        }
        return s;
    }

    // ---------- Serialization ----------
    public static void writeSnapshot(Snapshot s, Writer out) throws IOException {
        out.write("SNAPSHOT tick=" + s.tick + " players=" + s.players.size());
        out.write("\n");
        for (PlayerSnap ps : s.players.values()) {
            out.write("P id=" + ps.id +
                " x=" + fmt(ps.x) + " y=" + fmt(ps.y) +
                " hp=" + fmt(ps.hp) + " st=" + fmt(ps.st) + " sh=" + fmt(ps.sh) +
                " facing=" + ps.facing +
                " alive=" + (ps.alive ? 1 : 0) +
                " name=" + urlEnc(ps.name) + "\n");
        }
        for (EnemySnap es : s.enemies.values()) {
            out.write("E id=" + es.id +
                " x=" + fmt(es.x) + " y=" + fmt(es.y) +
                " hp=" + fmt(es.hp) +
                " alive=" + (es.alive ? 1 : 0) + "\n");
        }
        out.write("END\n");
        out.flush();
    }

    // ---------- Map transfer ----------

    public static final class MapData {
        public int w, h;
        public boolean[][] walls; // [h][w], true = wall
    }

    /** Server -> Client: write the collision map as plain text rows of '#' (wall) and '.' (floor). */
    public static void writeMap(int w, int h, java.util.function.BiPredicate<Integer, Integer> isWall, Writer out) throws IOException {
        out.write("MAP w=" + w + " h=" + h + "\n");
        for (int y = 0; y < h; y++) {
            StringBuilder row = new StringBuilder(w);
            for (int x = 0; x < w; x++) {
                row.append(isWall.test(x, y) ? '#' : '.');
            }
            out.write(row.toString());
            out.write("\n");
        }
        out.write("ENDMAP\n");
        out.flush();
    }

    /** Client: read a MAP block that starts with the given firstLine ("MAP ...") and ends with "ENDMAP". */
    public static MapData readMap(BufferedReader in, String firstLine) throws IOException {
        if (firstLine == null) return null;
        String head = firstLine.trim();
        if (!head.startsWith("MAP")) return null;

        int w = 0, h = 0;
        for (String tok : head.split("\\s+")) {
            if (tok.startsWith("w=")) w = parseInt(tok.substring(2), 0);
            if (tok.startsWith("h=")) h = parseInt(tok.substring(2), 0);
        }
        if (w <= 0 || h <= 0) throw new IOException("Invalid MAP header: " + head);

        boolean[][] walls = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            String row = in.readLine();
            if (row == null) throw new EOFException("Unexpected EOF in MAP at row " + y);
            if (row.length() < w) throw new IOException("MAP row too short at y=" + y);
            for (int x = 0; x < w; x++) {
                char c = row.charAt(x);
                walls[y][x] = (c == '#');
            }
        }
        // Read and validate ENDMAP (consume extra lines until we see it, for safety)
        String end = in.readLine();
        while (end != null && !end.trim().equals("ENDMAP")) end = in.readLine();
        if (end == null) throw new EOFException("Missing ENDMAP");

        MapData md = new MapData();
        md.w = w; md.h = h; md.walls = walls;
        return md;
    }


    // ---------- utils ----------
    private interface KV {
        void put(String k, String v);
    }

    private static void parseKeyVals(String s, KV kv) {
        for (String p : s.trim().split("\\s+")) {
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            kv.put(p.substring(0, eq), p.substring(eq + 1));
        }
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

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.3f", d);
    }

    public static String urlEnc(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    public static String urlDec(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
