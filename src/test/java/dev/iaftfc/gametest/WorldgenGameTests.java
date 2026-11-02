package dev.iaftfc.gametest;

import com.google.gson.JsonParser;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import dev.iaftfc.worldgen.CaveOreSelection;
import dev.iaftfc.worldgen.ClimateSelection;
import dev.iaftfc.worldgen.DragonClimateWorldgen;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.region.RegionGenerator;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private static final String[] ELEMENTS = {"fire", "ice", "lightning"};

    @GameTest(template = "empty")
    public static void climateSelection(GameTestHelper helper) {
        var preset = helper.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET)
            .get(ResourceLocation.parse("tfc:overworld"));
        var generator = (TFCChunkGenerator) preset.overworld().orElseThrow().generator();
        var noise = new RegionGenerator(generator.settings(), Seed.of(12345L));
        ClimateSelection firstSelection = new ClimateSelection(i -> sample(noise, 12345L, i));
        var secondNoise = new RegionGenerator(generator.settings(), Seed.of(12345L));
        ClimateSelection secondSelection = new ClimateSelection(i -> sample(secondNoise, 12345L, i));
        var center = DragonClimateWorldgen.climate(noise, 24, -36);
        var first = firstSelection.select(center, 12345L, 24, -36, 9821379);
        var second = secondSelection.select(center, 12345L, 24, -36, 9821379);
        helper.assertTrue(first.element() == second.element() && first.choice() == second.choice(), "Selection changed for one candidate");
        double[] scores = first.scores();
        double[][] values = new double[4096][3];
        double[] mean = new double[3];
        double[] deviation = new double[3];
        for (int i = 0; i < values.length; i++) {
            var point = sample(noise, 12345L, i);
            values[i] = new double[] {point.temperature(), point.rainfall(), point.variance()};
            for (int j = 0; j < 3; j++) mean[j] += values[i][j] / values.length;
        }
        for (double[] value : values) for (int j = 0; j < 3; j++) {
            deviation[j] += (value[j] - mean[j]) * (value[j] - mean[j]) / values.length;
        }
        for (int j = 0; j < 3; j++) deviation[j] = Math.max(Math.sqrt(deviation[j]), 1e-6);
        double zT = (center.temperature() - mean[0]) / deviation[0];
        double zR = (center.rainfall() - mean[1]) / deviation[1];
        double zV = (center.variance() - mean[2]) / deviation[2];
        helper.assertTrue(Math.abs(scores[0] - zT) < 1e-9 && Math.abs(scores[1] + zT) < 1e-9
            && Math.abs(scores[2] - (zR + 0.5 * zV - 0.5 * Math.abs(zT))) < 1e-9,
            "Climate scores changed");
        double[] warm = firstSelection.probabilities(firstSelection.scores(new ClimateSelection.ClimateValues(
            center.temperature() + 1, center.rainfall(), center.variance())));
        double[] cold = firstSelection.probabilities(firstSelection.scores(new ClimateSelection.ClimateValues(
            center.temperature() - 1, center.rainfall(), center.variance())));
        double[] rain = firstSelection.probabilities(firstSelection.scores(new ClimateSelection.ClimateValues(
            center.temperature(), center.rainfall() + 1, center.variance())));
        double[] variance = firstSelection.probabilities(firstSelection.scores(new ClimateSelection.ClimateValues(
            center.temperature(), center.rainfall(), center.variance() + 0.01)));
        helper.assertTrue(warm[0] / warm[1] > first.probabilities()[0] / first.probabilities()[1]
            && cold[1] / cold[0] > first.probabilities()[1] / first.probabilities()[0]
            && rain[2] / rain[0] > first.probabilities()[2] / first.probabilities()[0]
            && variance[2] / variance[0] > first.probabilities()[2] / first.probabilities()[0],
            "Climate suitability is not monotonic");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tfcClimateNoiseWithoutChunks(GameTestHelper helper) {
        var presets = helper.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET);
        var preset = presets.get(ResourceLocation.parse("tfc:overworld"));
        helper.assertTrue(preset != null, "TFC Overworld preset is missing");
        var generator = (TFCChunkGenerator) preset.overworld().orElseThrow().generator();
        var climateNoise = new RegionGenerator(generator.settings(), Seed.of(12345L));
        for (int i = 0; i < 4096; i++) {
            long hash = ClimateSelection.splitMix64(12345L + i * 0x9E3779B97F4A7C15L);
            int x = (short) (hash & 0xffff);
            int z = (short) ((hash >>> 16) & 0xffff);
            helper.assertTrue(!helper.getLevel().hasChunk(x, z), "Sample chunk was already loaded");
            var values = DragonClimateWorldgen.climate(climateNoise, x, z);
            helper.assertTrue(Double.isFinite(values.temperature()) && Double.isFinite(values.rainfall())
                && Double.isFinite(values.variance()), "TFC noise gave invalid climate values");
            helper.assertTrue(!helper.getLevel().hasChunk(x, z), "Climate sampling loaded a chunk");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tfcClimateBalanced(GameTestHelper helper) {
        var presets = helper.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET);
        var generator = (TFCChunkGenerator) presets.get(ResourceLocation.parse("tfc:overworld"))
            .overworld().orElseThrow().generator();
        for (long seed : new long[] {12345L, 67890L, -99L}) {
            ClimateSelection selection = DragonClimateWorldgen.selection(seed, generator);
            helper.assertTrue(selection == DragonClimateWorldgen.selection(seed, generator),
                "Climate correction was not cached for seed " + seed);
            double[] average = new double[3];
            for (int i = 0; i < 4096; i++) {
                long hash = ClimateSelection.splitMix64(seed + i * 0x9E3779B97F4A7C15L);
                int x = (short) (hash & 0xffff);
                int z = (short) ((hash >>> 16) & 0xffff);
                var climate = DragonClimateWorldgen.climate(seed, generator, x, z);
                double[] probabilities = selection.probabilities(selection.scores(climate));
                for (int element = 0; element < 3; element++) average[element] += probabilities[element] / 4096;
            }
            for (double probability : average) helper.assertTrue(probability >= 0.32 && probability <= 0.35,
                "Unbalanced TFC climate for seed " + seed + ": " + probability);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void structureFrequency(GameTestHelper helper) throws Exception {
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet roost = sets.get(ResourceLocation.parse("iceandfire:dragon_roost"));
        StructureSet cave = sets.get(ResourceLocation.parse("iceandfire:dragon_cave"));
        helper.assertTrue(roost != null && cave != null, "Dragon structure sets are missing");
        var roostPlacement = (RandomSpreadStructurePlacement) roost.placement();
        var cavePlacement = (RandomSpreadStructurePlacement) cave.placement();
        helper.assertTrue(roostPlacement.spacing() == 15 && roostPlacement.separation() == 6,
            "Roost placement changed");
        helper.assertTrue(cavePlacement.spacing() == 18 && cavePlacement.separation() == 12,
            "Cave placement changed");
        helper.assertTrue(roost.structures().size() == 3 && cave.structures().size() == 3,
            "Dragon structure entries changed");
        for (var spec : List.of(new Object[] {"dragon_roost", roostPlacement, 3747893},
            new Object[] {"dragon_cave", cavePlacement, 9821379})) {
            String name = (String) spec[0];
            var placement = (RandomSpreadStructurePlacement) spec[1];
            int salt = (int) spec[2];
            var resource = helper.getLevel().getServer().getResourceManager().getResource(
                ResourceLocation.fromNamespaceAndPath("iceandfire", "worldgen/structure_set/" + name + ".json")).orElseThrow();
            helper.assertTrue(resource.sourcePackId().equals("mod/iceandfire"), "Structure set was overridden: " + name);
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("placement");
                helper.assertTrue(json.get("salt").getAsInt() == salt
                    && json.getAsJsonObject("exclusion_zone").get("chunk_count").getAsInt() == 10
                    && json.getAsJsonObject("exclusion_zone").get("other_set").getAsString().equals("minecraft:villages"),
                    "Original salt or village exclusion changed: " + name);
            }
            Set<ChunkPos> candidates = new HashSet<>();
            for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) {
                candidates.add(placement.getPotentialStructureChunk(12345L, x * placement.spacing(), z * placement.spacing()));
            }
            helper.assertTrue(candidates.size() == 400, "Structure set candidate count changed: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonBiomesBeforePlacement(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var preset = helper.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET)
            .get(ResourceLocation.parse("tfc:overworld"));
        var generator = (TFCChunkGenerator) preset.overworld().orElseThrow().generator();
        var state = generator.createState(sets.asLookup(), helper.getLevel().getChunkSource().randomState(), 12345L);
        for (String kind : List.of("roost", "cave")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("iceandfire", "dragon_" + kind);
            helper.assertTrue(state.possibleStructureSets().stream().anyMatch(holder ->
                holder.unwrapKey().isPresent() && holder.unwrapKey().orElseThrow().location().equals(id)),
                "TFC generator excluded dragon " + kind + " before placement");
        }
        var land = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.parse("tfc:plains")));
        var ocean = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.parse("tfc:ocean")));
        for (String element : ELEMENTS) for (String kind : List.of("roost", "cave")) {
            var structure = structures.get(ResourceLocation.fromNamespaceAndPath("iceandfire", element + "_dragon_" + kind));
            helper.assertTrue(structure.biomes().contains(land), "TFC land was filtered before placement: " + element + " " + kind);
            helper.assertTrue(!structure.biomes().contains(ocean), "TFC ocean was allowed: " + element + " " + kind);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sixDragonStructures(GameTestHelper helper) throws Exception {
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        for (String element : ELEMENTS) for (String kind : List.of("roost", "cave")) {
            var structure = structures.get(ResourceLocation.fromNamespaceAndPath("iceandfire", element + "_dragon_" + kind));
            helper.assertTrue(structure != null, "Missing " + element + " dragon " + kind);
            boolean generated = false;
            for (int attempt = 0; attempt < 20 && !generated; attempt++) {
                var start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                    new ChunkPos(300 + attempt * 3, 300 + element.length() * 20 + kind.length() * 4),
                    0, level, biome -> true);
                generated = start.isValid() && !start.getPieces().isEmpty();
                if (generated) {
                    helper.assertTrue(start.getPieces().getFirst().getClass().getSimpleName()
                        .toLowerCase().contains(element), "Wrong dragon piece for " + element + " " + kind);
                    var gender = start.getPieces().getFirst().getClass().getSuperclass()
                        .getDeclaredField(kind.equals("roost") ? "isMale" : "male");
                    gender.setAccessible(true);
                    boolean male = gender.getBoolean(start.getPieces().getFirst());
                    BoundingBox box = start.getBoundingBox();
                    for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
                        for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                            ChunkPos current = new ChunkPos(chunkX, chunkZ);
                            start.placeInChunk(level, level.structureManager(), generator, RandomSource.create(55L),
                                new BoundingBox(current.getMinBlockX(), box.minY(), current.getMinBlockZ(),
                                    current.getMaxBlockX(), box.maxY(), current.getMaxBlockZ()), current);
                        }
                    }
                    var dragons = level.getEntities((Entity) null, new AABB(box.minX(), box.minY(), box.minZ(),
                        box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1), entity ->
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals(element + "_dragon"));
                    helper.assertTrue(!dragons.isEmpty(), "Generated " + element + " " + kind + " without its dragon");
                    for (Entity entity : dragons) {
                        DragonBaseEntity dragon = (DragonBaseEntity) entity;
                        helper.assertTrue(dragon.getAgeInDays() >= (kind.equals("roost") ? 40 : 75),
                            "Generated dragon is too young for its structure");
                        helper.assertTrue(dragon.getVariant() != null && !dragon.getVariant().isBlank(),
                            "Generated dragon has no colour variant");
                        helper.assertTrue(dragon.isMale() == male, "Generated dragon lost its piece gender");
                    }
                }
            }
            helper.assertTrue(generated, "Failed to generate " + element + " dragon " + kind);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void roostTerrain(GameTestHelper helper) throws Exception {
        BlockState stone = block("tfc:rock/raw/basalt").defaultBlockState();
        BlockState gravel = block("tfc:rock/gravel/basalt").defaultBlockState();
        BlockState dirt = block("tfc:dirt/entisol").defaultBlockState();
        for (String element : ELEMENTS) {
            Object piece = roostPiece(element);
            Method transform = piece.getClass().getDeclaredMethod("transform", BlockState.class);
            transform.setAccessible(true);
            for (var input : List.of(stone, block("tfc:rock/raw/granite").defaultBlockState(),
                block("tfc:rock/raw/diorite").defaultBlockState(), gravel,
                block("tfc:rock/gravel/granite").defaultBlockState(), dirt,
                block("tfc:dirt/alfisol").defaultBlockState())) {
                BlockState result = (BlockState) transform.invoke(piece, input);
                String path = BuiltInRegistries.BLOCK.getKey(input.getBlock()).getPath();
                String category = path.contains("/raw/") ? "stone" : path.contains("/gravel/") ? "gravel" : "dirt";
                helper.assertTrue(result.is(block("iceandfire:" + (element.equals("fire") ? "chared"
                    : element.equals("ice") ? "frozen" : "crackled") + "_" + category)),
                    "Roost did not transform TFC " + category + " for " + element + ": input="
                        + BuiltInRegistries.BLOCK.getKey(input.getBlock()) + ", output="
                        + BuiltInRegistries.BLOCK.getKey(result.getBlock()));
            }
            BlockState wood = (BlockState) transform.invoke(piece, block("tfc:wood/log/oak").defaultBlockState());
            helper.assertTrue(wood.is(block("iceandfire:" + (element.equals("ice") ? "frozen_splinters" : "ash"))),
                "Roost did not transform TFC wood for " + element);
            BlockState flower = (BlockState) transform.invoke(piece, block("tfc:plant/hibiscus").defaultBlockState());
            helper.assertTrue(flower.isAir(), "Roost did not remove TFC flowers for " + element);
        }
        helper.assertTrue(BuiltInRegistries.BLOCK.keySet().stream().noneMatch(id -> id.getNamespace().equals("iaftfc")
            && (id.getPath().contains("chared") || id.getPath().contains("frozen")
                || id.getPath().contains("crackled"))), "Roost registered per-rock attribute blocks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void roostBoulderGround(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        Object piece = roostPiece("fire");
        Method boulder = piece.getClass().getMethod("generateBoulder", net.minecraft.world.level.LevelAccessor.class,
            RandomSource.class, BlockPos.class, Block.class, int.class, boolean.class);
        for (int i = 0; i < 3; i++) {
            BlockPos ground = helper.absolutePos(new BlockPos(i * 8, 80, 0));
            Block source = switch (i) {
            case 0 -> block("tfc:grass/entisol");
                case 1 -> block("tfc:rock/raw/basalt");
                default -> block("tfc:rock/gravel/basalt");
            };
            level.setBlock(ground, source.defaultBlockState(), 3);
            boulder.invoke(piece, level, RandomSource.create(44L), ground.above(), Blocks.COBBLESTONE, 1, true);
            helper.assertTrue(level.getBlockState(ground.above()).is(Blocks.COBBLESTONE),
                "Boulder sank below " + source);
        }
        BlockPos protectedGround = helper.absolutePos(new BlockPos(32, 80, 0));
        level.setBlock(protectedGround, block("tfc:rock/raw/basalt").defaultBlockState(), 3);
        level.setBlock(protectedGround.above(), Blocks.CHEST.defaultBlockState(), 3);
        boulder.invoke(piece, level, RandomSource.create(44L), protectedGround.above(), Blocks.COBBLESTONE, 1, true);
        helper.assertTrue(level.getBlockEntity(protectedGround.above()) != null
            && level.getBlockState(protectedGround.above()).is(Blocks.CHEST), "Boulder removed a chest");
        Method pile = piece.getClass().getSuperclass().getDeclaredMethod("generateRoostPile",
            net.minecraft.world.level.WorldGenLevel.class, RandomSource.class, BlockPos.class, Block.class);
        pile.setAccessible(true);
        BlockPos pileCenter = helper.absolutePos(new BlockPos(40, 80, 0));
        for (int y = 0; y < 3; y++) {
            level.setBlock(pileCenter.above(y), Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(pileCenter.east().above(y), Blocks.BEDROCK.defaultBlockState(), 3);
        }
        for (long seed = 0; seed < 10; seed++) pile.invoke(piece, level, RandomSource.create(seed), pileCenter, Blocks.COBBLESTONE);
        for (int y = 0; y < 3; y++) {
            helper.assertTrue(level.getBlockState(pileCenter.above(y)).is(Blocks.CHEST)
                && level.getBlockEntity(pileCenter.above(y)) != null, "Roost pile removed a block entity");
            helper.assertTrue(level.getBlockState(pileCenter.east().above(y)).is(Blocks.BEDROCK),
                "Roost pile removed bedrock");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void caveOreHostRock(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        for (int e = 0; e < 3; e++) {
            Object piece = cavePiece(ELEMENTS[e], 99L);
            Method shell = piece.getClass().getMethod("createShell", net.minecraft.world.level.LevelAccessor.class,
                RandomSource.class, Set.class);
            Set<BlockPos> positions = new HashSet<>();
            for (int i = 0; i < 300; i++) {
                BlockPos pos = helper.absolutePos(new BlockPos(e * 2, 10 + i, 0));
                positions.add(pos);
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
            shell.invoke(piece, level, RandomSource.create(5678L), positions);
            int ores = 0;
            java.util.Map<BlockPos, ResourceLocation> firstOres = new java.util.HashMap<>();
            for (BlockPos pos : positions) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
                if (level.getBlockState(pos).is(Tags.Blocks.ORES) || id.getPath().startsWith("ore/")) {
                    ores++;
                    firstOres.put(pos, id);
                    boolean expected = false;
                    for (int index = 0; index < 9; index++) {
                        if (id.equals(CaveOreSelection.select(ClimateSelection.Element.values()[e], index))) expected = true;
                    }
                    helper.assertTrue(expected, "Unexpected cave ore: " + id);
                }
            }
            helper.assertTrue(ores >= 5, "Cave placed too few gem ores: " + ores);
            for (BlockPos pos : positions) level.setBlock(pos, block("tfc:rock/raw/basalt").defaultBlockState(), 3);
            shell.invoke(piece, level, RandomSource.create(5678L), positions);
            for (var ore : firstOres.entrySet()) {
                helper.assertTrue(BuiltInRegistries.BLOCK.getKey(level.getBlockState(ore.getKey()).getBlock())
                    .equals(ore.getValue()), "Host rock changed the cave gem at " + ore.getKey());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void caveOreIndependent(GameTestHelper helper) throws Exception {
        for (var element : ClimateSelection.Element.values()) {
            Set<ResourceLocation> ores = new HashSet<>();
            for (int index = 0; index < 9; index++) {
                ResourceLocation id = CaveOreSelection.select(element, index);
                helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Missing TFC gem ore: " + id);
                ores.add(id);
            }
            helper.assertTrue(ores.size() == 9, "Gem selection is not one-to-one");
        }
        BlockPos pos = new BlockPos(10, 30, -4);
        int[] counts = new int[9];
        for (long seed = 0; seed < 100; seed++) for (int x = 0; x < 1000; x++) {
            counts[CaveOreSelection.index(seed, new BlockPos(x, 30, -4))]++;
        }
        for (int count : counts) helper.assertTrue(count > 10000 && count < 12300,
            "Gem index is not close to 1/9: " + count);
        var level = helper.getLevel();
        for (String element : ELEMENTS) {
            Object piece = cavePiece(element, 99L);
            Method shell = piece.getClass().getMethod("createShell",
                net.minecraft.world.level.LevelAccessor.class, RandomSource.class, Set.class);
            ClimateSelection.Element type = ClimateSelection.Element.valueOf(element.toUpperCase(java.util.Locale.ROOT));
            for (double branch : new double[] {-1, 0.1, 0.3, 0.9}) {
                for (String host : List.of("minecraft:stone", "tfc:rock/raw/basalt", "tfc:rock/raw/granite")) {
                    BlockPos target = helper.absolutePos(new BlockPos(0, 100, 0));
                    level.setBlock(target, block(host).defaultBlockState(), 3);
                    int[] doubleCalls = {0};
                    RandomSource random = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                        new Class<?>[] {RandomSource.class}, (proxy, method, args) -> switch (method.getName()) {
                            case "nextBoolean" -> branch < 0;
                            case "nextDouble" -> doubleCalls[0]++ == 0 ? 0.0 : branch;
                            case "nextInt" -> 0;
                            default -> throw new AssertionError("Unexpected ore RNG call: " + method.getName());
                        });
                    shell.invoke(piece, level, random, Set.of(target));
                    ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock());
                    ResourceLocation expected = CaveOreSelection.select(type, CaveOreSelection.index(99L, target));
                    helper.assertTrue(actual.equals(expected), "Ore branch or host rock changed cave gem: "
                        + element + " " + branch + " " + host + " -> " + actual);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cavePreservesBlockEntities(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        Object piece = cavePiece("fire", 99L);
        Method hollow = piece.getClass().getMethod("hollowOut", net.minecraft.world.level.LevelAccessor.class, Set.class);
        BlockPos anvil = helper.absolutePos(new BlockPos(0, 90, 0));
        BlockPos stone = anvil.east();
        BlockPos bedrock = anvil.west();
        Block tfcAnvil = block("tfc:metal/anvil/copper");
        helper.assertTrue(!(tfcAnvil instanceof BaseEntityBlock), "TFC anvil must exercise the cave protection guard");
        level.setBlock(anvil, tfcAnvil.defaultBlockState(), 3);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 3);
        Method shell = piece.getClass().getMethod("createShell", net.minecraft.world.level.LevelAccessor.class,
            RandomSource.class, Set.class);
        shell.invoke(piece, level, RandomSource.create(55L), Set.of(anvil, stone, bedrock));
        helper.assertTrue(level.getBlockEntity(anvil) != null && level.getBlockState(anvil).is(tfcAnvil),
            "Cave shell removed a block entity");
        hollow.invoke(piece, level, Set.of(anvil, stone, bedrock));
        helper.assertTrue(level.getBlockEntity(anvil) != null && level.getBlockState(anvil).is(tfcAnvil),
            "Cave removed a block entity");
        helper.assertTrue(level.getBlockState(stone).isAir() && level.getBlockState(bedrock).isAir(),
            "Cave hollowing changed its ordinary or unbreakable behavior");
        helper.succeed();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private static ClimateSelection.ClimateValues sample(RegionGenerator noise, long seed, int index) {
        long hash = ClimateSelection.splitMix64(seed + index * 0x9E3779B97F4A7C15L);
        return DragonClimateWorldgen.climate(noise, (short) (hash & 0xffff), (short) ((hash >>> 16) & 0xffff));
    }

    private static Object roostPiece(String element) throws Exception {
        Class<?> type = Class.forName("com.iafenvoy.iceandfire.world.structure."
            + capitalize(element) + "DragonRoostStructure$" + capitalize(element) + "DragonRoostPiece");
        Constructor<?> constructor = type.getDeclaredConstructor(int.class, BoundingBox.class, Block.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(0, new BoundingBox(0, 0, 0, 0, 0, 0), Blocks.GOLD_BLOCK, true);
    }

    private static Object cavePiece(String element, long seed) throws Exception {
        Class<?> type = Class.forName("com.iafenvoy.iceandfire.world.structure."
            + capitalize(element) + "DragonCaveStructure$" + capitalize(element) + "DragonCavePiece");
        Constructor<?> constructor = type.getDeclaredConstructor(int.class, BoundingBox.class, boolean.class,
            BlockPos.class, int.class, long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(0, new BoundingBox(0, 0, 0, 0, 0, 0), true, BlockPos.ZERO, 30, seed);
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
