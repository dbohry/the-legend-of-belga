package com.lhamacorp.games.tlob.client.maps;

/**
 * Represents different biomes in the game world, each with unique characteristics
 * and generation parameters.
 */
public enum Biome {
    MEADOWS("meadows", 0.45, 2500, 0.6, 0.3, 0.1),
    FOREST("forest", 0.55, 3000, 0.7, 0.4, 0.2),
    CAVE("cave", 0.35, 2000, 0.4, 0.1, 0.05),
    DESERT("desert", 0.30, 1800, 0.3, 0.05, 0.02),
    VULCAN("vulcan", 0.40, 2200, 0.2, 0.15, 0.25);
    
    private final String name;
    private final double wallDensity;
    private final int carveSteps;
    private final double plantDensity;
    private final double pathDensity;
    private final double featureDensity;
    
    Biome(String name, double wallDensity, int carveSteps, double plantDensity, 
          double pathDensity, double featureDensity) {
        this.name = name;
        this.wallDensity = wallDensity;
        this.carveSteps = carveSteps;
        this.plantDensity = plantDensity;
        this.pathDensity = pathDensity;
        this.featureDensity = featureDensity;
    }
    
    public String getName() {
        return name;
    }
    
    public double getWallDensity() {
        return wallDensity;
    }
    
    public int getCarveSteps() {
        return carveSteps;
    }
    
    public double getPlantDensity() {
        return plantDensity;
    }
    
    public double getPathDensity() {
        return pathDensity;
    }
    
    public double getFeatureDensity() {
        return featureDensity;
    }
    
    /**
     * Get the next biome in the progression cycle.
     */
    public Biome getNext() {
        Biome[] biomes = values();
        int currentIndex = ordinal();
        int nextIndex = (currentIndex + 1) % biomes.length;
        return biomes[nextIndex];
    }
    
    /**
     * Get biome by name (case-insensitive).
     */
    public static Biome fromName(String name) {
        for (Biome biome : values()) {
            if (biome.name.equalsIgnoreCase(name)) {
                return biome;
            }
        }
        return MEADOWS; // Default fallback
    }
}
