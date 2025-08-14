package com.lhamacorp.games.tlob.client.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

class KeyManagerTest {

    private KeyManager keyManager;

    @BeforeEach
    void setUp() {
        keyManager = new KeyManager();
    }

    // ===== Basic Input States =====

    @Test
    void testInitialState() {
        assertFalse(keyManager.up);
        assertFalse(keyManager.down);
        assertFalse(keyManager.left);
        assertFalse(keyManager.right);
        assertFalse(keyManager.attack);
        assertFalse(keyManager.shift);
        assertFalse(keyManager.defense);
        assertFalse(keyManager.escape);
        assertFalse(keyManager.enter);
        assertFalse(keyManager.i);
        assertFalse(keyManager.mute);
    }

    // ===== Movement Keys =====

    @Test
    void testMovementKeys() {
        // Test W key
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_W));
        assertTrue(keyManager.up);
        assertFalse(keyManager.down);
        assertFalse(keyManager.left);
        assertFalse(keyManager.right);

        // Test S key
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_S));
        assertTrue(keyManager.up);
        assertTrue(keyManager.down);
        assertFalse(keyManager.left);
        assertFalse(keyManager.right);

        // Test A key
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_A));
        assertTrue(keyManager.up);
        assertTrue(keyManager.down);
        assertTrue(keyManager.left);
        assertFalse(keyManager.right);

        // Test D key
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_D));
        assertTrue(keyManager.up);
        assertTrue(keyManager.down);
        assertTrue(keyManager.left);
        assertTrue(keyManager.right);
    }

    @Test
    void testArrowKeys() {
        // Test Up arrow
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_UP));
        assertTrue(keyManager.up);
        assertFalse(keyManager.down);

        // Test Down arrow
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_DOWN));
        assertTrue(keyManager.up);
        assertTrue(keyManager.down);

        // Test Left arrow
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_LEFT));
        assertTrue(keyManager.left);
        assertFalse(keyManager.right);

        // Test Right arrow
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_RIGHT));
        assertTrue(keyManager.left);
        assertTrue(keyManager.right);
    }

    // ===== Action Keys =====

    @Test
    void testSpaceKey() {
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_SPACE));
        assertTrue(keyManager.attack);

        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_SPACE));
        assertFalse(keyManager.attack);
    }

    @Test
    void testShiftKey() {
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_SHIFT));
        assertTrue(keyManager.shift);

        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_SHIFT));
        assertFalse(keyManager.shift);
    }

    // ===== Block Key (New Feature) =====

    @Test
    void testBlockKey() {
        // Test Ctrl key press
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_CONTROL));
        assertTrue(keyManager.defense);

        // Test Ctrl key release
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_CONTROL));
        assertFalse(keyManager.defense);
    }

    @Test
    void testBlockKeyMultiplePresses() {
        // Multiple presses should keep block true
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_CONTROL));
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_CONTROL));
        assertTrue(keyManager.defense);

        // Release should set it to false
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_CONTROL));
        assertFalse(keyManager.defense);
    }

    // ===== Other Keys =====

    @Test
    void testEscapeKey() {
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_ESCAPE));
        assertTrue(keyManager.escape);

        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_ESCAPE));
        assertFalse(keyManager.escape);
    }

    @Test
    void testEnterKey() {
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_ENTER));
        assertTrue(keyManager.enter);

        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_ENTER));
        assertFalse(keyManager.enter);
    }

    @Test
    void testIKey() {
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_I));
        assertTrue(keyManager.i);

        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_I));
        assertFalse(keyManager.i);
    }

    // ===== Mute Key (Toggle Behavior) =====

    @Test
    void testMuteKeyToggle() {
        // Initial state
        assertFalse(keyManager.mute);

        // First press should toggle to true
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_M));
        assertTrue(keyManager.mute);

        // Second press should not toggle (due to mDown logic)
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_M));
        assertTrue(keyManager.mute);

        // Release should reset mDown
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_M));
        assertTrue(keyManager.mute);

        // Next press should toggle to false
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_M));
        assertFalse(keyManager.mute);
    }

    // ===== Mouse Input =====

    @Test
    void testMouseInput() {
        // Test left mouse button press
        keyManager.mousePressed(createMouseEvent(1));
        assertTrue(keyManager.attack);

        // Test left mouse button release
        keyManager.mouseReleased(createMouseEvent(1));
        assertFalse(keyManager.attack);
    }

    // ===== Key Release Behavior =====

    @Test
    void testKeyRelease() {
        // Set up multiple keys
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_W));
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_D));
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_SPACE));
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_CONTROL));

        assertTrue(keyManager.up);
        assertTrue(keyManager.right);
        assertTrue(keyManager.attack);
        assertTrue(keyManager.defense);

        // Release all keys
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_W));
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_D));
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_SPACE));
        keyManager.keyReleased(createKeyEvent(KeyEvent.VK_CONTROL));

        assertFalse(keyManager.up);
        assertFalse(keyManager.right);
        assertFalse(keyManager.attack);
        assertFalse(keyManager.defense);
    }

    // ===== Edge Cases =====

    @Test
    void testUnknownKey() {
        // Test that unknown keys don't affect the state
        boolean initialState = keyManager.up;
        keyManager.keyPressed(createKeyEvent(KeyEvent.VK_F1)); // Unknown key
        assertEquals(initialState, keyManager.up);
    }

    @Test
    void testKeyTyped() {
        // keyTyped should not affect the state
        boolean initialState = keyManager.up;
        keyManager.keyTyped(createKeyEvent(KeyEvent.VK_W));
        assertEquals(initialState, keyManager.up);
    }

    // ===== Helper Methods =====

    private KeyEvent createKeyEvent(int keyCode) {
        return new KeyEvent(
            new java.awt.Button(), // source
            1, // id
            0L, // when
            0, // modifiers
            keyCode, // keyCode
            ' ', // keyChar
            1 // keyLocation
        );
    }

    private java.awt.event.MouseEvent createMouseEvent(int button) {
        return new java.awt.event.MouseEvent(
            new java.awt.Button(), // source
            1, // id
            0L, // when
            0, // modifiers
            0, // x
            0, // y
            0, // clickCount
            false, // popupTrigger
            button // button
        );
    }
}
