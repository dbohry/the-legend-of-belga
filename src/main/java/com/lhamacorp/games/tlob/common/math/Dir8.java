package com.lhamacorp.games.tlob.common.math;

public final class Dir8 {
    private Dir8() {
    }

    /** angle (rad) -> octant [0..7] */
    public static int angleToOctant(double ang) {
        int o = (int) Math.round(ang / (Math.PI / 4.0));
        if (o < 0) o += 8;
        return o & 7;
    }

    /** octant [0..7] -> angle (rad) */
    public static double octantToAngle(int octant) {
        return (octant & 7) * (Math.PI / 4.0);
    }
}
