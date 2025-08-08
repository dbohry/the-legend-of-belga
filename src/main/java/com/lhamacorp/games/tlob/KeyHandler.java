package com.lhamacorp.games.tlob;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public volatile boolean up;
    public volatile boolean down;
    public volatile boolean left;
    public volatile boolean right;
    public volatile boolean attack;
    public volatile boolean enter;
    public volatile boolean escape;
    public volatile boolean shift;

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                up = true; break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                down = true; break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                left = true; break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                right = true; break;
            case KeyEvent.VK_SPACE:
                attack = true; break;
            case KeyEvent.VK_ENTER:
                enter = true; break;
            case KeyEvent.VK_ESCAPE:
                escape = true; break;
            case KeyEvent.VK_SHIFT:
                shift = true; break;
            default:
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                up = false; break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                down = false; break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                left = false; break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                right = false; break;
            case KeyEvent.VK_SPACE:
                attack = false; break;
            case KeyEvent.VK_ENTER:
                enter = false; break;
            case KeyEvent.VK_ESCAPE:
                escape = false; break;
            case KeyEvent.VK_SHIFT:
                shift = false; break;
            default:
        }
    }
}
