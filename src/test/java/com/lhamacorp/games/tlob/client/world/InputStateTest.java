package com.lhamacorp.games.tlob.client.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputStateTest {

    private InputState inputState;

    @BeforeEach
    void setUp() {
        inputState = new InputState();
    }

    @Test
    void testInitialState() {
        assertFalse(inputState.up);
        assertFalse(inputState.down);
        assertFalse(inputState.left);
        assertFalse(inputState.right);
        assertFalse(inputState.attack);
        assertFalse(inputState.shift);
        assertFalse(inputState.block);
    }

    @Test
    void testSetInputValues() {
        inputState.up = true;
        inputState.down = true;
        inputState.left = true;
        inputState.right = true;
        inputState.attack = true;
        inputState.shift = true;
        inputState.block = true;

        assertTrue(inputState.up);
        assertTrue(inputState.down);
        assertTrue(inputState.left);
        assertTrue(inputState.right);
        assertTrue(inputState.attack);
        assertTrue(inputState.shift);
        assertTrue(inputState.block);
    }

    @Test
    void testClearMethod() {
        // Set all inputs to true
        inputState.up = true;
        inputState.down = true;
        inputState.left = true;
        inputState.right = true;
        inputState.attack = true;
        inputState.shift = true;
        inputState.block = true;

        // Verify they're all true
        assertTrue(inputState.up);
        assertTrue(inputState.down);
        assertTrue(inputState.left);
        assertTrue(inputState.right);
        assertTrue(inputState.attack);
        assertTrue(inputState.shift);
        assertTrue(inputState.block);

        // Clear all inputs
        inputState.clear();

        // Verify they're all false
        assertFalse(inputState.up);
        assertFalse(inputState.down);
        assertFalse(inputState.left);
        assertFalse(inputState.right);
        assertFalse(inputState.attack);
        assertFalse(inputState.shift);
        assertFalse(inputState.block);
    }

    @Test
    void testPartialInputState() {
        // Set only some inputs
        inputState.up = true;
        inputState.right = true;
        inputState.block = true;

        // Verify only those are true
        assertTrue(inputState.up);
        assertFalse(inputState.down);
        assertFalse(inputState.left);
        assertTrue(inputState.right);
        assertFalse(inputState.attack);
        assertFalse(inputState.shift);
        assertTrue(inputState.block);
    }

    @Test
    void testInputStateIndependence() {
        // Create two input states
        InputState input1 = new InputState();
        InputState input2 = new InputState();

        // Set different values
        input1.up = true;
        input1.block = true;
        input2.down = true;
        input2.attack = true;

        // Verify they're independent
        assertTrue(input1.up);
        assertTrue(input1.block);
        assertFalse(input1.down);
        assertFalse(input1.attack);

        assertFalse(input2.up);
        assertFalse(input2.block);
        assertTrue(input2.down);
        assertTrue(input2.attack);
    }

    @Test
    void testBlockInputSpecific() {
        // Test block input specifically
        assertFalse(inputState.block);
        
        inputState.block = true;
        assertTrue(inputState.block);
        
        inputState.block = false;
        assertFalse(inputState.block);
    }

    @Test
    void testMixedInputStates() {
        // Test various combinations
        inputState.up = true;
        inputState.left = true;
        inputState.block = true;
        
        assertTrue(inputState.up);
        assertFalse(inputState.down);
        assertTrue(inputState.left);
        assertFalse(inputState.right);
        assertFalse(inputState.attack);
        assertFalse(inputState.shift);
        assertTrue(inputState.block);
        
        // Change some inputs
        inputState.up = false;
        inputState.right = true;
        inputState.attack = true;
        
        assertFalse(inputState.up);
        assertFalse(inputState.down);
        assertTrue(inputState.left);
        assertTrue(inputState.right);
        assertTrue(inputState.attack);
        assertFalse(inputState.shift);
        assertTrue(inputState.block);
    }
}
