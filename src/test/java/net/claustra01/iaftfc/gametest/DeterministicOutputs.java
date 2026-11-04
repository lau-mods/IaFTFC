package net.claustra01.iaftfc.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.claustra01.iaftfc.worldgen.CaveOreSelection;
import net.claustra01.iaftfc.worldgen.ClimateSelection;
import net.claustra01.iaftfc.worldgen.DragonClimateWorldgen;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

final class DeterministicOutputs {
    static void write(ServerLevel level, String suffix) throws IOException {
        StringBuilder output = new StringBuilder();
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
            new Vec3(0.5, 80.5, 0.5)).create(LootContextParamSets.CHEST);
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            for (String place : new String[] {"roost", "female_cave", "male_cave"}) {
                var id = ResourceLocation.parse("iceandfire:chest/" + element + "_dragon_" + place);
                var table = level.getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, id));
                for (long seed = 1; seed <= 32; seed++) {
                    output.append(id).append(':').append(seed);
                    for (var stack : table.getRandomItems(params, seed)) {
                        output.append(':').append(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                            .append('*').append(stack.getCount());
                    }
                    output.append('\n');
                }
            }
        }
        var preset = level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
            .get(ResourceLocation.parse("tfc:overworld"));
        var generator = (TFCChunkGenerator) preset.overworld().orElseThrow().generator();
        var climate = DragonClimateWorldgen.selection(level.getSeed(), generator);
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -16; z <= 16; z += 4) {
                var point = DragonClimateWorldgen.climate(level.getSeed(), generator, x, z);
                var selected = climate.select(point, level.getSeed(), x, z, 9821379).element();
                int ore = CaveOreSelection.index(0, new BlockPos(x * 16, 40, z * 16));
                output.append(x).append(',').append(z).append(':').append(selected).append(':')
                    .append(CaveOreSelection.select(selected, ore)).append('\n');
            }
        }
        Path fixture = Path.of(System.getProperty("iaftfc.gametest.fixture"));
        Files.writeString(fixture.resolveSibling("draws-" + suffix + ".txt"), output);
    }

    private DeterministicOutputs() {}
}
