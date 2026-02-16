package com.lhamacorp.games.tlob.client.managers;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class KeyManager implements KeyListener, MouseListener {

    // Key constants for better maintainability
    private static final int KEY_W = KeyEvent.VK_W;
    private static final int KEY_A = KeyEvent.VK_A;
    private static final int KEY_S = KeyEvent.VK_S;
    private static final int KEY_D = KeyEvent.VK_D;
    private static final int KEY_UP = KeyEvent.VK_UP;
    private static final int KEY_DOWN = KeyEvent.VK_DOWN;
    private static final int KEY_LEFT = KeyEvent.VK_LEFT;
    private static final int KEY_RIGHT = KeyEvent.VK_RIGHT;
    private static final int KEY_SPACE = KeyEvent.VK_SPACE;
    private static final int KEY_ENTER = KeyEvent.VK_ENTER;
    private static final int KEY_ESCAPE = KeyEvent.VK_ESCAPE;
    private static final int KEY_SHIFT = KeyEvent.VK_SHIFT;
    private static final int KEY_CTRL = KeyEvent.VK_CONTROL;
    private static final int KEY_ALT = KeyEvent.VK_ALT;
    private static final int KEY_M = KeyEvent.VK_M;
    private static final int KEY_I = KeyEvent.VK_I;
    private static final int KEY_1 = KeyEvent.VK_1;
    private static final int KEY_2 = KeyEvent.VK_2;

    public volatile boolean up;
    public volatile boolean down;
    public volatile boolean left;
    public volatile boolean right;
    public volatile boolean attack;
    public volatile boolean defense;
    public volatile boolean enter;
    public volatile boolean escape;
    public volatile boolean shift;
    public volatile boolean dash;
    public volatile boolean mute = false;
    public volatile boolean i;
    public volatile boolean weaponSwitch1;
    public volatile boolean weaponSwitch2;

    private volatile boolean attackKey;
    private volatile boolean attackMouse;
    private volatile boolean defenseKey;
    private volatile boolean defenseMouse;

    private volatile boolean escapePressedOnce;
    private volatile boolean enterPressedOnce;
    private volatile boolean mDown;

    private void updateAttack() {
        attack = attackKey || attackMouse;
    }

    private void updateDefense() {
        defense = defenseKey || defenseMouse;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KEY_W:
            case KEY_UP:
                up = true;
                break;
            case KEY_S:
            case KEY_DOWN:
                down = true;
                break;
            case KEY_A:
            case KEY_LEFT:
                left = true;
                break;
            case KEY_D:
            case KEY_RIGHT:
                right = true;
                break;
            case KEY_SPACE:
                attackKey = true;
                updateAttack();
                break;
            case KEY_ENTER:
                if (!enter) enterPressedOnce = true;
                enter = true;
                break;
            case KEY_ESCAPE:
                if (!escape) escapePressedOnce = true;
                escape = true;
                break;
            case KEY_SHIFT:
                shift = true;
                break;
            case KEY_ALT:
                defenseKey = true;
                updateDefense();
                break;
            case KEY_CTRL:
                dash = true;
                break;
            case KEY_M:
                if (!mDown) {
                    mute = !mute;
                    mDown = true;
                }
                break;
            case KEY_I:
                i = true;
                break;
            case KEY_1:
                weaponSwitch1 = true;
                break;
            case KEY_2:
                weaponSwitch2 = true;
                break;
            default:
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KEY_W:
            case KEY_UP:
                up = false;
                break;
            case KEY_S:
            case KEY_DOWN:
                down = false;
                break;
            case KEY_A:
            case KEY_LEFT:
                left = false;
                break;
            case KEY_D:
            case KEY_RIGHT:
                right = false;
                break;
            case KEY_SPACE:
                attackKey = false;
                updateAttack();
                break;
            case KEY_ENTER:
                enter = false;
                break;
            case KEY_ESCAPE:
                escape = false;
                break;
            case KEY_SHIFT:
                shift = false;
                break;
            case KEY_ALT:
                defenseKey = false;
                updateDefense();
                break;
            case KEY_CTRL:
                dash = false;
                break;
            case KEY_M:
                mDown = false;
                break;
            case KEY_I:
                i = false;
                break;
            case KEY_1:
                weaponSwitch1 = false;
                break;
            case KEY_2:
                weaponSwitch2 = false;
                break;
            default:
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            attackMouse = true;
            updateAttack();
        }

        if (e.getButton() == MouseEvent.BUTTON3) {
            defenseMouse = true;
            updateDefense();
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            attackMouse = false;
            updateAttack();
        }

        if (e.getButton() == MouseEvent.BUTTON3) {
            defenseMouse = false;
            updateDefense();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
