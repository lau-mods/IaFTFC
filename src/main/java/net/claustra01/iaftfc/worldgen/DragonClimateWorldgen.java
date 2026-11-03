package net.claustra01.iaftfc.worldgen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.Settings;

public final class DragonClimateWorldgen {
    private static final Map<ClimateKey, WorldClimate> MODELS = new ConcurrentHashMap<>();

    public static ClimateSelection selection(long seed, TFCChunkGenerator generator) {
        return worldClimate(seed, generator).selection();
    }

    public static ClimateSelection.ClimateValues climate(long seed, TFCChunkGenerator generator, int chunkX, int chunkZ) {
        return climate(worldClimate(seed, generator).noise(), chunkX, chunkZ);
    }

    public static ClimateSelection.ClimateValues climate(RegionGenerator noise, int chunkX, int chunkZ) {
        double x = Units.blockToGridExact(chunkX * 16.0 + 8);
        double z = Units.blockToGridExact(chunkZ * 16.0 + 8);
        return new ClimateSelection.ClimateValues(noise.temperatureNoise.noise(x, z),
            noise.rainfallNoise.noise(x, z), noise.rainfallVarianceNoise.noise(x, z));
    }

    private static WorldClimate worldClimate(long seed, TFCChunkGenerator generator) {
        return MODELS.computeIfAbsent(new ClimateKey(seed, generator.settings()), key -> {
            RegionGenerator noise = new RegionGenerator(key.settings(), Seed.of(key.seed()));
            ClimateSelection selection = new ClimateSelection(index -> {
                long hash = ClimateSelection.splitMix64(key.seed() + index * 0x9E3779B97F4A7C15L);
                int chunkX = (short) (hash & 0xffff);
                int chunkZ = (short) ((hash >>> 16) & 0xffff);
                return climate(noise, chunkX, chunkZ);
            });
            return new WorldClimate(noise, selection);
        });
    }

    private record ClimateKey(long seed, Settings settings) {}

    private record WorldClimate(RegionGenerator noise, ClimateSelection selection) {}

    private DragonClimateWorldgen() {}
}
