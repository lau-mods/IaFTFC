package net.claustra01.iaftfc_test.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.claustra01.iaftfc.plant.IaFTFCPlant;
import net.claustra01.iaftfc.plant.IaFTFCPlants;
import net.claustra01.iaftfc.worldgen.ElementalPlacement;
import net.dries007.tfc.common.blocks.plant.Plant;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.dries007.tfc.world.placement.ClimatePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class ElementalGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 400)
    public static void elementalLiliesReplaceCeWorldgen(GameTestHelper helper) {
        Registry<PlacedFeature> features = helper.getLevel().registryAccess()
            .registryOrThrow(Registries.PLACED_FEATURE);
        var level = helper.getLevel();
        var generator = (net.dries007.tfc.world.TFCChunkGenerator) level.getChunkSource().getGenerator();
        java.util.Map<String, BlockPos> climates = new java.util.HashMap<>();
        for (int x = -4096; x <= 4096 && climates.size() < 4; x += 64) {
            for (int z = -4096; z <= 4096 && climates.size() < 4; z += 64) {
                var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                var data = generator.chunkDataGenerator().createAndGenerate(chunk);
                BlockPos sample = new BlockPos(chunk.getMiddleBlockX(), 128, chunk.getMiddleBlockZ());
                String matching = "gap";
                for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
                    PlacedFeature patch = features.get(ResourceLocation.parse("iaftfc:" + element.id() + "_patch"));
                    ClimatePlacement climate = patch.placement().stream()
                        .filter(ClimatePlacement.class::isInstance).map(ClimatePlacement.class::cast)
                        .findFirst().orElseThrow();
                    if (climate.isValid(data, sample, RandomSource.create(0L), false)) {
                        matching = element.id();
                    }
                }
                if (!climates.containsKey(matching)) {
                    BlockPos actual = sample;
                    String actualMatch = "gap";
                    for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
                        PlacedFeature patch = features.get(ResourceLocation.parse("iaftfc:" + element.id() + "_patch"));
                        ClimatePlacement climate = patch.placement().stream()
                            .filter(ClimatePlacement.class::isInstance).map(ClimatePlacement.class::cast)
                            .findFirst().orElseThrow();
                        if (climate.isValid(data, actual, RandomSource.create(0L), false)) {
                            actualMatch = element.id();
                        }
                    }
                    if (matching.equals(actualMatch)) {
                        boolean stable = true;
                        for (int dx : new int[] {-16, 16}) {
                            for (int dz : new int[] {-16, 16}) {
                                BlockPos corner = actual.offset(dx, 0, dz);
                                var cornerData = generator.chunkDataGenerator().createAndGenerate(
                                    new net.minecraft.world.level.ChunkPos(corner));
                                for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
                                    PlacedFeature patch = features.get(ResourceLocation.parse(
                                        "iaftfc:" + element.id() + "_patch"));
                                    ClimatePlacement climate = patch.placement().stream()
                                        .filter(ClimatePlacement.class::isInstance).map(ClimatePlacement.class::cast)
                                        .findFirst().orElseThrow();
                                    if (climate.isValid(cornerData, corner, RandomSource.create(0L), false)
                                        != matching.equals(element.id())) stable = false;
                                }
                            }
                        }
                        if (stable) {
                            for (int cx = (actual.getX() - 16) >> 4; cx <= (actual.getX() + 24) >> 4; cx++) {
                                for (int cz = (actual.getZ() - 16) >> 4; cz <= (actual.getZ() + 24) >> 4; cz++) {
                                    level.getChunk(cx, cz);
                                }
                            }
                            for (int dx = -16; dx < 24 && stable; dx++) {
                                for (int dz = -16; dz < 24 && stable; dz++) {
                                    stable = level.getHeight(
                                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                                        actual.getX() + dx, actual.getZ() + dz) < actual.getY() - 1;
                                }
                            }
                            if (stable) climates.put(matching, actual);
                        }
                    }
                }
            }
        }
        helper.assertValueEqual(climates.size(), 4, "Seed zero must supply each climate and a gap");
        Block dirt = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:dirt/entisol"));
        for (var climate : climates.entrySet()) {
            BlockPos center = climate.getValue();
            BlockPos origin = center.offset(-8, 0, -8);
            for (int dx = -8; dx < 32; dx++) {
                for (int dz = -8; dz < 32; dz++) {
                    BlockPos ground = origin.offset(dx, -1, dz);
                    level.setBlockAndUpdate(ground, dirt.defaultBlockState());
                    level.setBlockAndUpdate(ground.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
            for (int cx = (origin.getX() - 8) >> 4; cx <= (origin.getX() + 31) >> 4; cx++) {
                for (int cz = (origin.getZ() - 8) >> 4; cz <= (origin.getZ() + 31) >> 4; cz++) {
                    net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(level.getChunk(cx, cz),
                        java.util.Set.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
                }
            }
            for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
                String id = element.id();
                PlacedFeature patch = features.get(ResourceLocation.parse("iaftfc:" + id + "_patch"));
                Block lily = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iaftfc:plant/" + id));
                ClimatePlacement filter = patch.placement().stream()
                    .filter(ClimatePlacement.class::isInstance).map(ClimatePlacement.class::cast)
                    .findFirst().orElseThrow();
                boolean eligible = filter.isValid(net.dries007.tfc.world.chunkdata.ChunkData.get(level, origin),
                    origin, RandomSource.create(0L),
                    net.dries007.tfc.client.overworld.SolarCalculator.getInNorthernHemisphere(origin, level));
                helper.assertValueEqual(eligible, climate.getKey().equals(id),
                    id + " live climate filter must match " + climate.getKey());
                if (eligible) {
                    helper.assertValueEqual(level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        origin.getX(), origin.getZ()), origin.getY(),
                        "Prepared TFC soil must be the world surface");
                    PlacedFeature direct = features.get(ResourceLocation.parse("iaftfc:" + id));
                    helper.assertTrue(direct.place(level, generator, RandomSource.create(0L), origin),
                        "The direct TFC Lily feature must place on prepared soil");
                    level.setBlockAndUpdate(origin, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                boolean found = false;
                boolean placed = false;
                for (int seed = 0; seed < 2048 && !found; seed++) {
                    boolean attempt = patch.place(level, generator,
                        RandomSource.create(seed * 341873128712L), origin);
                    placed |= attempt;
                    if (!attempt) continue;
                    for (int dx = -8; dx < 32 && !found; dx++) {
                        for (int dz = -8; dz < 32 && !found; dz++) {
                            for (int dy = -1; dy <= 1 && !found; dy++) {
                                found = level.getBlockState(origin.offset(dx, dy, dz)).is(lily);
                            }
                        }
                    }
                }
                helper.assertValueEqual(placed, climate.getKey().equals(id),
                    id + " patch placement must follow " + climate.getKey());
                helper.assertValueEqual(found, climate.getKey().equals(id),
                    id + " patch must follow the registered climate at " + climate.getKey());
            }
        }
        Registry<net.minecraft.world.level.biome.Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (var biome : biomes) {
            boolean dragonLand = biomes.getResourceKey(biome).map(key ->
                biomes.getHolderOrThrow(key).is(net.minecraft.tags.TagKey.create(Registries.BIOME,
                    ResourceLocation.parse("iaftfc:dragon_land")))).orElse(false);
            for (var step : biome.getGenerationSettings().features()) {
                for (var feature : step) {
                    ResourceLocation id = feature.unwrapKey().orElseThrow().location();
                    helper.assertTrue(!id.getNamespace().equals("iceandfire") ||
                        !id.getPath().matches("(fire|frost|lightning)_lily"),
                        "No biome may contain a CE Lily placed feature");
                }
            }
            if (dragonLand) {
                for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
                    ResourceLocation id = ResourceLocation.parse("iaftfc:" + element.id() + "_patch");
                    helper.assertTrue(biome.getGenerationSettings().features().stream()
                        .flatMap(set -> set.stream()).anyMatch(holder -> holder.is(
                            net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE, id))),
                        "Dragon land biome must contain " + id);
                }
            }
        }
        for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
            String id = element.id();
            JsonObject modifier = resource(helper, "iceandfire:neoforge/biome_modifier/" + id + ".json");
            helper.assertValueEqual(modifier.get("type").getAsString(), "neoforge:none",
                "CE Lily biome modifier must be disabled");
            BlockPos target = helper.absolutePos(new BlockPos(4, 1, 4));
            level.setBlockAndUpdate(target.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            PlacedFeature ceFeature = features.get(ResourceLocation.parse("iceandfire:" + id));
            ceFeature.place(level, generator, RandomSource.create(0L), target);
            helper.assertTrue(!level.getBlockState(target).is(BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("iceandfire:" + id))), "CE feature must be inert on valid ground");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void elementalLiliesFollowTfcFlowerRules(GameTestHelper helper) {
        int index = 0;
        for (IaFTFCPlant plant : IaFTFCPlant.values()) {
            PlantBlock block = IaFTFCPlants.PLANTS.get(plant).get();
            BlockItem item = IaFTFCPlants.PLANT_ITEMS.get(plant).get();
            helper.assertTrue(item.getBlock() == block, "The plant item must place its matching PlantBlock");
            helper.assertTrue(block.getPlant() == plant, "PlantBlock must expose its RegistryPlant entry");
            verifyRegistryPlant(helper, plant);

            BlockPos ground = new BlockPos(1 + index, 0, 1);
            helper.setBlock(ground, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:dirt/entisol")));
            BlockState state = block.defaultBlockState();
            helper.assertTrue(state.canSurvive(helper.getLevel(), helper.absolutePos(ground.above())),
                "The Lily must survive on TFC bush soil");
            helper.setBlock(ground, net.minecraft.world.level.block.Blocks.STONE);
            helper.assertTrue(!state.canSurvive(helper.getLevel(), helper.absolutePos(ground.above())),
                "The Lily must reject non-plantable stone");
            helper.setBlock(ground, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:dirt/entisol")));
            helper.setBlock(ground.above(), net.minecraft.world.level.block.Blocks.AIR);
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            BlockPos placement = helper.absolutePos(ground);
            item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(placement), Direction.UP, placement, false)));
            helper.assertBlockPresent(block, ground.above());
            helper.assertTrue(state.is(BlockTags.SMALL_FLOWERS), "The Lily must be a vanilla small flower");
            for (String tag : List.of("minecraft:mineable/hoe", "minecraft:replaceable",
                "tfc:consumes_tool_durability", "tfc:natural_regrowing_plants", "tfc:can_be_snow_piled")) {
                helper.assertTrue(state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag))),
                    "Missing TFC flower block tag " + tag);
            }
            for (String tag : List.of("minecraft:small_flowers", "tfc:plants", "tfc:compost_greens/low")) {
                helper.assertTrue(new ItemStack(item).is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))),
                    "Missing TFC flower item tag " + tag);
            }
            BlockState placedState = helper.getLevel().getBlockState(helper.absolutePos(ground.above()));
            helper.assertTrue(placedState.getBlock() == block, "The BlockItem must place the Lily on TFC dirt");
            List<ItemStack> normalDrops = Block.getDrops(state, helper.getLevel(),
                helper.absolutePos(ground.above()), null, null, ItemStack.EMPTY);
            helper.assertTrue(normalDrops.isEmpty(), "Breaking a TFC flower without an accepted tool must not recover it");
            List<ItemStack> sharpDrops = Block.getDrops(state, helper.getLevel(),
                helper.absolutePos(ground.above()), null, null, new ItemStack(Items.WOODEN_HOE));
            helper.assertValueEqual(sharpDrops.size(), 1, "A TFC sharp tool must recover exactly one plant item");
            helper.assertTrue(sharpDrops.getFirst().is(item), "A TFC sharp tool must recover the matching plant item");
            List<ItemStack> shearDrops = Block.getDrops(state, helper.getLevel(),
                helper.absolutePos(ground.above()), null, null, new ItemStack(Items.SHEARS));
            helper.assertValueEqual(shearDrops.size(), 1, "Shears must recover exactly one plant item");
            helper.assertTrue(shearDrops.getFirst().is(item), "Shears must recover the matching plant item");
            var miner = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            miner.setPos(Vec3.atCenterOf(helper.absolutePos(ground.above())));
            miner.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            ItemStack hoe = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("tfc:metal/hoe/copper")));
            miner.setItemInHand(InteractionHand.MAIN_HAND, hoe);
            miner.gameMode.destroyBlock(helper.absolutePos(ground.above()));
            helper.assertTrue(miner.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() > 0,
                "Harvesting a TFC flower must wear the hoe");
            index++;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void elementalLiliesCraftCeStews(GameTestHelper helper) {
        verifyStew(helper, "fire_stew", "fire_lily", Items.BLAZE_ROD, "fire_stew");
        verifyStew(helper, "frost_stew", "frost_lily", Items.PRISMARINE_CRYSTALS, "frost_stew");
        verifyStew(helper, "lightning_lily_mixture", "lightning_lily", Items.CHORUS_FRUIT,
            "lightning_stew");
        helper.succeed();
    }

    private static void verifyRegistryPlant(GameTestHelper helper, IaFTFCPlant plant) {
        helper.assertTrue(plant.getAgeProperty() == Plant.BLOOD_LILY.getAgeProperty(),
            "Age behavior must match a standard TFC flower");
        helper.assertValueEqual(plant.isWetSeasonBlooming(), Plant.BLOOD_LILY.isWetSeasonBlooming(),
            "Season type must match a standard TFC flower");
        helper.assertValueEqual(plant.getStartTime(), Plant.BLOOD_LILY.getStartTime(),
            "Bloom start must match a standard TFC flower");
        helper.assertValueEqual(plant.getEndTime(), Plant.BLOOD_LILY.getEndTime(),
            "Bloom end must match a standard TFC flower");
        helper.assertValueEqual(plant.getSpeedFactor(), Plant.BLOOD_LILY.getSpeedFactor(),
            "Movement behavior must match a standard TFC flower");
        helper.assertValueEqual(plant.getBloomOffset(), Plant.BLOOD_LILY.getBloomOffset(),
            "Bloom offset must match a standard TFC flower");
        helper.assertValueEqual(plant.getBloomingEnd(), Plant.BLOOD_LILY.getBloomingEnd(),
            "Blooming stage must match a standard TFC flower");
        helper.assertValueEqual(plant.getSeedingEnd(), Plant.BLOOD_LILY.getSeedingEnd(),
            "Seeding stage must match a standard TFC flower");
        helper.assertValueEqual(plant.getDyingEnd(), Plant.BLOOD_LILY.getDyingEnd(),
            "Dying stage must match a standard TFC flower");
        helper.assertValueEqual(plant.getDormantEnd(), Plant.BLOOD_LILY.getDormantEnd(),
            "Dormant stage must match a standard TFC flower");
        helper.assertValueEqual(plant.getSproutingEnd(), Plant.BLOOD_LILY.getSproutingEnd(),
            "Sprouting stage must match a standard TFC flower");
    }

    private static void verifyStew(GameTestHelper helper, String recipeId, String newLily, Item other,
        String stewId) {
        RecipeHolder<?> holder = helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse("iceandfire:" + recipeId)).orElseThrow();
        helper.assertTrue(holder.value() instanceof CraftingRecipe, "The CE Stew override must remain a crafting recipe");
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        CraftingInput newInput = CraftingInput.of(3, 1, List.of(
            new ItemStack(Items.BOWL), new ItemStack(other),
            new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iaftfc:plant/" + newLily)))));
        helper.assertTrue(recipe.matches(newInput, helper.getLevel()),
            "The CE Stew recipe must accept the corresponding IaFTFC Lily");
        ItemStack result = recipe.assemble(newInput, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + stewId))),
            "The recipe must preserve the CE Stew item and its dragon breeding behavior");
        helper.assertValueEqual(result.getCount(), 1, "The recipe must produce one CE Stew");

        CraftingInput oldInput = CraftingInput.of(3, 1, List.of(
            new ItemStack(Items.BOWL), new ItemStack(other),
            new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + newLily)))));
        helper.assertTrue(!recipe.matches(oldInput, helper.getLevel()),
            "The overridden recipe must not require or accept the CE Lily");
    }

    private static JsonObject resource(GameTestHelper helper, String id) {
        ResourceLocation location = ResourceLocation.parse(id);
        try (var stream = helper.getLevel().getServer().getResourceManager().getResource(location).orElseThrow().open();
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + id, exception);
        }
    }

    private ElementalGameTests() {
    }
}
