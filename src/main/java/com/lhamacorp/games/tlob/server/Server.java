package com.lhamacorp.games.tlob.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Thin acceptor that delegates all protocol/IO to GameSession.
 *
 * GameSession:
 *  - sends HELLO/SEED/TICKRATE/YOU/READY/WELCOME
 *  - starts a per-connection reader thread
 *  - parses INPUT lines, simulates, and broadcasts SNAPSHOTs
 */
public class Server {

    private static GameSession SESSION;

    public static void main(String[] args) throws IOException {
        int port = (args.length >= 1) ? parseInt(args[0], 7777) : 7777;
        Long forcedSeed = (args.length >= 2) ? parseLong(args[1], null) : null;

        long seed = (forcedSeed != null) ? forcedSeed : pickSeed();
        int tickrate = 60;

        System.out.println("[SeedServer] Listening on port " + port + ", seed=" + seed + ", tickrate=" + tickrate);

        // Start the authoritative session loop
        SESSION = new GameSession(seed, tickrate);
        Thread sim = new Thread(SESSION, "GameSession");
        sim.setDaemon(true);
        sim.start();

        // Optional: clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(SESSION::shutdown, "SessionShutdown"));

        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("0.0.0.0", port));

            while (true) {
                Socket s = server.accept();
                System.out.println("[SeedServer] Client connected from " + s.getRemoteSocketAddress());
                try {
                    int id = SESSION.addClient(s, /*name*/ null, /*version*/ null);
                    System.out.println("[SeedServer] Client assigned id=" + id);
                } catch (IOException e) {
                    System.out.println("[SeedServer] Failed to add client: " + e.getMessage());
                    try {
                        s.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    // --- utils ---

    private static long pickSeed() {
        long t = System.currentTimeMillis();
        long r = new Random().nextLong();
        return (t ^ (r * 0x9E3779B97F4A7C15L));
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static Long parseLong(String s, Long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
