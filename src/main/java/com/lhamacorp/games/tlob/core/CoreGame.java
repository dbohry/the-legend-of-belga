package com.lhamacorp.games.tlob.core;

import com.lhamacorp.games.tlob.server.GameSession;

import java.util.Random;

/**
 * Headless game runner (no UI). Starts the authoritative simulation loop
 * without accepting any network clients. Useful for testing core logic.
 */
public final class CoreGame {

    public static void main(String[] args) {
        long seed = (args.length >= 1) ? parseLong(args[0], pickSeed()) : pickSeed();
        int tickrate = (args.length >= 2) ? parseInt(args[1], 60) : 60;

        System.out.println("[CoreGame] starting headless simulation: seed=" + seed + ", tickrate=" + tickrate);
        GameSession session = new GameSession(seed, tickrate);
        Thread sim = new Thread(session, "GameSession");
        sim.setDaemon(true);
        sim.start();

        Runtime.getRuntime().addShutdownHook(new Thread(session::shutdown, "CoreGameShutdown"));

        // Keep the main thread alive until interrupted
        try {
            // Sleep indefinitely; simulation runs on its own thread
            // If you want to run for a limited time, adjust this logic.
            while (true) Thread.sleep(1_000_000L);
        } catch (InterruptedException ignored) {
        }
    }

    private static long pickSeed() {
        long t = System.currentTimeMillis();
        long r = new Random().nextLong();
        return (t ^ (r * 0x9E3779B97F4A7C15L));
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }
}
