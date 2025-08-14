package com.lhamacorp.games.tlob.client.world;

import java.util.List;

/** Collapses a tick’s commands into the booleans Player expects. */
final class VirtualInput implements PlayerInputView {
    private final int dx, dy;
    private final boolean sprint, attack;

    private VirtualInput(int dx, int dy, boolean sprint, boolean attack) {
        this.dx = Integer.signum(dx);
        this.dy = Integer.signum(dy);
        this.sprint = sprint;
        this.attack = attack;
    }

    static VirtualInput from(List<InputCommand> cmds) {
        int dx = 0, dy = 0;
        boolean sprint = false, attack = false;
        if (cmds != null) for (InputCommand c : cmds) {
            if (c instanceof MoveCmd m) {
                dx += m.dx();
                dy += m.dy();
                sprint |= m.sprint();
            } else if (c instanceof AttackCmd) attack = true;
        }
        // clamp to {-1,0,1}
        dx = Math.max(-1, Math.min(1, dx));
        dy = Math.max(-1, Math.min(1, dy));
        return new VirtualInput(dx, dy, sprint, attack);
    }

    @Override
    public boolean left() {
        return dx < 0;
    }

    @Override
    public boolean right() {
        return dx > 0;
    }

    @Override
    public boolean up() {
        return dy < 0;
    }

    @Override
    public boolean down() {
        return dy > 0;
    }

    @Override
    public boolean sprint() {
        return sprint;
    }

    @Override
    public boolean attack() {
        return attack;
    }

    @Override
    public boolean defense() {
        return false; // Virtual input doesn't support blocking
    }
    
    @Override
    public boolean dash() {
        return false; // Virtual input doesn't support dashing
    }
}
