package com.lhamacorp.games.tlob;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class KeyManager implements KeyListener, MouseListener {

    public volatile boolean up;
    public volatile boolean down;
    public volatile boolean left;
    public volatile boolean right;
    public volatile boolean attack;
    public volatile boolean enter;
    public volatile boolean escape;
    public volatile boolean shift;
    public volatile boolean mute = false;

    private volatile boolean attackKey;
    private volatile boolean attackMouse;

    private void updateAttack() {
        attack = attackKey || attackMouse;
    }

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
                attackKey = true; updateAttack(); break;
            case KeyEvent.VK_ENTER:
                enter = true; break;
            case KeyEvent.VK_ESCAPE:
                escape = true; break;
            case KeyEvent.VK_SHIFT:
                shift = true; break;
            case KeyEvent.VK_M:
                mute = !mute; break;
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
                attackKey = false; updateAttack(); break;
            case KeyEvent.VK_ENTER:
                enter = false; break;
            case KeyEvent.VK_ESCAPE:
                escape = false; break;
            case KeyEvent.VK_SHIFT:
                shift = false; break;
            case KeyEvent.VK_M:
                mute = !mute; break;
            default:
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            attackMouse = true;
            updateAttack();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            attackMouse = false;
            updateAttack();
        }
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
}
