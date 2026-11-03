package net.claustra01.iaftfc.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.claustra01.iaftfc.worldgen.ClimateSelection;
import net.claustra01.iaftfc.worldgen.DragonClimateWorldgen;
import net.claustra01.iaftfc.worldgen.ElementalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.fml.ModList;
import net.dries007.tfc.world.TFCChunkGenerator;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class ElementalWorldgenGameTests {
    private static final String[] ELEMENTS = {"fire", "ice", "lightning"};

    @GameTest(template = "empty")
    public static void silverSapphireVeinsAndRecipes(GameTestHelper helper) throws Exception {
        helper.assertTrue(helper.getLevel().getChunkSource().getGenerator() instanceof TFCChunkGenerator
            && helper.getLevel().getSeed() == 0L, "GameTest must generate the fixed-seed TFC Overworld");
        Set<ResourceLocation> disabledFeatures = new java.util.HashSet<>();
        for (String ore : List.of("silver", "sapphire")) {
            JsonObject modifier = json(helper, "iceandfire:neoforge/biome_modifier/" + ore + "_ore.json");
            helper.assertTrue(modifier.get("type").getAsString().equals("neoforge:none"),
                "IaF ore biome modifier remains active: " + ore);
            disabledFeatures.add(ResourceLocation.parse(iafJson("neoforge/biome_modifier/" + ore
                + "_ore.json").get("features").getAsString()));
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("iceandfire:" + ore + "_ore")),
                "Legacy IaF ore ID disappeared: " + ore);
        }
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("tfc:ore/normal_native_silver"))
            && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("tfc:gem/sapphire"))
            && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("tfc:ore/sapphire/basalt")),
            "TFC silver or sapphire is unavailable");
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        int sampled = 0;
        int nativeSilver = 0;
        for (var biome : biomes.holders().toList()) {
            if (!biome.key().location().getNamespace().equals("tfc")) continue;
            sampled++;
            Set<ResourceLocation> features = features(biome.value());
            helper.assertTrue(features.stream().noneMatch(disabledFeatures::contains),
                "IaF ore was applied to a TFC biome: " + biome.key().location());
            if (features.contains(ResourceLocation.parse("tfc:vein/normal_native_silver"))) nativeSilver++;
        }
        helper.assertTrue(sampled >= 100 && nativeSilver > 0,
            "TFC biome or native silver generation is missing: " + sampled + ", " + nativeSilver);
        helper.assertTrue(biomes.holders().anyMatch(biome -> features(biome.value())
            .contains(ResourceLocation.parse("tfc:random_active_hot_spring"))),
            "TFC Sapphire hot spring is absent from world generation");
        int silverOre = 0;
        int sapphireOre = 0;
        for (int chunkX = -8; chunkX < 8; chunkX++) for (int chunkZ = -8; chunkZ < 8; chunkZ++) {
            var chunk = helper.getLevel().getChunk(chunkX, chunkZ);
            for (int sectionY = chunk.getMinSection(); sectionY < chunk.getMaxSection(); sectionY++) {
                var section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                if (section.hasOnlyAir()) continue;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    ResourceLocation block = BuiltInRegistries.BLOCK.getKey(section.getBlockState(x, y, z).getBlock());
                    helper.assertTrue(!block.equals(ResourceLocation.parse("iceandfire:silver_ore"))
                        && !block.equals(ResourceLocation.parse("iceandfire:sapphire_ore")),
                        "IaF ore generated in TFC chunk " + chunkX + "," + chunkZ);
                    if (block.getNamespace().equals("tfc") && block.getPath().startsWith("ore/")) {
                        if (block.getPath().contains("native_silver")) silverOre++;
                        if (block.getPath().startsWith("ore/sapphire/")) sapphireOre++;
                    }
                }
            }
        }
        helper.assertTrue(silverOre > 0, "TFC native silver did not generate in 256 sampled chunks");
        ConfiguredFeature<?, ?> spring = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
            .get(ResourceLocation.parse("tfc:sapphire_hot_spring"));
        helper.assertTrue(spring != null, "Original TFC Sapphire feature is unavailable");
        BlockPos springPos = new BlockPos(512, 90, 512);
        var basalt = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/raw/basalt")).defaultBlockState();
        for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) {
            for (int y = 60; y < 90; y++) helper.getLevel().setBlock(springPos.offset(x, y - 90, z), basalt, 2);
            for (int y = 90; y < 96; y++) helper.getLevel().setBlock(springPos.offset(x, y - 90, z), Blocks.AIR.defaultBlockState(), 2);
        }
        int springPlacements = 0;
        for (int attempt = 0; attempt < 8 && sapphireOre == 0; attempt++) {
            if (spring.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(attempt), springPos)) springPlacements++;
            for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) for (int y = 60; y < 90; y++) {
                ResourceLocation block = BuiltInRegistries.BLOCK.getKey(helper.getLevel()
                    .getBlockState(new BlockPos(springPos.getX() + x, y, springPos.getZ() + z)).getBlock());
                if (block.getNamespace().equals("tfc") && block.getPath().startsWith("ore/sapphire/")) sapphireOre++;
            }
        }
        helper.assertTrue(sapphireOre > 0,
            "Original TFC hot spring did not produce Sapphire ore in " + springPlacements + " placements");
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String part : List.of("head", "neck", "body", "tail")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.parse("iceandfire:dragonarmor_silver_" + part)).isEmpty(),
                "M07 silver dragon armor route reopened");
        }
        for (var recipe : recipes.getRecipes()) {
            String path = recipe.id().getPath();
            String result = BuiltInRegistries.ITEM.getKey(recipe.value().getResultItem(helper.getLevel().registryAccess()).getItem()).getPath();
            if (!path.contains("dragon") && !path.contains("stew") && !path.contains("lily")
                && !result.contains("dragon") && !result.contains("stew") && !result.contains("lily")) continue;
            String data = Recipe.CODEC.encodeStart(helper.getLevel().registryAccess()
                .createSerializationContext(JsonOps.INSTANCE), recipe.value()).getOrThrow().toString();
            for (String old : List.of("iceandfire:silver_ingot", "iceandfire:silver_nugget",
                "iceandfire:raw_silver", "iceandfire:sapphire_gem", "iceandfire:ingots/silver",
                "iceandfire:gems/sapphire", "c:ingots/silver", "c:gems/sapphire"))
                helper.assertTrue(!data.contains(old), "Dragon recipe still consumes an IaF metal or gem: "
                    + recipe.id() + " " + old);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lilyClimateGroundAndStew(GameTestHelper helper) throws Exception {
        TFCChunkGenerator generator = tfcGenerator(helper);
        var context = new PlacementContext(helper.getLevel(), generator, Optional.empty());
        var land = TagKey.create(Registries.BIOME, ResourceLocation.parse("iaftfc:dragon_land"));
        int landCount = 0;
        for (var biome : helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).holders().toList()) {
            if (!biome.is(land)) continue;
            landCount++;
            Set<ResourceLocation> features = features(biome.value());
            for (String element : ELEMENTS) {
                String lily = element.equals("ice") ? "frost_lily" : element + "_lily";
                helper.assertTrue(features.contains(ResourceLocation.parse("iceandfire:" + lily)),
                    "Lily feature is absent from TFC land: " + biome.key().location() + " " + lily);
                helper.assertTrue(!features.contains(ResourceLocation.parse("iaftfc:" + lily)),
                    "Duplicate lily feature in TFC biome: " + biome.key().location() + " " + lily);
            }
        }
        helper.assertTrue(landCount >= 50, "TFC land tag is too narrow for lilies: " + landCount);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        for (int index = 0; index < ELEMENTS.length; index++) {
            String element = ELEMENTS[index];
            String lily = element.equals("ice") ? "frost_lily" : element + "_lily";
            JsonObject feature = json(helper, "iceandfire:worldgen/placed_feature/" + lily + ".json");
            helper.assertTrue(feature.get("feature").getAsString().equals("iceandfire:" + lily)
                && feature.getAsJsonArray("placement").size() == 5
                && feature.getAsJsonArray("placement").get(0).getAsJsonObject().get("chance").getAsInt() == 32,
                "IaF lily attempt distribution changed: " + lily);
            JsonObject configured = json(helper, "iceandfire:worldgen/configured_feature/" + lily + ".json");
            var settings = configured.getAsJsonObject("config");
            var candidatePlacements = settings.getAsJsonObject("feature").getAsJsonArray("placement");
            helper.assertTrue(settings.get("tries").getAsInt() == 8
                && settings.get("xz_spread").getAsInt() == 7 && settings.get("y_spread").getAsInt() == 3
                && candidatePlacements.size() == 2
                && candidatePlacements.get(1).getAsJsonObject().get("lily").getAsBoolean(),
                "Lily candidate checks or original spread changed: " + lily);
            String ground = switch (element) {
                case "fire" -> "tfc:sand/yellow";
                case "ice" -> "tfc:sea_ice";
                default -> "tfc:grass/entisol";
            };
            var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(ground)).defaultBlockState();
            helper.getLevel().setBlock(pos.below(), state, 3);
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + lily))
                .defaultBlockState().canSurvive(helper.getLevel(), pos), "Lily cannot survive on TFC ground: " + ground);
            boolean accepted = new ElementalPlacement(ClimateSelection.Element.valueOf(element.toUpperCase(java.util.Locale.ROOT)), true).getPositions(context, RandomSource.create(42), pos)
                .findAny().isPresent();
            helper.assertTrue(accepted, "Lily rejected suitable ground: " + element);
            helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(new ElementalPlacement(ClimateSelection.Element.valueOf(element.toUpperCase(java.util.Locale.ROOT)), true).getPositions(context, RandomSource.create(42), pos)
                .findAny().isEmpty(), "Lily accepted unsuitable ground: " + element);
            String stewRecipe = switch (element) {
                case "ice" -> "frost_stew";
                case "lightning" -> "lightning_lily_mixture";
                default -> "fire_stew";
            };
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.parse("iceandfire:" + stewRecipe))
                .isPresent(), "Original stew recipe is unavailable: " + element);
            helper.assertTrue(sameAsIaf(helper, "recipe/" + stewRecipe + ".json"),
                "Original stew recipe was changed: " + stewRecipe);
            String stewItem = element.equals("lightning") ? "lightning_stew" : stewRecipe;
            DragonBaseEntity dragon = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.parse("iceandfire:" + element + "_dragon")).create(helper.getLevel());
            helper.assertTrue(dragon != null, "Original dragon is unavailable: " + element);
            dragon.setAgeInTicks(2400000);
            ItemStack stew = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + stewItem)));
            helper.assertTrue(dragon.isMature() && dragon.isFood(stew),
                "Original stew is no longer breeding food: " + element);
            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            player.setItemInHand(InteractionHand.MAIN_HAND, stew);
            dragon.mobInteract(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(dragon.isInLove(), "Original stew no longer starts breeding: " + element);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skeletonClimateAndLoot(GameTestHelper helper) throws Exception {
        TFCChunkGenerator generator = tfcGenerator(helper);
        var context = new PlacementContext(helper.getLevel(), generator, Optional.empty());
        var land = TagKey.create(Registries.BIOME, ResourceLocation.parse("iaftfc:dragon_land"));
        int landCount = 0;
        for (var biome : helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).holders().toList()) {
            if (!biome.is(land)) continue;
            landCount++;
            Set<ResourceLocation> features = features(biome.value());
            for (String element : ELEMENTS) {
                helper.assertTrue(features.contains(ResourceLocation.parse("iceandfire:spawn_dragon_skeleton_" + element)),
                    "Skeleton feature is absent from TFC land: " + biome.key().location() + " " + element);
                helper.assertTrue(!features.contains(ResourceLocation.parse("iaftfc:spawn_dragon_skeleton_" + element)),
                    "Duplicate skeleton feature in TFC biome: " + biome.key().location() + " " + element);
            }
        }
        helper.assertTrue(landCount >= 50, "TFC land tag is too narrow for skeletons: " + landCount);
        var selection = DragonClimateWorldgen.selection(helper.getLevel().getSeed(), generator);
        boolean[] found = new boolean[3];
        for (int i = 0; i < 4096; i++) {
            long hash = ClimateSelection.splitMix64(helper.getLevel().getSeed() + i * 0x9E3779B97F4A7C15L);
            int x = (short) (hash & 0xffff);
            int z = (short) ((hash >>> 16) & 0xffff);
            var scores = selection.scores(DragonClimateWorldgen.climate(helper.getLevel().getSeed(), generator, x, z));
            BlockPos pos = new BlockPos(x * 16 + 8, 80, z * 16 + 8);
            for (int element = 0; element < 3; element++) {
                boolean accepted = new ElementalPlacement(ClimateSelection.Element.values()[element], false)
                    .getPositions(context, RandomSource.create(i), pos).findAny().isPresent();
                boolean expected = scores[element] >= scores[0] && scores[element] >= scores[1]
                    && scores[element] >= scores[2];
                helper.assertTrue(accepted == expected, "Skeleton climate selection differs at " + x + "," + z);
                found[element] |= accepted;
            }
        }
        for (int element = 0; element < 3; element++) {
            String name = ELEMENTS[element];
            helper.assertTrue(found[element], "No suitable TFC climate found for " + name + " skeleton");
            JsonObject placed = json(helper, "iceandfire:worldgen/placed_feature/spawn_dragon_skeleton_" + name + ".json");
            helper.assertTrue(placed.get("feature").getAsString().equals("iceandfire:spawn_dragon_skeleton_" + name)
                && placed.getAsJsonArray("placement").size() == 3,
                "Skeleton feature or placement distribution changed: " + name);
            helper.assertTrue(sameAsIaf(helper, "loot_table/entities/dragon/" + name
                + "_dragon_skeleton.json"), "Original skeleton loot was changed: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lilyFeaturePlacesOnTfcSoil(GameTestHelper helper) {
        var level = helper.getLevel();
        TFCChunkGenerator generator = tfcGenerator(helper);
        var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var soil = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:dirt/entisol")).defaultBlockState();
        for (int element = 0; element < 3; element++) {
            BlockPos pos = strongClimate(helper, generator, element);
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                level.setBlock(pos.offset(x, -1, z), soil, 3);
                for (int y = 0; y <= 3; y++) level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
            String lily = element == 1 ? "frost_lily" : ELEMENTS[element] + "_lily";
            ConfiguredFeature<?, ?> feature = configured.get(ResourceLocation.parse("iceandfire:" + lily));
            helper.assertTrue(feature != null, "Configured lily feature is missing: " + lily);
            helper.assertTrue(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + lily))
                    .defaultBlockState().canSurvive(level, pos),
                "Prepared TFC soil cannot support " + lily + " at " + pos + " ground="
                    + BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.below()).getBlock()));
            helper.assertTrue(new ElementalPlacement(ClimateSelection.Element.values()[element], true)
                .getPositions(new PlacementContext(level, generator, Optional.empty()), RandomSource.create(0), pos)
                .findAny().isPresent(), "Candidate filter rejected prepared soil for " + lily);
            boolean placed = false;
            for (int attempt = 0; attempt < 64 && !placed; attempt++) {
                feature.place(level, generator, RandomSource.create(attempt), pos);
                for (int x = -7; x <= 7 && !placed; x++) for (int z = -7; z <= 7; z++) {
                    if (level.getBlockState(pos.offset(x, 0, z)).is(
                        BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + lily)))) {
                        placed = true;
                        break;
                    }
                }
            }
            helper.assertTrue(placed, "Natural lily feature did not place on TFC soil: " + lily);
            BlockPos manual = strongClimate(helper, generator, (element + 1) % 3);
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                level.setBlock(manual.offset(x, -1, z), soil, 3);
                for (int y = 0; y <= 3; y++) level.setBlock(manual.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
            var placedFeature = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(ResourceLocation.parse("iceandfire:" + lily));
            helper.assertTrue(placedFeature != null, "Placed lily feature is missing: " + lily);
            for (int attempt = 0; attempt < 64; attempt++)
                placedFeature.place(level, generator, RandomSource.create(attempt), manual);
            for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++)
                helper.assertTrue(!level.getBlockState(manual.offset(x, 0, z)).is(
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + lily))),
                    "Lily generated in another element's climate: " + lily);
            var player = FakePlayerFactory.getMinecraft(level);
            ItemStack lilyItem = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + lily)));
            player.setItemInHand(InteractionHand.MAIN_HAND, lilyItem);
            lilyItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(manual.below()), Direction.UP, manual.below(), false)));
            helper.assertTrue(level.getBlockState(manual).is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + lily))),
                "Manual lily planting was blocked by mismatched climate: " + lily);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skeletonFeatureSuppliesBone(GameTestHelper helper) {
        var level = helper.getLevel();
        TFCChunkGenerator generator = tfcGenerator(helper);
        var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        RandomSource certain = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
            new Class<?>[] {RandomSource.class}, (proxy, method, args) -> switch (method.getName()) {
                case "nextDouble" -> 0.0;
                case "nextInt" -> 0;
                case "nextLong" -> 0L;
                case "nextFloat" -> 0f;
                case "nextBoolean" -> false;
                default -> throw new AssertionError("Unexpected skeleton RNG call: " + method.getName());
            });
        List<DragonBaseEntity> harvested = new ArrayList<>();
        for (int element = 0; element < 3; element++) {
            String name = ELEMENTS[element];
            BlockPos pos = helper.absolutePos(new BlockPos(element * 12, 80, 0));
            level.setBlock(pos.offset(8, -1, 8), Blocks.STONE.defaultBlockState(), 3);
            ConfiguredFeature<?, ?> feature = configured.get(ResourceLocation.parse(
                "iceandfire:spawn_dragon_skeleton_" + name));
            helper.assertTrue(feature != null, "Configured skeleton feature is missing: " + name);
            feature.place(level, generator, certain, pos);
            var entities = level.getEntitiesOfClass(DragonBaseEntity.class,
                AABB.encapsulatingFullBlocks(new BlockPos(pos.getX() - 2, -64, pos.getZ() - 2),
                    new BlockPos(pos.getX() + 18, 320, pos.getZ() + 18)));
            var dragon = entities.stream().filter(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                .equals(ResourceLocation.parse("iceandfire:" + name + "_dragon"))).findFirst().orElse(null);
            helper.assertTrue(dragon != null && dragon.isModelDead() && dragon.getDeathStage() > 0,
                "Original feature did not create a harvestable " + name + " skeleton at " + pos
                    + " nearby=" + entities.stream().map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())).toList());
            helper.assertTrue(dragon.getDeadLootTable().getPath().endsWith("_dragon_skeleton")
                && dragon.getItemFromLootTable().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:dragonbone"))),
                "Original skeleton loot no longer supplies dragonbone: " + name);
            dragon.setAgeInTicks(2400000);
            dragon.setDeathStage(1);
            var player = FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (int stage = 0; stage < 32 && !dragon.isRemoved(); stage++) {
                int before = dragon.getDeathStage();
                dragon.mobInteract(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(dragon.isRemoved() || dragon.getDeathStage() > before,
                    "Skeleton could not be harvested: " + name);
            }
            harvested.add(dragon);
        }
        helper.runAfterDelay(2, () -> {
            for (DragonBaseEntity dragon : harvested) {
                var drops = level.getEntitiesOfClass(ItemEntity.class, dragon.getBoundingBox().inflate(8));
                helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:dragonbone")))),
                    "Skeleton did not drop dragonbone: " + dragon.getType());
                helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(dragon.getSkull())),
                    "Skeleton did not drop its skull: " + dragon.getType());
            }
            helper.succeed();
        });
    }

    private static BlockPos strongClimate(GameTestHelper helper, TFCChunkGenerator generator, int element) {
        long seed = helper.getLevel().getSeed();
        var selection = DragonClimateWorldgen.selection(seed, generator);
        for (int index = 0; index < 4096; index++) {
            long hash = ClimateSelection.splitMix64(seed + index * 0x9E3779B97F4A7C15L);
            int x = (short) (hash & 0xffff);
            int z = (short) ((hash >>> 16) & 0xffff);
            double[] scores = selection.scores(DragonClimateWorldgen.climate(seed, generator, x, z));
            if (scores[element] > scores[(element + 1) % 3] + 0.5
                && scores[element] > scores[(element + 2) % 3] + 0.5)
                return new BlockPos(x * 16 + 8, 81, z * 16 + 8);
        }
        throw new AssertionError("No TFC climate sample favors " + ELEMENTS[element]);
    }

    private static TFCChunkGenerator tfcGenerator(GameTestHelper helper) {
        var preset = helper.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET)
            .get(ResourceLocation.parse("tfc:overworld"));
        return (TFCChunkGenerator) preset.overworld().orElseThrow().generator();
    }

    private static Set<ResourceLocation> features(Biome biome) {
        return biome.getGenerationSettings().features().stream().flatMap(holders -> holders.stream())
            .map(holder -> holder.unwrapKey())
            .flatMap(Optional::stream)
            .map(key -> key.location())
            .collect(Collectors.toSet());
    }

    private static JsonObject json(GameTestHelper helper, String id) throws Exception {
        try (var reader = helper.getLevel().getServer().getResourceManager()
            .getResource(ResourceLocation.parse(id)).orElseThrow().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static boolean sameAsIaf(GameTestHelper helper, String path) throws Exception {
        var original = ModList.get().getModFileById("iceandfire").getFile()
            .findResource("data/iceandfire/" + path);
        try (var active = helper.getLevel().getServer().getResourceManager()
            .getResource(ResourceLocation.parse("iceandfire:" + path)).orElseThrow().open()) {
            return Arrays.equals(Files.readAllBytes(original), active.readAllBytes());
        }
    }

    private static JsonObject iafJson(String path) throws Exception {
        var original = ModList.get().getModFileById("iceandfire").getFile()
            .findResource("data/iceandfire/" + path);
        try (var reader = Files.newBufferedReader(original)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
