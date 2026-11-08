package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import net.dries007.tfc.common.TFCAttachments;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.claustra01.iaftfc.config.IaFTFCConfig;
import net.claustra01.iaftfc.mixin.StructurePlacementAccessor;
import net.claustra01.iaftfc.worldgen.DragonClimate;
import net.claustra01.iaftfc.worldgen.StructureSpacingResolver;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void climateCandidatesAreExclusiveAndDeterministic(GameTestHelper helper) {
        helper.assertValueEqual(DragonClimate.select(-5.0F, 400.0F).orElseThrow(),
            DragonClimate.Element.ICE, "Cold climate must select Ice");
        helper.assertValueEqual(DragonClimate.select(24.0F, 100.0F).orElseThrow(),
            DragonClimate.Element.FIRE, "Hot and dry climate must select Fire");
        helper.assertValueEqual(DragonClimate.select(12.0F, 500.0F).orElseThrow(),
            DragonClimate.Element.LIGHTNING, "Warm and wet climate must select Lightning");
        helper.assertTrue(DragonClimate.select(6.0F, 300.0F).isEmpty(),
            "A climate gap must not be assigned to another element");
        verifyClimateStarts(helper);

        Registry<StructureSet> sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        Map<String, Integer> references = new HashMap<>();
        for (StructureSet set : sets) {
            for (StructureSet.StructureSelectionEntry entry : set.structures()) {
                entry.structure().unwrapKey().ifPresent(key -> {
                    ResourceLocation id = key.location();
                    if (id.getNamespace().equals("iceandfire")
                        && id.getPath().matches("(fire|ice|lightning)_dragon_(roost|cave)")) {
                        references.merge(id.getPath(), 1, Integer::sum);
                    }
                });
            }
        }
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String type : List.of("roost", "cave")) {
                helper.assertValueEqual(references.getOrDefault(element + "_dragon_" + type, 0), 1,
                    "Each dragon structure must have exactly one registered set");
            }
        }

        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : landBiomes(helper)) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biome));
            for (String element : List.of("fire", "ice", "lightning")) {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME,
                    ResourceLocation.parse("iceandfire:structure_gen/" + element));
                helper.assertTrue(biomes.getHolderOrThrow(key).is(tag),
                    "Missing " + biome + " in the CE " + element + " structure tag");
            }
        }
        Set<String> actual = new HashSet<>();
        for (String prefix : List.of("tags/worldgen/biome", "worldgen/structure_set", "worldgen/configured_feature",
            "worldgen/placed_feature", "neoforge/biome_modifier", "recipe", "loot_table")) {
            helper.getLevel().getServer().getResourceManager()
                .listResourceStacks(prefix, id -> id.getNamespace().equals("iceandfire"))
                .forEach((id, resources) -> resources.forEach(resource -> {
                    if (resource.sourcePackId().contains("iaftfc")) {
                        actual.add(id.toString());
                    }
                }));
        }
        helper.assertValueEqual(actual, overrides(), "The manifest must match actual CE overrides");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void probabilityAndSpacingSettingsReachGeneration(GameTestHelper helper) {
        var level = helper.getLevel();
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure fireRoost = structures.get(ResourceLocation.parse("iceandfire:fire_dragon_roost"));
        double originalChance = IafCommonConfig.INSTANCE.worldGen.generateFireDragonRoostChance.getValue();
        int originalRoost = IaFTFCConfig.DRAGON_ROOST_SPACING.get();
        int originalCave = IaFTFCConfig.DRAGON_CAVE_SPACING.get();
        try {
            Registry<StructureSet> sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            for (String type : List.of("roost", "cave")) {
                RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement)
                    sets.get(ResourceLocation.parse("iceandfire:dragon_" + type)).placement();
                Set<ChunkPos> initial = candidateChunks(level, placement);
                if (type.equals("roost")) IaFTFCConfig.DRAGON_ROOST_SPACING.set(48);
                else IaFTFCConfig.DRAGON_CAVE_SPACING.set(60);
                StructureSpacingResolver.apply(level.getServer());
                helper.assertValueEqual(placement.spacing(), type.equals("roost") ? 48 : 60,
                    "The registered spacing must follow the IaFTFC setting");
                helper.assertTrue(!initial.equals(candidateChunks(level, placement)),
                    "Changing spacing must move registered structure candidates");
                if (type.equals("roost")) IaFTFCConfig.DRAGON_ROOST_SPACING.set(5);
                else IaFTFCConfig.DRAGON_CAVE_SPACING.set(5);
                StructureSpacingResolver.apply(level.getServer());
                helper.assertTrue(placement.separation() < placement.spacing(),
                    "Separation must remain below effective spacing");
                StructurePlacement.ExclusionZone exclusion = ((StructurePlacementAccessor) placement)
                    .iaftfc$getExclusionZone().orElseThrow();
                helper.assertValueEqual(exclusion.chunkCount(), 10, "Village exclusion must remain ten chunks");
                helper.assertValueEqual(exclusion.otherSet().unwrapKey().orElseThrow().location(),
                    ResourceLocation.parse("minecraft:villages"), "Village exclusion set must remain");
                if (type.equals("roost")) IaFTFCConfig.DRAGON_ROOST_SPACING.set(originalRoost);
                else IaFTFCConfig.DRAGON_CAVE_SPACING.set(originalCave);
                StructureSpacingResolver.apply(level.getServer());
                helper.assertValueEqual(placement.separation(), type.equals("roost") ? 10 : 20,
                    "Restoring spacing must restore the set separation");
            }
            helper.assertValueEqual(StructureSpacingResolver.spacing(StructureSpacingResolver.Kind.ROOST,
                new FutureCeWorldgen(64)), 64, "A CE spacing field must take priority");

            ChunkPos candidate = null;
            RandomSpreadStructurePlacement roost = (RandomSpreadStructurePlacement)
                sets.get(ResourceLocation.parse("iceandfire:dragon_roost")).placement();
            IafCommonConfig.INSTANCE.worldGen.generateFireDragonRoostChance.setValue(1.0D);
            for (int gx = -256; gx <= 256 && candidate == null; gx += 8) {
                for (int gz = -256; gz <= 256 && candidate == null; gz += 8) {
                    ChunkPos pos = roost.getPotentialStructureChunk(0L, gx * roost.spacing(), gz * roost.spacing());
                    ChunkData data = ((TFCChunkGenerator) level.getChunkSource().getGenerator())
                        .chunkDataGenerator().createAndGenerate(pos);
                    if (DragonClimate.select(data.getAverageSeaLevelTemp(pos.getMiddleBlockX(), pos.getMiddleBlockZ()),
                        data.getAverageGroundwater(pos.getMiddleBlockX(), pos.getMiddleBlockZ()))
                        .filter(DragonClimate.Element.FIRE::equals).isPresent()
                        && generate(level, fireRoost, pos).isValid()) {
                        candidate = pos;
                    }
                }
            }
            helper.assertTrue(candidate != null, "A Fire-eligible candidate must exist");
            IafCommonConfig.INSTANCE.worldGen.generateFireDragonRoostChance.setValue(0.0D);
            helper.assertTrue(!generate(level, fireRoost, candidate).isValid(), "Zero CE chance must reject a start");
            IafCommonConfig.INSTANCE.worldGen.generateFireDragonRoostChance.setValue(1.0D);
            helper.assertTrue(generate(level, fireRoost, candidate).isValid(),
                "Positive CE chance must allow a start at the same candidate");
            helper.assertTrue(!generate(level, fireRoost, new ChunkPos(level.getSharedSpawnPos())).isValid(),
                "CE spawn safety distance must reject a nearby start");
        } finally {
            IafCommonConfig.INSTANCE.worldGen.generateFireDragonRoostChance.setValue(originalChance);
            IaFTFCConfig.DRAGON_ROOST_SPACING.set(originalRoost);
            IaFTFCConfig.DRAGON_CAVE_SPACING.set(originalCave);
            StructureSpacingResolver.apply(level.getServer());
        }
        helper.succeed();
    }

    private static Set<ChunkPos> candidateChunks(net.minecraft.server.level.ServerLevel level,
        RandomSpreadStructurePlacement placement) {
        Set<ChunkPos> chunks = new HashSet<>();
        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                if (placement.isStructureChunk(level.getChunkSource().getGeneratorState(), x, z)) {
                    chunks.add(new ChunkPos(x, z));
                }
            }
        }
        return chunks;
    }

    private static StructureStart generate(net.minecraft.server.level.ServerLevel level, Structure structure,
        ChunkPos pos) {
        return structure.generate(level.registryAccess(), level.getChunkSource().getGenerator(),
            level.getChunkSource().getGenerator().getBiomeSource(),
            level.getChunkSource().getGeneratorState().randomState(), level.getServer().getStructureManager(),
            0L, pos, 0, level, structure.biomes()::contains);
    }

    private static Set<String> overrides() {
        try (InputStream stream = WorldgenGameTests.class.getResourceAsStream(
            "/iaftfc/gametest/override_manifest.json")) {
            if (stream == null) {
                throw new IllegalStateException("Override manifest is missing");
            }
            Set<String> values = new HashSet<>();
            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()
                .getAsJsonArray("allowed_overrides").forEach(value -> values.add(value.getAsString()));
            return values;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read override manifest", exception);
        }
    }

    private static List<String> landBiomes(GameTestHelper helper) {
        try (InputStream stream = helper.getLevel().getServer().getResourceManager()
            .getResource(ResourceLocation.parse("iaftfc:tags/worldgen/biome/dragon_land.json"))
            .orElseThrow().open()) {
            List<String> values = new java.util.ArrayList<>();
            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()
                .getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
            return values;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read TFC land biomes", exception);
        }
    }

    private static void verifyClimateStarts(GameTestHelper helper) {
        var level = helper.getLevel();
        Registry<StructureSet> sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        Set<String> checked = new HashSet<>();
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String type : List.of("roost", "cave")) {
                String target = element + "_dragon_" + type;
                String setId = element.equals("fire") ? "iceandfire:dragon_" + type
                    : "iaftfc:" + element + "_dragon_" + type;
                RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement)
                    sets.get(ResourceLocation.parse(setId)).placement();
                boolean started = false;
                for (int gx = -256; gx <= 256 && !started; gx += 8) {
                    for (int gz = -256; gz <= 256 && !started; gz += 8) {
                        ChunkPos pos = placement.getPotentialStructureChunk(0L,
                            gx * placement.spacing(), gz * placement.spacing());
                        ChunkData data = ((TFCChunkGenerator) level.getChunkSource().getGenerator())
                            .chunkDataGenerator().createAndGenerate(pos);
                        String climate = DragonClimate.select(
                            data.getAverageSeaLevelTemp(pos.getMiddleBlockX(), pos.getMiddleBlockZ()),
                            data.getAverageGroundwater(pos.getMiddleBlockX(), pos.getMiddleBlockZ()))
                            .map(value -> value.name().toLowerCase()).orElse("gap");
                        if (!climate.equals(element) && !(climate.equals("gap") && !checked.contains("gap"))) {
                            continue;
                        }
                        Map<String, Boolean> first = starts(level, pos, data);
                        helper.assertValueEqual(first, starts(level, pos, data),
                            "Repeated structure evaluation must be deterministic");
                        for (String other : List.of("fire", "ice", "lightning")) {
                            if (!other.equals(climate)) {
                                for (String otherType : List.of("roost", "cave")) {
                                    helper.assertTrue(!first.get(other + "_dragon_" + otherType),
                                        "Wrong element started in " + climate + " climate");
                                }
                            }
                        }
                        checked.add(climate);
                        started = first.get(target);
                    }
                }
                helper.assertTrue(started, "No eligible " + target + " started in seed-0 TFC terrain");
            }
        }
        helper.assertTrue(checked.contains("gap"), "An all-ineligible TFC climate must be evaluated");
    }

    private static Map<String, Boolean> starts(net.minecraft.server.level.ServerLevel level,
        ChunkPos pos, ChunkData data) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
            level.registryAccess().registryOrThrow(Registries.BIOME), null);
        chunk.setData(TFCAttachments.CHUNK_DATA.get(), data);
        level.getChunkSource().getGenerator().createStructures(level.registryAccess(),
            level.getChunkSource().getGeneratorState(), level.structureManager(), chunk,
            level.getServer().getStructureManager());
        Map<String, Boolean> results = new HashMap<>();
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String type : List.of("roost", "cave")) {
                Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .get(ResourceLocation.parse("iceandfire:" + element + "_dragon_" + type));
                StructureStart start = chunk.getStartForStructure(structure);
                results.put(element + "_dragon_" + type, start != null && start.isValid());
            }
        }
        return results;
    }

    public static final class FutureCeWorldgen {
        public final FutureIntegerEntry dragonRoostSpacing;

        public FutureCeWorldgen(int value) {
            dragonRoostSpacing = new FutureIntegerEntry(value);
        }
    }

    public static final class FutureIntegerEntry {
        private final int value;

        public FutureIntegerEntry(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private WorldgenGameTests() {
    }
}
