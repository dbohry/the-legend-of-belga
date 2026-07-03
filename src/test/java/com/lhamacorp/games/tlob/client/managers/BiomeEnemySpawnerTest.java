package com.lhamacorp.games.tlob.client.managers;

import com.lhamacorp.games.tlob.client.entities.Entity;
import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.maps.Biome;
import com.lhamacorp.games.tlob.client.maps.TileMap;
import com.lhamacorp.games.tlob.client.weapons.Sword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BiomeEnemySpawner} and its underlying {@link MobRegistry}.
 */
public class BiomeEnemySpawnerTest {

    private BiomeEnemySpawner spawner;
    private Random testRng;
    private Player mockPlayer;

    @BeforeEach
    public void setUp() {
        testRng = new Random(42L); // Fixed seed for deterministic testing
        spawner = new BiomeEnemySpawner(new Sword(2, 28, 12, 10, 16), testRng);
        mockPlayer = new Player(50, 50, new Sword(2, 28, 10, 10, 16));
    }

    private TileMap floorMap(Biome biome) {
        int[][] tiles = new int[20][20];
        for (int[] row : tiles) java.util.Arrays.fill(row, 0); // all floor
        return new TileMap(tiles, biome, testRng);
    }

    @Test
    public void testDefaultWeightsAreCorrect() {
        assertEquals(8.0, spawner.getRegistry().get("soldier").getWeight(Biome.MEADOWS), 0.01);
        assertEquals(2.0, spawner.getRegistry().get("archer").getWeight(Biome.MEADOWS), 0.01);
        assertEquals(0.0, spawner.getRegistry().get("golen").getWeight(Biome.MEADOWS), 0.01);

        assertEquals(4.0, spawner.getRegistry().get("soldier").getWeight(Biome.FOREST), 0.01);
        assertEquals(6.0, spawner.getRegistry().get("archer").getWeight(Biome.FOREST), 0.01);
        assertEquals(0.0, spawner.getRegistry().get("golen").getWeight(Biome.FOREST), 0.01);

        assertEquals(7.0, spawner.getRegistry().get("soldier").getWeight(Biome.VULCAN), 0.01);
        assertEquals(3.0, spawner.getRegistry().get("archer").getWeight(Biome.VULCAN), 0.01);
        assertEquals(0.0, spawner.getRegistry().get("golen").getWeight(Biome.VULCAN), 0.01);
    }

    @Test
    public void testDefaultBiomeMultipliersAreCorrect() {
        assertEquals(1.0, spawner.getRegistry().getBiomeMultiplier(Biome.MEADOWS), 0.01);
        assertEquals(1.2, spawner.getRegistry().getBiomeMultiplier(Biome.FOREST), 0.01);
        assertEquals(0.8, spawner.getRegistry().getBiomeMultiplier(Biome.CAVE), 0.01);
        assertEquals(1.1, spawner.getRegistry().getBiomeMultiplier(Biome.DESERT), 0.01);
        assertEquals(1.3, spawner.getRegistry().getBiomeMultiplier(Biome.VULCAN), 0.01);
    }

    @Test
    public void testSetEnemyTypeWeight() {
        spawner.getRegistry().get("archer").setWeight(Biome.FOREST, 8.0);
        assertEquals(8.0, spawner.getRegistry().get("archer").getWeight(Biome.FOREST), 0.01);

        // Other weights unaffected
        assertEquals(4.0, spawner.getRegistry().get("soldier").getWeight(Biome.FOREST), 0.01);
        assertEquals(0.0, spawner.getRegistry().get("golen").getWeight(Biome.FOREST), 0.01);
    }

    @Test
    public void testSetBiomeEnemyMultiplier() {
        spawner.getRegistry().setBiomeMultiplier(Biome.CAVE, 1.5);
        assertEquals(1.5, spawner.getRegistry().getBiomeMultiplier(Biome.CAVE), 0.01);

        assertEquals(1.0, spawner.getRegistry().getBiomeMultiplier(Biome.MEADOWS), 0.01);
        assertEquals(1.2, spawner.getRegistry().getBiomeMultiplier(Biome.FOREST), 0.01);
    }

    @Test
    public void testWeightValidation() {
        spawner.getRegistry().get("soldier").setWeight(Biome.MEADOWS, -5.0);
        assertEquals(0.0, spawner.getRegistry().get("soldier").getWeight(Biome.MEADOWS), 0.01);

        spawner.getRegistry().setBiomeMultiplier(Biome.DESERT, -2.0);
        assertEquals(0.1, spawner.getRegistry().getBiomeMultiplier(Biome.DESERT), 0.01);
    }

    @Test
    public void testWeightMatrixStructure() {
        // Every built-in mob should have a non-negative, reasonable weight in every biome.
        for (Biome biome : Biome.values()) {
            for (MobRegistry.MobEntry entry : spawner.getRegistry().all()) {
                double weight = entry.getWeight(biome);
                assertTrue(weight >= 0.0, "Weight should be non-negative");
                assertTrue(weight <= 10.0, "Weight should be reasonable (<= 10.0)");
            }
        }
    }

    @Test
    public void testMultiplierRange() {
        for (Biome biome : Biome.values()) {
            double multiplier = spawner.getRegistry().getBiomeMultiplier(biome);
            assertTrue(multiplier >= 0.1, "Multiplier should be >= 0.1");
            assertTrue(multiplier <= 3.0, "Multiplier should be reasonable (<= 3.0)");
        }
    }

    @Test
    public void testGolenIsRegisteredAsAnEliteSlot() {
        MobRegistry.MobEntry golen = spawner.getRegistry().get("golen");
        assertNotNull(golen);
        assertTrue(golen.isElite());
        assertEquals(SpawnManager.getGolenReplacementRatio(), golen.slotCost);
    }

    @Test
    public void testSpawnProducesOnlyWeightedMobsForBiome() {
        // Give Meadows only Soldiers (zero out Archer/Golen) and confirm spawn() honors it.
        spawner.getRegistry().get("archer").setWeight(Biome.MEADOWS, 0.0);
        spawner.getRegistry().get("golen").setWeight(Biome.MEADOWS, 0.0);

        List<Entity> enemies = new ArrayList<>();
        spawner.spawn(floorMap(Biome.MEADOWS), mockPlayer, enemies, 0, 32);

        assertFalse(enemies.isEmpty());
        for (Entity e : enemies) {
            assertEquals("Soldier", e.getName());
        }
    }

    /**
     * Demonstrates the core goal of the refactor: a brand-new mob can be added to the game
     * with a single registration call, and it immediately participates in weighted spawning
     * like any built-in mob - no enum, no array resize, no if/else edits required.
     */
    @Test
    public void testNewMobCanBeRegisteredInOneLine() {
        spawner.getRegistry()
            .register("skeleton", (x, y) -> new com.lhamacorp.games.tlob.client.entities.Soldier(
                x, y, new Sword(1, 20, 8, 8, 20)))
            .setDefaultWeight(0.0)
            .setWeight(Biome.CAVE, 100.0); // dominate the pool so it reliably spawns

        spawner.getRegistry().get("soldier").setWeight(Biome.CAVE, 0.0);
        spawner.getRegistry().get("archer").setWeight(Biome.CAVE, 0.0);
        spawner.getRegistry().get("golen").setWeight(Biome.CAVE, 0.0);

        assertNotNull(spawner.getRegistry().get("skeleton"));

        List<Entity> enemies = new ArrayList<>();
        spawner.spawn(floorMap(Biome.CAVE), mockPlayer, enemies, 0, 32);

        assertFalse(enemies.isEmpty());
    }
}
