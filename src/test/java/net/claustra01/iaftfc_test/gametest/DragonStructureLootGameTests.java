package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.item.block.PileBlock;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.registry.tag.IafBlockTags;
import com.iafenvoy.iceandfire.world.structure.FireDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.LightningDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.FireDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.LightningDragonCaveStructure;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.claustra01.iaftfc.worldgen.DragonCaveOres;
import net.claustra01.iaftfc.worldgen.DragonChests;
import net.claustra01.iaftfc.worldgen.DragonClimate;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonStructureLootGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final List<String> ORDINARY_INGOTS = List.of("copper", "bronze", "bismuth_bronze", "black_bronze",
        "wrought_iron");
    private static final List<String> ORDINARY_EQUIPMENT = List.of("copper", "bronze", "bismuth_bronze",
        "black_bronze");
    private static final Set<String> UNFINISHED_SHAPES = Set.of("sword_blade", "javelin_head", "mace_head",
        "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots");
    /** CE dragon rewards without a replacement rule, kept as rolled. */
    private static final Set<String> KEPT_REWARDS = Set.of("minecraft:bone", "minecraft:book",
        "minecraft:enchanted_book", "iceandfire:manuscript", "minecraft:skeleton_skull", "minecraft:cobweb",
        "minecraft:packed_ice", "iceandfire:crackled_gravel", "minecraft:experience_bottle", "minecraft:flint",
        "iceandfire:cannoli", "minecraft:golden_apple", "minecraft:enchanted_golden_apple");

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonChestsAndLootProfiles(GameTestHelper helper) {
        int x = 1;
        for (DragonClimate.Element element : DragonClimate.Element.values()) {
            Block chest = DragonChests.block(element);
            helper.assertTrue(chest instanceof ChestBlock, "Element chest must retain vanilla chest behavior");
            BlockPos pos = new BlockPos(x++, 2, 1);
            helper.setBlock(pos, chest);
            helper.assertTrue(helper.getBlockEntity(pos) instanceof ChestBlockEntity,
                "Element chest must create a loot-capable chest block entity");
        }
        helper.assertValueEqual(BuiltInRegistries.BLOCK.getKey(DragonChests.block(DragonClimate.Element.FIRE)),
            ResourceLocation.parse("tfc:wood/chest/chestnut"), "Fire structures must use Chestnut");
        helper.assertValueEqual(BuiltInRegistries.BLOCK.getKey(DragonChests.block(DragonClimate.Element.ICE)),
            ResourceLocation.parse("tfc:wood/chest/white_cedar"), "Ice structures must use White Cedar");
        helper.assertValueEqual(BuiltInRegistries.BLOCK.getKey(DragonChests.block(DragonClimate.Element.LIGHTNING)),
            ResourceLocation.parse("tfc:wood/chest/blackwood"), "Lightning structures must use Blackwood");

        List<CavePiece> femalePieces = List.of(
            new TestFireCave(false), new TestIceCave(false), new TestLightningCave(false));
        List<CavePiece> malePieces = List.of(
            new TestFireCave(true), new TestIceCave(true), new TestLightningCave(true));
        List<String> elements = List.of("fire", "ice", "lightning");
        for (int index = 0; index < elements.size(); index++) {
            verifyCaveChest(helper, femalePieces.get(index), elements.get(index), false, new BlockPos(index + 1, 2, 3));
            verifyCaveChest(helper, malePieces.get(index), elements.get(index), true, new BlockPos(index + 1, 2, 4));
        }
        verifyRoostChest(helper, new TestFireRoost(), "fire", new BlockPos(40, 3, 0));
        verifyRoostChest(helper, new TestIceRoost(), "ice", new BlockPos(100, 3, 0));
        verifyRoostChest(helper, new TestLightningRoost(), "lightning", new BlockPos(160, 3, 0));

        for (String element : elements) {
            verifyTable(helper, element, "roost", ORDINARY_INGOTS, ORDINARY_EQUIPMENT, false);
            verifyTable(helper, element, "female_cave", List.of("wrought_iron", "steel"), List.of("wrought_iron"),
                false);
            verifyTable(helper, element, "male_cave", List.of("steel", "black_steel"), List.of("steel"), true);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonCaveOresMatchNearbyRock(GameTestHelper helper) {
        BlockPos origin = new BlockPos(5, 5, 5);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -4, -4), origin.offset(4, 4, 4))) {
            helper.setBlock(pos, Blocks.AIR);
        }
        helper.setBlock(origin.offset(2, 0, 0), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/raw/granite")));
        helper.assertValueEqual(DragonCaveOres.findRock(helper.getLevel(), helper.absolutePos(origin)).orElseThrow(),
            "granite", "Nearby raw rock must select the cave geology");

        for (Rock rock : Rock.values()) {
            String rockName = rock.getSerializedName();
            for (DragonClimate.Element element : DragonClimate.Element.values()) {
                assertTfcOre(helper, DragonCaveOres.select(rockName, element, DragonCaveOres.Category.GEM)
                    .orElseThrow(), elementGem(element), rockName);
                assertTfcOre(helper, DragonCaveOres.select(rockName, element, DragonCaveOres.Category.METAL)
                    .orElseThrow(), elementMetal(element), rockName);
            }
        }
        double previousRatio = IafCommonConfig.INSTANCE.dragon.generateOreRatio.getValue();
        try {
            IafCommonConfig.INSTANCE.dragon.generateOreRatio.setValue(1.0D);
            for (String rock : List.of("granite", "basalt")) {
                for (int index = 0; index < 3; index++) {
                    CavePiece piece = switch (index) {
                        case 0 -> new TestFireCave(false);
                        case 1 -> new TestIceCave(false);
                        default -> new TestLightningCave(false);
                    };
                    Set<BlockPos> shell = new HashSet<>();
                    int x = 30 + index * 40 + (rock.equals("basalt") ? 140 : 0);
                    for (int dx = 0; dx < 12; dx++) {
                        for (int dz = 0; dz < 12; dz++) {
                            BlockPos pos = helper.absolutePos(new BlockPos(x + dx, 5, dz));
                            helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(
                                ResourceLocation.parse("tfc:rock/raw/" + rock)).defaultBlockState(), 3);
                            shell.add(pos);
                        }
                    }
                    piece.createShell(helper.getLevel(), RandomSource.create(41L), shell);
                    DragonClimate.Element element = DragonClimate.Element.values()[index];
                    String gem = elementGem(element);
                    String metal = elementMetal(element);
                    int gemCount = 0;
                    int metalCount = 0;
                    for (BlockPos pos : shell) {
                        BlockState state = helper.getLevel().getBlockState(pos);
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                        helper.assertTrue(!state.is(IafBlockTags.DRAGON_CAVE_COMMON_ORES)
                            && !state.is(IafBlockTags.DRAGON_CAVE_UNCOMMON_ORES)
                            && !state.is(IafBlockTags.DRAGON_CAVE_RARE_ORES), "CE cave ore must not appear");
                        if (id.getNamespace().equals("tfc")) {
                            assertTfcOre(helper, state, null, rock);
                            if (id.getPath().startsWith("ore/" + gem + "/")) {
                                gemCount++;
                            } else if (id.getPath().startsWith("ore/" + metal + "/")) {
                                metalCount++;
                            } else {
                                helper.fail("Shell ore must be the element gem or rich element metal: " + id);
                            }
                        } else {
                            helper.assertTrue(id.getNamespace().equals("iceandfire")
                                && (id.getPath().contains("stone") || id.getPath().contains("cobblestone")),
                                "Shell must contain only the element palette or TFC ore");
                        }
                    }
                    helper.assertTrue(gemCount > 0, "Element gem must appear in the generated shell");
                    helper.assertTrue(metalCount > 0, "Rich element metal must appear in the generated shell");
                }
            }
        } finally {
            IafCommonConfig.INSTANCE.dragon.generateOreRatio.setValue(previousRatio);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void pileLayersDropMatchingSmallOre(GameTestHelper helper) {
        List<String> metals = List.of("gold", "silver", "copper");
        List<String> ores = List.of("native_gold", "native_silver", "native_copper");
        for (int index = 0; index < metals.size(); index++) {
            Block pile = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + metals.get(index) + "_pile"));
            Item expected = BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:ore/small_" + ores.get(index)));
            for (int layers = 1; layers <= 8; layers++) {
                int expectedCount = (layers + 1) / 2;
                BlockState state = pile.defaultBlockState().setValue(PileBlock.LAYERS, layers);
                List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                    null);
                helper.assertValueEqual(drops.stream().filter(stack -> stack.is(expected)).mapToInt(ItemStack::getCount).sum(),
                    expectedCount, "Pile must drop half its layers, rounded up, as TFC small ore");
                helper.assertValueEqual(drops.stream().mapToInt(ItemStack::getCount).sum(), expectedCount,
                    "Pile must not retain a second nugget drop");
            }
        }
        helper.succeed();
    }

    /**
     * Compares the unmodified CE roll ({@code getRandomItemsRaw} skips GLMs) with the GLM result of the same seed.
     * Both pass through the vanilla max-stack splitter, which already uses TFC stack sizes before GLMs run.
     */
    @SuppressWarnings("deprecation")
    private static void verifyTable(GameTestHelper helper, String element, String variant, List<String> ingots,
        List<String> equipmentMetals, boolean male) {
        String tableName = element + "_dragon_" + variant;
        ResourceLocation tableId = ResourceLocation.parse("iceandfire:chest/" + tableName);
        ResourceLocation file = ResourceLocation.parse("iceandfire:loot_table/chest/" + tableName + ".json");
        helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResourceStack(file).stream()
            .noneMatch(resource -> resource.sourcePackId().contains("iaftfc")), "CE loot table must not be overridden");
        LootTable loaded = helper.getLevel().getServer().reloadableRegistries()
            .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, tableId));
        helper.assertTrue(loaded != LootTable.EMPTY, "CE loot table must be loaded by the server");
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))))
            .create(LootContextParamSets.CHEST);
        Set<String> seenIngots = new HashSet<>();
        Set<String> seenOutputs = new HashSet<>();
        Set<String> seenEquipmentMetals = new HashSet<>();
        Set<String> seenSwordShapes = new HashSet<>();
        Set<String> enchantedFinished = new HashSet<>();
        int unfinished = 0;
        int finished = 0;
        for (long seed = 1; seed <= 1500; seed++) {
            List<ItemStack> raw = new ArrayList<>();
            loaded.getRandomItemsRaw(new LootContext.Builder(params).withOptionalRandomSeed(seed)
                .create(Optional.empty()), LootTable.createStackSplitter(helper.getLevel(), raw::add));
            List<ItemStack> modified = loaded.getRandomItems(params, seed);
            int index = 0;
            for (ItemStack source : raw) {
                String sourceId = id(source);
                Set<String> allowed = allowedOutputs(sourceId, element, variant, ingots, equipmentMetals);
                helper.assertTrue(index < modified.size(), "Replacement must keep every rolled stack: " + sourceId);
                ItemStack first = modified.get(index);
                if (allowed.isEmpty()) {
                    helper.assertTrue(ItemStack.matches(source, first), "Unmapped CE reward must remain: " + sourceId);
                    index++;
                    continue;
                }
                String output = id(first);
                helper.assertTrue(allowed.contains(output), sourceId + " must not become " + output);
                seenOutputs.add(output);
                int total = 0;
                while (total < source.getCount()) {
                    helper.assertTrue(index < modified.size() && modified.get(index).is(first.getItem()),
                        "A replaced stack must be split into the same item: " + sourceId);
                    ItemStack part = modified.get(index++);
                    helper.assertTrue(part.getCount() <= part.getMaxStackSize(), "Split stack exceeds its maximum");
                    total += part.getCount();
                }
                helper.assertValueEqual(total, source.getCount(), "Replacement must keep the rolled count: " + sourceId);
                String path = output.substring(output.indexOf(':') + 1);
                if (path.startsWith("metal/ingot/")) {
                    seenIngots.add(path.substring("metal/ingot/".length()));
                }
                if (isEquipmentSource(sourceId)) {
                    String[] parts = path.split("/");
                    seenEquipmentMetals.add(parts[2]);
                    boolean isUnfinished = UNFINISHED_SHAPES.contains(parts[1]);
                    boolean enchanted = !first.getEnchantments().isEmpty();
                    if (isUnfinished) {
                        unfinished++;
                        helper.assertTrue(!enchanted, "Work-in-progress equipment must not be enchanted");
                    } else {
                        finished++;
                        if (!male) {
                            helper.assertTrue(!enchanted, "Only male cave finished equipment is enchanted");
                        } else if (!enchanted) {
                            // TFC javelins and maces have no primary enchantments, as with CE's own roll.
                            helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                                .holders().noneMatch(first::isPrimaryItemFor),
                                "Enchantable male cave equipment must be re-enchanted: " + output);
                        } else {
                            enchantedFinished.add(parts[1]);
                        }
                    }
                    if (sourceId.endsWith("_sword")) {
                        seenSwordShapes.add(parts[1]);
                    }
                }
            }
            helper.assertValueEqual(index, modified.size(), "Replacement must not add extra stacks");
        }
        helper.assertTrue(seenIngots.containsAll(ingots), "Every profile ingot must be rolled: " + tableName);
        String elementMetal = switch (element) {
            case "fire" -> "gold";
            case "ice" -> "silver";
            default -> "copper";
        };
        helper.assertTrue(seenIngots.contains(elementMetal), "Element ingot must be rolled: " + tableName);
        helper.assertTrue(seenOutputs.contains("tfc:ore/small_native_" + elementMetal),
            "Element small ore must be rolled: " + tableName);
        helper.assertTrue(seenEquipmentMetals.equals(Set.copyOf(equipmentMetals)),
            "Equipment must use exactly the profile metals: " + tableName);
        helper.assertTrue(seenSwordShapes.equals(Set.of("sword_blade", "javelin_head", "mace_head", "sword",
            "javelin", "mace")), "Sword family must use every TFC weapon shape: " + tableName);
        if (male) {
            helper.assertTrue(enchantedFinished.containsAll(Set.of("sword", "helmet", "chestplate", "greaves", "boots")),
                "Male cave finished swords and armor must be re-enchanted: " + tableName);
        }
        double ratio = (double) unfinished / (unfinished + finished);
        helper.assertTrue(ratio > 0.6 && ratio < 0.73,
            "Work-in-progress and finished equipment must follow 2:1, was " + ratio + " in " + tableName);
    }

    private static Set<String> allowedOutputs(String source, String element, String variant, List<String> ingots,
        List<String> equipmentMetals) {
        String fixed = switch (element + "|" + source) {
            // CE 2.2's ice male cave rolls Gold in place of the ice element Silver.
            case "ice|minecraft:gold_ingot" -> variant.equals("male_cave") ? "tfc:metal/ingot/silver" : null;
            case "ice|minecraft:gold_nugget" -> variant.equals("male_cave") ? "tfc:ore/small_native_silver" : null;
            case "fire|minecraft:emerald" -> "tfc:gem/ruby";
            case "fire|minecraft:gold_ingot" -> "tfc:metal/ingot/gold";
            case "ice|iceandfire:sapphire_gem" -> "tfc:gem/sapphire";
            case "ice|iceandfire:silver_ingot" -> "tfc:metal/ingot/silver";
            case "ice|iceandfire:silver_nugget" -> "tfc:ore/small_native_silver";
            case "lightning|minecraft:amethyst_shard" -> "tfc:gem/topaz";
            case "lightning|minecraft:copper_ingot" -> "tfc:metal/ingot/copper";
            case "lightning|iceandfire:copper_nugget" -> "tfc:ore/small_native_copper";
            default -> null;
        };
        if (fixed != null) {
            return Set.of(fixed);
        }
        Set<String> values = new HashSet<>();
        switch (source) {
            case "minecraft:diamond", "minecraft:emerald", "minecraft:amethyst_shard", "iceandfire:sapphire_gem" ->
                List.of("amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz")
                    .forEach(gem -> values.add("tfc:gem/" + gem));
            case "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:copper_ingot", "iceandfire:silver_ingot" ->
                ingots.forEach(metal -> values.add("tfc:metal/ingot/" + metal));
            case "minecraft:gold_nugget" -> values.add("tfc:ore/small_native_gold");
            case "minecraft:chain" -> values.add("tfc:metal/chain/wrought_iron");
            case "minecraft:obsidian" -> values.addAll(List.of("tfc:powder/flux", "minecraft:glass",
                "minecraft:glass_pane", "tfc:kaolin_clay", "tfc:powder/wood_ash", "tfc:powder/saltpeter",
                "tfc:powder/sylvite"));
            default -> {
                List<String> shapes = equipmentShapes(source);
                if (shapes != null) {
                    shapes.forEach(shape -> equipmentMetals.forEach(metal ->
                        values.add("tfc:metal/" + shape + "/" + metal)));
                } else if (!KEPT_REWARDS.contains(source)) {
                    throw new IllegalStateException("CE dragon reward has no replacement rule: " + source);
                }
            }
        }
        return values;
    }

    private static List<String> equipmentShapes(String source) {
        if (!isEquipmentSource(source)) {
            return null;
        }
        if (source.endsWith("_sword")) {
            return List.of("sword_blade", "javelin_head", "mace_head", "sword", "javelin", "mace");
        }
        String part = source.substring(source.lastIndexOf('_') + 1);
        String tfcPart = part.equals("leggings") ? "greaves" : part;
        return List.of("unfinished_" + tfcPart, tfcPart);
    }

    private static boolean isEquipmentSource(String source) {
        return source.matches("minecraft:(iron|diamond)_(sword|helmet|chestplate|leggings|boots)")
            || source.matches("iceandfire:(silver|copper)_sword")
            || source.matches("iceandfire:armor_(silver|copper)_metal_(helmet|chestplate|leggings|boots)");
    }

    private static void verifyFilledChest(GameTestHelper helper, ChestBlockEntity chest, String element) {
        chest.unpackLootTable(null);
        boolean filled = false;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            filled = true;
            String id = id(stack);
            helper.assertTrue(KEPT_REWARDS.contains(id) || id.startsWith("tfc:") || id.startsWith("minecraft:glass"),
                "Chest fill must pass CE rewards through the TFC replacement: " + id + " in " + element);
        }
        helper.assertTrue(filled, "Dragon chest must be filled from its CE loot table");
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void verifyRoostChest(GameTestHelper helper, Object piece, String element, BlockPos relativeOrigin) {
        BlockPos origin = helper.absolutePos(relativeOrigin);
        try {
            Method decoration = Class.forName("com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece")
                .getDeclaredMethod("generateDecoration",
                WorldGenLevel.class, BlockPos.class, RandomSource.class, int.class, boolean.class);
            decoration.setAccessible(true);
            for (int attempt = 0; attempt < 16; attempt++) {
                decoration.invoke(piece, helper.getLevel(), origin, RandomSource.create(100L + attempt), 12, false);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to generate CE roost decoration", exception);
        }
        int found = 0;
        for (BlockPos column : BlockPos.betweenClosed(origin.offset(-10, 0, -10), origin.offset(10, 0, 10))) {
            BlockPos pos = helper.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, column).below();
            if (helper.getLevel().getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                helper.assertValueEqual(BuiltInRegistries.BLOCK.getKey(chest.getBlockState().getBlock()),
                    ResourceLocation.parse("tfc:wood/chest/" + switch (element) {
                        case "fire" -> "chestnut";
                        case "ice" -> "white_cedar";
                        default -> "blackwood";
                    }), "Roost decoration must place the element TFC chest");
                helper.assertValueEqual(chest.getLootTable().location(),
                    ResourceLocation.parse("iceandfire:chest/" + element + "_dragon_roost"),
                    "Roost chest must retain the CE loot table ID");
                verifyFilledChest(helper, chest, element);
                found++;
            }
        }
        helper.assertTrue(found > 0, "Roost decoration must place a chest");
    }

    private static void verifyCaveChest(GameTestHelper helper, CavePiece piece,
        String element, boolean male, BlockPos pos) {
        piece.placeChest(helper.getLevel(), helper.absolutePos(pos), chestRandom());
        helper.assertBlockPresent(DragonChests.block(switch (element) {
            case "fire" -> DragonClimate.Element.FIRE;
            case "ice" -> DragonClimate.Element.ICE;
            default -> DragonClimate.Element.LIGHTNING;
        }), pos);
        ChestBlockEntity chest = (ChestBlockEntity)helper.getBlockEntity(pos);
        helper.assertValueEqual(chest.getLootTable().location(), ResourceLocation.parse("iceandfire:chest/" + element
            + "_dragon_" + (male ? "male_cave" : "female_cave")), "Cave chest must retain its CE loot table ID");
        verifyFilledChest(helper, chest, element);
    }

    private static RandomSource chestRandom() {
        for (long seed = 0; ; seed++) {
            RandomSource random = RandomSource.create(seed);
            if (random.nextInt(99) == 60) {
                return RandomSource.create(seed);
            }
        }
    }

    private static String elementGem(DragonClimate.Element element) {
        return switch (element) {
            case FIRE -> "ruby";
            case ICE -> "sapphire";
            case LIGHTNING -> "topaz";
        };
    }

    private static String elementMetal(DragonClimate.Element element) {
        return switch (element) {
            case FIRE -> "rich_native_gold";
            case ICE -> "rich_native_silver";
            case LIGHTNING -> "rich_native_copper";
        };
    }

    private static void assertTfcOre(GameTestHelper helper, BlockState state, String ore, String rock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        helper.assertValueEqual(id.getNamespace(), "tfc", "Cave resources must be registered TFC blocks");
        helper.assertTrue(id.getPath().startsWith("ore/") && id.getPath().endsWith("/" + rock),
            "Cave ore must match its nearby rock");
        if (ore != null) {
            helper.assertTrue(id.getPath().startsWith("ore/" + ore + "/"), "Element ore must match its dragon");
        }
    }

    private DragonStructureLootGameTests() {
    }

    private interface CavePiece {
        void placeChest(LevelAccessor level, BlockPos pos, RandomSource random);
        void createShell(LevelAccessor level, RandomSource random, Set<BlockPos> shell);
    }

    private static final class TestFireCave extends FireDragonCaveStructure.FireDragonCavePiece implements CavePiece {
        private TestFireCave(boolean male) {
            super(0, new BoundingBox(0, 0, 0, 0, 0, 0), male, BlockPos.ZERO, 0, 0L);
        }

        @Override
        public void placeChest(LevelAccessor level, BlockPos pos, RandomSource random) {
            setGoldPile(level, pos, random);
        }

        @Override
        public void createShell(LevelAccessor level, RandomSource random, Set<BlockPos> shell) {
            super.createShell(level, random, shell);
        }
    }

    private static final class TestIceCave extends IceDragonCaveStructure.IceDragonCavePiece implements CavePiece {
        private TestIceCave(boolean male) {
            super(0, new BoundingBox(0, 0, 0, 0, 0, 0), male, BlockPos.ZERO, 0, 0L);
        }

        @Override
        public void placeChest(LevelAccessor level, BlockPos pos, RandomSource random) {
            setGoldPile(level, pos, random);
        }

        @Override
        public void createShell(LevelAccessor level, RandomSource random, Set<BlockPos> shell) {
            super.createShell(level, random, shell);
        }
    }

    private static final class TestLightningCave extends LightningDragonCaveStructure.LightningDragonCavePiece
        implements CavePiece {
        private TestLightningCave(boolean male) {
            super(0, new BoundingBox(0, 0, 0, 0, 0, 0), male, BlockPos.ZERO, 0, 0L);
        }

        @Override
        public void placeChest(LevelAccessor level, BlockPos pos, RandomSource random) {
            setGoldPile(level, pos, random);
        }

        @Override
        public void createShell(LevelAccessor level, RandomSource random, Set<BlockPos> shell) {
            super.createShell(level, random, shell);
        }
    }

    private static final class TestFireRoost extends FireDragonRoostStructure.FireDragonRoostPiece {
        private TestFireRoost() {
            super(0, new BoundingBox(0, 0, 0, 0, 0, 0), Blocks.GOLD_BLOCK, false);
        }
    }

    private static final class TestIceRoost extends IceDragonRoostStructure.IceDragonRoostPiece {
        private TestIceRoost() {
            super(0, new BoundingBox(0, 0, 0, 0, 0, 0), Blocks.GOLD_BLOCK, false);
        }
    }

    private static final class TestLightningRoost extends LightningDragonRoostStructure.LightningDragonRoostPiece {
        private TestLightningRoost() {
            super(0, new BoundingBox(0, 0, 0, 0, 0, 0), Blocks.GOLD_BLOCK, false);
        }
    }
}
