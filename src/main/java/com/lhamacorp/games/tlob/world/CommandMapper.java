package com.lhamacorp.games.tlob.world;

import com.lhamacorp.games.tlob.managers.KeyManager;

import java.util.ArrayList;
import java.util.List;

/** Translate current keyboard state into a single-tick command list. */
public final class CommandMapper {
    private CommandMapper() {
    }

    public static List<InputCommand> fromKeys(KeyManager k) {
        int dx = 0, dy = 0;
        if (k.left) dx--;
        if (k.right) dx++;
        if (k.up) dy--;
        if (k.down) dy++;
        boolean sprint = k.shift;
        boolean attack = k.attack;

        List<InputCommand> out = new ArrayList<>(2);
        if (dx != 0 || dy != 0 || sprint) out.add(new MoveCmd(dx, dy, sprint));
        if (attack) out.add(new AttackCmd());
        return out;
    }
}
