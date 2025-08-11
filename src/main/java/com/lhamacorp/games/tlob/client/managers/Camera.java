package com.lhamacorp.games.tlob.client.managers;

public final class Camera {
    private int x, y;

    public void follow(double targetX, double targetY, int mapWpx, int mapHpx, int screenW, int screenH) {
        int tx = (int) Math.round(targetX - screenW / 2.0);
        int ty = (int) Math.round(targetY - screenH / 2.0);
        x = clamp(tx, 0, Math.max(0, mapWpx - screenW));
        y = clamp(ty, 0, Math.max(0, mapHpx - screenH));
    }

    public int offsetX() {
        return x;
    }

    public int offsetY() {
        return y;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}