package com.lhamacorp.games.tlob.common.net;

import java.io.BufferedReader;
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
                        case "name" -> ps.name = urlDec(v);
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
