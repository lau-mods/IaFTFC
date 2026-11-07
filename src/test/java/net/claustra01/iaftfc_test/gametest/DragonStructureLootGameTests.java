package net.claustra01.iaftfc_test.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iafenvoy.iceandfire.item.block.PileBlock;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.registry.tag.IafBlockTags;
import com.iafenvoy.iceandfire.world.structure.FireDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.LightningDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.FireDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.LightningDragonCaveStructure;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
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
            verifyTable(helper, element, "roost", List.of("copper", "bronze", "bismuth_bronze", "black_bronze"),
                false);
            verifyTable(helper, element, "female_cave", List.of("wrought_iron"), false);
            verifyTable(helper, element, "male_cave", List.of("steel"), true);
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
                BlockState gem = DragonCaveOres.select(rockName, element, DragonCaveOres.Category.ELEMENT,
                    RandomSource.create(0L)).orElseThrow();
                String expected = switch (element) {
                    case FIRE -> "ruby";
                    case ICE -> "sapphire";
                    case LIGHTNING -> "topaz";
                };
                assertTfcOre(helper, gem, expected, rockName);
            }
            for (DragonCaveOres.Category category : List.of(DragonCaveOres.Category.COMMON,
                DragonCaveOres.Category.UNCOMMON, DragonCaveOres.Category.RARE)) {
                BlockState ore = DragonCaveOres.select(rockName, DragonClimate.Element.FIRE, category,
                    RandomSource.create(rock.ordinal() * 31L + category.ordinal())).orElseThrow();
                assertTfcOre(helper, ore, null, rockName);
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
                    String gem = List.of("ruby", "sapphire", "topaz").get(index);
                    int gemCount = 0;
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
                            }
                        } else {
                            helper.assertTrue(id.getNamespace().equals("iceandfire")
                                && (id.getPath().contains("stone") || id.getPath().contains("cobblestone")),
                                "Shell must contain only the element palette or TFC ore");
                        }
                    }
                    helper.assertTrue(gemCount > 0, "Element gem must appear in the generated shell");
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
            for (int layers : List.of(1, 8)) {
                BlockState state = pile.defaultBlockState().setValue(PileBlock.LAYERS, layers);
                List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                    null);
                helper.assertValueEqual(drops.stream().filter(stack -> stack.is(expected)).mapToInt(ItemStack::getCount).sum(),
                    layers, "Pile layers must equal the TFC small ore count");
                helper.assertValueEqual(drops.stream().mapToInt(ItemStack::getCount).sum(), layers,
                    "Pile must not retain a second nugget drop");
            }
        }
        helper.succeed();
    }

    private static void verifyTable(GameTestHelper helper, String element, String variant, List<String> metals,
        boolean enchanted) {
        String tableName = element + "_dragon_" + variant;
        JsonObject root = resource(helper, "iceandfire:loot_table/chest/" + tableName + ".json");
        String sourcePath = "/data/iceandfire/loot_table/chest/" + tableName + ".json";
        JsonObject original;
        try (var stream = FireDragonCaveStructure.class.getResourceAsStream(sourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing CE loot table " + sourcePath);
            }
            original = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read CE loot table " + sourcePath, exception);
        }
        JsonObject pool = root.getAsJsonArray("pools").get(0).getAsJsonObject();
        JsonObject originalPool = original.getAsJsonArray("pools").get(0).getAsJsonObject();
        helper.assertValueEqual(root.getAsJsonArray("pools").size(), original.getAsJsonArray("pools").size(),
            "CE pool count must be retained");
        helper.assertValueEqual(pool.get("name").getAsString(), originalPool.get("name").getAsString(),
            "CE pool name must be retained");
        helper.assertValueEqual(pool.getAsJsonObject("rolls").get("min").getAsInt(),
            originalPool.getAsJsonObject("rolls").get("min").getAsInt(), "CE minimum rolls must be retained");
        helper.assertValueEqual(pool.getAsJsonObject("rolls").get("max").getAsInt(), enchanted ? 6 : 4,
            "CE roll range must be retained");
        JsonArray entries = pool.getAsJsonArray("entries");
        int originalWeight = 0;
        for (JsonElement value : originalPool.getAsJsonArray("entries")) {
            originalWeight += value.getAsJsonObject().get("weight").getAsInt();
        }
        int actualWeight = 0;
        for (JsonElement value : entries) {
            actualWeight += value.getAsJsonObject().get("weight").getAsInt();
        }
        helper.assertValueEqual(actualWeight, originalWeight, "CE total entry weight must be retained");
        helper.assertValueEqual(actualWeight, switch (element) {
            case "fire" -> enchanted ? 212 : 165;
            case "ice" -> enchanted ? 212 : 175;
            default -> enchanted ? 226 : 192;
        }, "CE 2.2 table weight must be retained");
        for (JsonElement value : originalPool.getAsJsonArray("entries")) {
            JsonObject source = value.getAsJsonObject();
            String name = source.get("name").getAsString();
            if (name.endsWith("_sword") || name.endsWith("_helmet") || name.endsWith("_chestplate")
                || name.endsWith("_leggings") || name.endsWith("_boots")) {
                continue;
            }
            String mapped = switch (name) {
                case "minecraft:diamond" -> "tfc:gem/diamond";
                case "minecraft:emerald", "iceandfire:sapphire_gem", "minecraft:amethyst_shard" ->
                    "tfc:gem/" + switch (element) {
                        case "fire" -> "ruby";
                        case "ice" -> "sapphire";
                        default -> "topaz";
                    };
                case "minecraft:chain" -> "tfc:metal/chain/wrought_iron";
                case "minecraft:gold_nugget", "iceandfire:silver_nugget", "iceandfire:copper_nugget" ->
                    "tfc:ore/small_native_" + switch (element) {
                        case "fire" -> "gold";
                        case "ice" -> "silver";
                        default -> "copper";
                    };
                default -> name;
            };
            if (name.endsWith("_ingot")) {
                continue;
            }
            JsonObject actual = null;
            for (JsonElement candidate : entries) {
                JsonObject entry = candidate.getAsJsonObject();
                if (entry.has("name") && entry.get("name").getAsString().equals(mapped)) {
                    actual = entry;
                    break;
                }
            }
            helper.assertTrue(actual != null, "CE reward must remain: " + name);
            helper.assertValueEqual(actual.get("weight").getAsInt(), source.get("weight").getAsInt(),
                "CE reward weight must remain: " + name);
            JsonArray sourceFunctions = source.has("functions") ? source.getAsJsonArray("functions") : new JsonArray();
            JsonArray actualFunctions = actual.has("functions") ? actual.getAsJsonArray("functions") : new JsonArray();
            helper.assertValueEqual(actualFunctions.size(), sourceFunctions.size(),
                "CE reward function count must remain: " + name);
            for (int index = 0; index < sourceFunctions.size(); index++) {
                JsonObject originalFunction = sourceFunctions.get(index).getAsJsonObject();
                JsonObject generatedFunction = actualFunctions.get(index).getAsJsonObject();
                helper.assertValueEqual(generatedFunction.get("function").getAsString().replace("minecraft:", ""),
                    originalFunction.get("function").getAsString().replace("minecraft:", ""),
                    "CE reward function must remain: " + name);
                if (originalFunction.has("count")) {
                    JsonObject sourceCount = originalFunction.getAsJsonObject("count");
                    JsonObject actualCount = generatedFunction.getAsJsonObject("count");
                    for (String part : List.of("min", "max", "value")) {
                        if (sourceCount.has(part)) {
                            helper.assertValueEqual(actualCount.get(part).getAsInt(), sourceCount.get(part).getAsInt(),
                                "CE reward count provider must remain: " + name);
                        }
                    }
                }
            }
        }
        Set<String> names = new HashSet<>();
        Set<String> sharedTables = new HashSet<>();
        for (JsonElement value : entries) {
            JsonObject entry = value.getAsJsonObject();
            if (entry.has("name")) {
                String name = entry.get("name").getAsString();
                helper.assertTrue(names.add(name), "A reward entry must not be counted twice in one table: " + name);
                helper.assertTrue(!name.contains("red_steel") && !name.contains("blue_steel"),
                    "Generic loot must not include Red or Blue Steel");
            } else {
                String valueName = entry.get("value").getAsString();
                helper.assertTrue(sharedTables.add(valueName),
                    "A shared reward table must not be counted twice: " + valueName);
                helper.assertValueEqual(entry.get("weight").getAsInt(), enchanted ? 10 : 5,
                    "CE equipment entry weight must be retained");
            }
        }
        helper.assertTrue(names.contains("iceandfire:manuscript"), "CE manuscript reward must remain");
        helper.assertTrue(names.contains("tfc:metal/chain/wrought_iron"), "Chain must use TFC Wrought Iron");
        String gem = switch (element) {
            case "fire" -> "ruby";
            case "ice" -> "sapphire";
            default -> "topaz";
        };
        String nativeOre = switch (element) {
            case "fire" -> "gold";
            case "ice" -> "silver";
            default -> "copper";
        };
        helper.assertTrue(names.contains("tfc:gem/" + gem), "Element gem must use the TFC reward");
        helper.assertTrue(names.contains("tfc:ore/small_native_" + nativeOre), "Nugget must become a TFC small ore");
        String profile = variant.equals("roost") ? "ordinary" : enchanted ? "lategame" : "dangerous";
        verifyEquipmentProfile(helper, profile, metals, enchanted, sharedTables);
        for (String metal : metals) {
            helper.assertTrue(names.contains("tfc:metal/ingot/" + metal), "Profile ingot is missing");
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.parse("iceandfire:chest/" + tableName));
        LootTable loaded = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
        helper.assertTrue(loaded != LootTable.EMPTY, "Generated loot table must be loaded by the server");
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))))
            .create(LootContextParamSets.CHEST);
        boolean foundProfile = false;
        for (long seed = 0; seed < 200 && !foundProfile; seed++) {
            for (ItemStack stack : loaded.getRandomItems(params, seed)) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (metals.stream().anyMatch(metal -> itemId.equals("tfc:metal/ingot/" + metal)
                    || itemId.endsWith("/" + metal) && itemId.startsWith("tfc:metal/"))) {
                    foundProfile = true;
                }
            }
        }
        helper.assertTrue(foundProfile, "Loaded loot table must yield its TFC equipment profile");
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
                found++;
            }
        }
        helper.assertTrue(found > 0, "Roost decoration must place a chest");
    }

    private static void verifyEquipmentProfile(GameTestHelper helper, String profile, List<String> metals,
        boolean enchanted, Set<String> parentTables) {
        for (String equipment : List.of("sword", "helmet", "chestplate", "greaves", "boots")) {
            String table = "iaftfc:shared/equipment/" + profile + "/" + equipment;
            helper.assertTrue(parentTables.contains(table), "Profile equipment table is missing");
            JsonArray entries = resource(helper, "iaftfc:loot_table/shared/equipment/" + profile + "/"
                + equipment + ".json")
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            Set<String> names = new HashSet<>();
            int unfinishedWeight = 0;
            int finishedWeight = 0;
            for (JsonElement value : entries) {
                JsonObject entry = value.getAsJsonObject();
                String name = entry.get("name").getAsString();
                helper.assertTrue(names.add(name), "Shared equipment entry must be unique");
                if (name.contains("unfinished_") || name.contains("sword_blade")) {
                    unfinishedWeight += entry.get("weight").getAsInt();
                    helper.assertTrue(!hasEnchant(entry), "Work-in-progress equipment must not be enchanted");
                } else {
                    finishedWeight += entry.get("weight").getAsInt();
                    helper.assertValueEqual(hasEnchant(entry), enchanted,
                        "Only lategame finished equipment may be enchanted");
                }
                helper.assertTrue(!name.contains("red_steel") && !name.contains("blue_steel"),
                    "Generic loot must not include Red or Blue Steel");
            }
            helper.assertValueEqual(unfinishedWeight, finishedWeight * 2,
                "Work-in-progress and finished equipment weights must have a 2:1 ratio");
            for (String metal : metals) {
                String unfinished = equipment.equals("sword") ? "sword_blade" : "unfinished_" + equipment;
                helper.assertTrue(names.contains("tfc:metal/" + unfinished + "/" + metal),
                    "Profile work-in-progress equipment is missing");
                helper.assertTrue(names.contains("tfc:metal/" + equipment + "/" + metal),
                    "Profile finished equipment is missing");
            }
        }
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
    }

    private static RandomSource chestRandom() {
        for (long seed = 0; ; seed++) {
            RandomSource random = RandomSource.create(seed);
            if (random.nextInt(99) == 60) {
                return RandomSource.create(seed);
            }
        }
    }

    private static boolean hasEnchant(JsonObject entry) {
        if (!entry.has("functions")) {
            return false;
        }
        for (JsonElement function : entry.getAsJsonArray("functions")) {
            if (function.getAsJsonObject().get("function").getAsString().equals("minecraft:enchant_with_levels")) {
                return true;
            }
        }
        return false;
    }

    private static void assertTfcOre(GameTestHelper helper, BlockState state, String ore, String rock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        helper.assertValueEqual(id.getNamespace(), "tfc", "Cave resources must be registered TFC blocks");
        helper.assertTrue(id.getPath().startsWith("ore/") && id.getPath().endsWith("/" + rock),
            "Cave ore must match its nearby rock");
        if (ore != null) {
            helper.assertTrue(id.getPath().startsWith("ore/" + ore + "/"), "Element gem must match its dragon");
        }
    }

    private static JsonObject resource(GameTestHelper helper, String id) {
        try (var reader = new InputStreamReader(helper.getLevel().getServer().getResourceManager()
            .getResource(ResourceLocation.parse(id)).orElseThrow().open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read generated loot table " + id, exception);
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
