package net.claustra01.iaftfc.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.claustra01.iaftfc.loot.EnchantEquipment;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class LootGameTests {
    @GameTest(template = "empty")
    public static void repeatableChestDraws(GameTestHelper helper) {
        var params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))))
            .create(LootContextParamSets.CHEST);
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String place : List.of("roost", "female_cave", "male_cave")) {
                var id = ResourceLocation.parse("iceandfire:chest/" + element + "_dragon_" + place);
                var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, id));
                for (long seed = 1; seed <= 64; seed++) {
                    var first = table.getRandomItems(params, seed);
                    var second = table.getRandomItems(params, seed);
                    helper.assertTrue(first.size() == second.size(), "Loot count changed with the same seed: " + id);
                    for (int index = 0; index < first.size(); index++) {
                        helper.assertTrue(ItemStack.matches(first.get(index), second.get(index)),
                            "Loot item changed with the same seed: " + id + " seed=" + seed);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestAst(GameTestHelper helper) throws Exception {
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String place : List.of("roost", "female_cave", "male_cave")) {
                String path = "chest/" + element + "_dragon_" + place;
                JsonObject original = original(path);
                JsonObject changed = generated(helper, "iceandfire", path);
                JsonObject originalRoot = original.deepCopy();
                JsonObject changedRoot = changed.deepCopy();
                originalRoot.remove("pools");
                changedRoot.remove("pools");
                helper.assertTrue(originalRoot.equals(changedRoot), path + " root changed");
                JsonArray originalPools = original.getAsJsonArray("pools");
                JsonArray changedPools = changed.getAsJsonArray("pools");
                helper.assertTrue(originalPools.size() == changedPools.size(), path + " pool count changed");
                for (int pool = 0; pool < originalPools.size(); pool++) {
                    JsonObject before = originalPools.get(pool).getAsJsonObject();
                    JsonObject after = changedPools.get(pool).getAsJsonObject();
                    JsonObject beforeProperties = before.deepCopy();
                    JsonObject afterProperties = after.deepCopy();
                    beforeProperties.remove("entries");
                    afterProperties.remove("entries");
                    helper.assertTrue(beforeProperties.equals(afterProperties), path + " pool settings changed");
                    JsonArray beforeEntries = before.getAsJsonArray("entries");
                    JsonArray afterEntries = after.getAsJsonArray("entries");
                    helper.assertTrue(beforeEntries.size() == afterEntries.size(), path + " entry count changed");
                    for (int index = 0; index < beforeEntries.size(); index++) {
                        JsonObject source = beforeEntries.get(index).getAsJsonObject();
                        JsonObject result = afterEntries.get(index).getAsJsonObject();
                        String sourceItem = source.get("name").getAsString();
                        helper.assertTrue(!sourceItem.startsWith("iceandfire:dragonarmor_"),
                            "Fixed IaF chest unexpectedly contains a disabled Dragon Armor acquisition: " + sourceItem);
                        JsonObject sourceFields = source.deepCopy();
                        JsonObject resultFields = result.deepCopy();
                        normalizeEnchant(resultFields, sourceItem);
                        sourceFields.remove("name");
                        sourceFields.remove("type");
                        resultFields.remove("name");
                        resultFields.remove("value");
                        resultFields.remove("type");
                        helper.assertTrue(sourceFields.equals(resultFields), path + " entry " + index + " count, weight or conditions changed");
                        String expected = expectedTarget(sourceItem, element, place);
                        helper.assertTrue(expected.equals(entryName(result)), path + " unexpected translation of " + sourceItem);
                        helper.assertTrue(result.get("type").getAsString().equals(expected.startsWith("iaftfc:")
                            ? "minecraft:loot_table" : source.get("type").getAsString()), path + " wrong entry type for " + sourceItem);
                        if (result.has("functions")) {
                            int setCounts = 0;
                            for (var function : result.getAsJsonArray("functions")) {
                                if (function.getAsJsonObject().get("function").getAsString().endsWith("set_count")) setCounts++;
                            }
                            helper.assertTrue(setCounts <= 1, path + " doubled count provider");
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void profiles(GameTestHelper helper) throws Exception {
        String[] profiles = {"ordinary", "dangerous", "lategame"};
        String[][] ingots = {{"copper", "bronze", "bismuth_bronze", "black_bronze", "wrought_iron"},
            {"wrought_iron", "steel"}, {"steel", "black_steel"}};
        String[][] equipment = {{"copper", "bronze", "bismuth_bronze", "black_bronze"},
            {"wrought_iron"}, {"steel"}};
        for (int index = 0; index < profiles.length; index++) {
            String profile = profiles[index];
            JsonArray ingotEntries = entries(generated(helper, "iaftfc", "shared/ingots/" + profile));
            helper.assertTrue(ingotEntries.size() == ingots[index].length, profile + " ingot count");
            for (int i = 0; i < ingots[index].length; i++) {
                JsonObject entry = ingotEntries.get(i).getAsJsonObject();
                helper.assertTrue(entry.get("name").getAsString().equals("tfc:metal/ingot/" + ingots[index][i])
                    && entry.get("weight").getAsInt() == 1, profile + " ingot " + i);
            }
            assertDraws(helper, "shared/ingots/" + profile, names(ingotEntries));
            for (String family : List.of("sword", "helmet", "chestplate", "leggings", "boots")) {
                JsonArray options = entries(generated(helper, "iaftfc", "shared/equipment/" + profile + "/" + family));
                int unfinished = 0;
                int finished = 0;
                Set<String> metals = new HashSet<>();
                Set<String> kinds = new HashSet<>();
                for (var option : options) {
                    JsonObject entry = option.getAsJsonObject();
                    String item = entry.get("name").getAsString();
                    String path = item.substring("tfc:metal/".length());
                    String material = path.substring(path.lastIndexOf('/') + 1);
                    metals.add(material);
                    int weight = entry.get("weight").getAsInt();
                    if (path.startsWith("unfinished_") || path.contains("_blade/") || path.contains("_head/")) {
                        unfinished += weight;
                        helper.assertTrue(weight == (family.equals("sword") ? 4 : 2), item + " unfinished weight");
                    } else {
                        finished += weight;
                        helper.assertTrue(weight == (family.equals("sword") ? 2 : 1), item + " finished weight");
                    }
                    if (family.equals("sword")) kinds.add(path.substring(0, path.indexOf('/')).replace("_blade", "").replace("_head", ""));
                    else helper.assertTrue(path.contains(family.equals("leggings") ? "greaves" : family), item + " wrong armor slot");
                }
                helper.assertTrue(metals.equals(Set.of(equipment[index])), profile + " " + family + " metals " + metals);
                helper.assertTrue(unfinished == 2 * finished, profile + " " + family + " progression ratio");
                if (family.equals("sword")) helper.assertTrue(kinds.equals(Set.of("sword", "javelin", "mace")), "Sword family changed");
                assertDraws(helper, "shared/equipment/" + profile + "/" + family, names(options));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toolFamilies(GameTestHelper helper) throws Exception {
        String[] profiles = {"ordinary", "dangerous", "lategame"};
        String[][] metals = {{"copper", "bronze", "bismuth_bronze", "black_bronze"}, {"wrought_iron"}, {"steel"}};
        String[][] families = {{"pickaxe", "pickaxe", "propick"}, {"axe", "axe", "saw"},
            {"hoe", "hoe", "scythe"}, {"sword", "sword", "javelin", "mace"},
            {"shovel", "shovel"}, {"knife", "knife"}, {"shears", "shears"}, {"shield", "shield"},
            {"bow", "bow"}, {"crossbow", "crossbow"}};
        for (int profile = 0; profile < profiles.length; profile++) {
            for (String[] family : families) {
                JsonArray options = entries(generated(helper, "iaftfc", "shared/equipment/" + profiles[profile] + "/" + family[0]));
                Set<String> expected = new HashSet<>();
                for (String metal : metals[profile]) {
                    for (int kind = 1; kind < family.length; kind++) {
                        if (family[0].equals("bow") || family[0].equals("crossbow")) {
                            expected.add("minecraft:" + family[0] + ":1");
                            continue;
                        }
                        String blade = List.of("sword", "saw", "scythe", "knife").contains(family[kind]) ? "blade" : "head";
                        if (family[0].equals("shears") || family[0].equals("shield")) {
                            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(
                                "tfc:metal/" + family[kind] + "_" + blade + "/" + metal)), family[0] + " has a TFC part");
                            expected.add("tfc:metal/" + family[kind] + "/" + metal + ":1");
                        } else {
                            expected.add("tfc:metal/" + family[kind] + "_" + blade + "/" + metal + ":4");
                            expected.add("tfc:metal/" + family[kind] + "/" + metal + ":2");
                        }
                    }
                }
                Set<String> actual = new HashSet<>();
                for (var option : options) {
                    JsonObject entry = option.getAsJsonObject();
                    actual.add(entry.get("name").getAsString() + ":" + entry.get("weight").getAsInt());
                }
                helper.assertTrue(actual.equals(expected), profiles[profile] + " " + family[0] + " family, metals or 2:1 ratio changed");
                helper.assertTrue(options.size() == expected.size(), profiles[profile] + " " + family[0] + " duplicate options");
                assertDraws(helper, "shared/equipment/" + profiles[profile] + "/" + family[0], names(options));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attributeDrops(GameTestHelper helper) throws Exception {
        JsonArray gems = entries(generated(helper, "iaftfc", "shared/gems"));
        helper.assertTrue(gems.size() == 9, "Gem candidates changed");
        Set<String> expectedGems = Set.of("amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz");
        Set<String> found = new HashSet<>();
        for (var option : gems) {
            JsonObject entry = option.getAsJsonObject();
            found.add(entry.get("name").getAsString().replace("tfc:gem/", ""));
            helper.assertTrue(entry.get("weight").getAsInt() == 1, "Gem weight changed");
        }
        helper.assertTrue(found.equals(expectedGems), "Gem candidates changed");
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String place : List.of("roost", "female_cave", "male_cave")) {
                JsonArray before = entries(original("chest/" + element + "_dragon_" + place));
                JsonArray after = entries(generated(helper, "iceandfire", "chest/" + element + "_dragon_" + place));
                String sourceGem = element.equals("fire") ? "minecraft:emerald" : element.equals("ice") ? "iceandfire:sapphire_gem" : "minecraft:amethyst_shard";
                String targetGem = "tfc:gem/" + (element.equals("fire") ? "ruby" : element.equals("ice") ? "sapphire" : "topaz");
                String sourceIngot = element.equals("fire") ? "minecraft:gold_ingot" : element.equals("ice") ? "iceandfire:silver_ingot" : "minecraft:copper_ingot";
                String targetMetal = element.equals("fire") ? "gold" : element.equals("ice") ? "silver" : "copper";
                String profile = place.equals("roost") ? "ordinary" : place.equals("female_cave") ? "dangerous" : "lategame";
                Set<String> matched = new HashSet<>();
                for (int i = 0; i < before.size(); i++) {
                    String source = before.get(i).getAsJsonObject().get("name").getAsString();
                    String target = entryName(after.get(i).getAsJsonObject());
                    if (source.equals(sourceGem)) { matched.add("gem"); helper.assertTrue(target.equals(targetGem), source + " attribute gem"); }
                    if (source.equals(sourceIngot)) { matched.add("ingot"); helper.assertTrue(target.equals("tfc:metal/ingot/" + targetMetal), source + " attribute ingot"); }
                    if (source.equals("minecraft:diamond")) { matched.add("generic_gem"); helper.assertTrue(target.equals("iaftfc:shared/gems"), "Generic gem missing shared table"); }
                    if (source.equals("minecraft:iron_ingot")) { matched.add("generic_ingot"); helper.assertTrue(target.equals("iaftfc:shared/ingots/" + profile), "Generic ingot profile"); }
                    if (source.endsWith("_nugget")) {
                        matched.add("nugget");
                        String ore = source.contains("silver") ? "silver" : source.contains("copper") ? "copper" : "gold";
                        helper.assertTrue(target.equals("tfc:ore/small_native_" + ore), "Wrong small ore");
                    }
                    JsonObject sourceEntry = before.get(i).getAsJsonObject();
                    if (source.equals(sourceGem) || source.equals(sourceIngot) || source.endsWith("_nugget")
                        || source.equals("minecraft:diamond") || source.equals("minecraft:iron_ingot")) {
                        helper.assertTrue(sourceEntry.get("functions").equals(after.get(i).getAsJsonObject().get("functions")),
                            source + " count provider changed");
                    }
                    helper.assertTrue(!target.contains("red_steel") && !target.contains("blue_steel")
                        && !target.contains("dragonsteel") && !target.equals("minecraft:iron_ingot")
                        && !target.equals("iceandfire:silver_ingot") && !target.equals("iceandfire:sapphire_gem"), "Forbidden chest loot " + target);
                }
                Set<String> expectedSources = new HashSet<>(Set.of("gem", "generic_gem", "generic_ingot", "nugget"));
                if (!element.equals("ice") || !place.equals("male_cave")) expectedSources.add("ingot");
                helper.assertTrue(matched.equals(expectedSources),
                    element + " " + place + " missing attribute sources " + matched);
                var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                    Registries.LOOT_TABLE, ResourceLocation.parse("iceandfire:chest/" + element + "_dragon_" + place)));
                var params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))))
                    .create(LootContextParamSets.CHEST);
                for (long seed = 0; seed < 256; seed++) {
                    for (var stack : table.getRandomItems(params, seed)) {
                        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        helper.assertTrue(!item.equals("minecraft:iron_ingot") && !item.equals("iceandfire:silver_ingot")
                            && !item.equals("iceandfire:sapphire_gem") && !item.contains("red_steel")
                            && !item.contains("blue_steel") && !item.contains("dragonsteel"), "Forbidden actual drop " + item);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equipmentEnchant(GameTestHelper helper) throws Exception {
        var params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))))
            .create(LootContextParamSets.CHEST);
        for (String element : List.of("fire", "ice", "lightning")) {
            String path = "chest/" + element + "_dragon_male_cave";
            JsonArray source = entries(original(path));
            JsonArray result = entries(generated(helper, "iceandfire", path));
            Set<String> families = new HashSet<>();
            for (int i = 0; i < source.size(); i++) {
                JsonObject entry = source.get(i).getAsJsonObject();
                String name = entry.get("name").getAsString();
                if (!name.endsWith("_sword") && List.of("helmet", "chestplate", "leggings", "boots")
                    .stream().noneMatch(part -> name.endsWith("_" + part))) continue;
                String family = name.substring(name.lastIndexOf('_') + 1);
                families.add(family);
                JsonObject normalized = result.get(i).getAsJsonObject().deepCopy();
                normalizeEnchant(normalized, name);
                helper.assertTrue(entry.get("functions").equals(normalized.get("functions")),
                    name + " parent enchant settings changed");
                helper.assertTrue(entryName(result.get(i).getAsJsonObject()).equals("iaftfc:shared/equipment/lategame/" + family),
                    name + " does not reach its equipment table");
            }
            helper.assertTrue(families.equals(Set.of("sword", "helmet", "chestplate", "leggings", "boots")),
                path + " enchant sources missing");
            var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.parse("iceandfire:" + path)));
            boolean sampledPart = false;
            boolean sampledFinished = false;
            for (long seed = 0; seed < 2048; seed++) {
                for (var stack : table.getRandomItems(params, seed)) {
                    String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (!item.startsWith("tfc:metal/") || item.contains("/ingot/")) continue;
                    boolean part = item.contains("unfinished_") || item.contains("_head/") || item.contains("_blade/");
                    if (part) {
                        sampledPart = true;
                        helper.assertTrue(!stack.isEnchanted(), item + " received an enchant");
                    } else {
                        sampledFinished = true;
                        helper.assertTrue(stack.isEnchanted(), item + " lost the original enchant");
                    }
                }
            }
            helper.assertTrue(sampledPart && sampledFinished, path + " did not sample both progression stages");
        }
        for (String profile : List.of("ordinary", "dangerous", "lategame")) {
            for (String family : List.of("bow", "crossbow")) {
                JsonArray options = entries(generated(helper, "iaftfc", "shared/equipment/" + profile + "/" + family));
                helper.assertTrue(options.size() == 1 && options.get(0).getAsJsonObject().get("name").getAsString()
                    .equals("minecraft:" + family), profile + " " + family + " mixed with another weapon");
            }
        }
        JsonArray coal = entries(generated(helper, "iaftfc", "shared/coal_like"));
        Set<String> fuels = new HashSet<>();
        for (var option : coal) {
            JsonObject entry = option.getAsJsonObject();
            fuels.add(entry.get("name").getAsString());
            helper.assertTrue(entry.get("weight").getAsInt() == 1, "Coal weights differ");
        }
        helper.assertTrue(fuels.equals(Set.of("minecraft:charcoal", "tfc:ore/bituminous_coal", "tfc:ore/lignite", "tfc:ore/graphite")),
            "Graphite must be mined ore, not powder");
        Set<String> utility = new HashSet<>();
        for (var option : entries(generated(helper, "iaftfc", "shared/utility"))) {
            JsonObject entry = option.getAsJsonObject();
            utility.add(entry.get("name").getAsString());
            helper.assertTrue(entry.get("weight").getAsInt() == 1, "Utility weights differ");
        }
        helper.assertTrue(utility.equals(Set.of("tfc:powder/flux", "minecraft:glass", "minecraft:glass_pane",
            "tfc:kaolin_clay", "tfc:powder/wood_ash", "tfc:powder/saltpeter", "tfc:powder/sylvite")),
            "Utility candidates changed or contain coal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void enchantReference(GameTestHelper helper) {
        var params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))))
            .create(LootContextParamSets.CHEST);
        for (String family : List.of("sword", "helmet", "chestplate", "leggings", "boots")) {
            String source = "minecraft:diamond_" + family;
            String part = family.equals("leggings") ? "greaves" : family;
            String finished = "tfc:metal/" + part + "/steel";
            String unfinished = "tfc:metal/" + (family.equals("sword") ? "sword_blade" : "unfinished_" + part) + "/steel";
            var function = new EnchantEquipment(25, true, ResourceLocation.parse(source));
            for (long seed = 1; seed <= 32; seed++) {
                var actual = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(finished)));
                var partStack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(unfinished)));
                var reference = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(source)));
                var actualContext = new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
                var referenceContext = new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
                function.apply(actual, actualContext);
                function.apply(partStack, new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty()));
                EnchantmentHelper.enchantItem(referenceContext.getRandom(), reference, 25,
                    helper.getLevel().registryAccess(), Optional.empty());
                helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(actual)
                    .equals(EnchantmentHelper.getEnchantmentsForCrafting(reference)), finished + " differs from original level 25 at seed "
                    + seed + ": " + EnchantmentHelper.getEnchantmentsForCrafting(actual) + " vs "
                    + EnchantmentHelper.getEnchantmentsForCrafting(reference));
                helper.assertTrue(!partStack.isEnchanted(), unfinished + " received an enchant");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pileDrops(GameTestHelper helper) throws Exception {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        for (String metal : List.of("gold", "silver", "copper")) {
            JsonArray original = entries(original("blocks/" + metal + "_pile"));
            JsonArray changed = entries(generated(helper, "iceandfire", "blocks/" + metal + "_pile"));
            helper.assertTrue(original.size() == 8 && changed.size() == 8, metal + " layer branches changed");
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:" + metal + "_pile"));
            IntegerProperty layers = (IntegerProperty) block.defaultBlockState().getProperties().stream()
                .filter(property -> property.getName().equals("layers")).findFirst().orElseThrow();
            for (int layer = 1; layer <= 8; layer++) {
                JsonObject before = original.get(layer - 1).getAsJsonObject().deepCopy();
                JsonObject after = changed.get(layer - 1).getAsJsonObject().deepCopy();
                before.remove("name");
                after.remove("name");
                helper.assertTrue(before.equals(after), metal + " layer " + layer + " original conditions changed");
                helper.assertTrue(changed.get(layer - 1).getAsJsonObject().get("name").getAsString()
                    .equals("tfc:ore/small_native_" + metal), metal + " wrong ore");
                var state = block.defaultBlockState().setValue(layers, layer);
                helper.getLevel().setBlock(pos, state, 3);
                helper.assertTrue(player.gameMode.destroyBlock(pos), metal + " layer " + layer + " could not be broken");
                var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
                int count = drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
                helper.assertTrue(count == 2 * layer, metal + " layer " + layer + " dropped " + count);
                for (var stack : drops) helper.assertTrue(BuiltInRegistries.ITEM.getKey(stack.getItem().getItem())
                    .equals(ResourceLocation.parse("tfc:ore/small_native_" + metal)), "Pile dropped nugget or ingot");
                for (var drop : drops) drop.discard();
            }
        }
        helper.succeed();
    }

    private static JsonObject original(String path) throws Exception {
        var file = ModList.get().getModFileById("iceandfire").getFile()
            .findResource("data/iceandfire/loot_table/" + path + ".json");
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject generated(GameTestHelper helper, String namespace, String path) throws Exception {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, "loot_table/" + path + ".json");
        var resource = helper.getLevel().getServer().getResourceManager().getResource(id).orElseThrow();
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonArray entries(JsonObject table) {
        return table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
    }

    private static Set<String> names(JsonArray entries) {
        Set<String> names = new HashSet<>();
        for (var entry : entries) names.add(entry.getAsJsonObject().get("name").getAsString());
        return names;
    }

    private static void assertDraws(GameTestHelper helper, String path, Set<String> expected) {
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.parse("iaftfc:" + path)));
        var params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))))
            .create(LootContextParamSets.CHEST);
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 512; seed++) {
            for (var stack : table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                helper.assertTrue(expected.contains(item), path + " yielded unexpected " + item);
                seen.add(item);
            }
        }
        helper.assertTrue(seen.equals(expected), path + " did not yield all candidates: " + seen);
    }

    private static String entryName(JsonObject entry) {
        return entry.has("value") ? entry.get("value").getAsString() : entry.get("name").getAsString();
    }

    private static void normalizeEnchant(JsonObject entry, String source) {
        if (!entry.has("functions")) return;
        for (var function : entry.getAsJsonArray("functions")) {
            JsonObject data = function.getAsJsonObject();
            if (!data.get("function").getAsString().equals("iaftfc:enchant_equipment")) continue;
            if (!data.get("source").getAsString().equals(source)) throw new AssertionError("Wrong enchant source for " + source);
            data.addProperty("function", "enchant_with_levels");
            data.remove("source");
        }
    }

    private static String expectedTarget(String source, String element, String place) {
        String profile = place.equals("roost") ? "ordinary" : place.equals("female_cave") ? "dangerous" : "lategame";
        String gem = element.equals("fire") ? "minecraft:emerald"
            : element.equals("ice") ? "iceandfire:sapphire_gem" : "minecraft:amethyst_shard";
        String metal = element.equals("fire") ? "gold" : element.equals("ice") ? "silver" : "copper";
        if (source.equals(gem)) return "tfc:gem/" + (element.equals("fire") ? "ruby" : element.equals("ice") ? "sapphire" : "topaz");
        if (source.equals("minecraft:diamond")) return "iaftfc:shared/gems";
        if (source.equals("minecraft:iron_ingot") || source.equals("minecraft:gold_ingot") && !element.equals("fire")) {
            return "iaftfc:shared/ingots/" + profile;
        }
        if (source.equals(element.equals("fire") ? "minecraft:gold_ingot"
            : element.equals("ice") ? "iceandfire:silver_ingot" : "minecraft:copper_ingot")) return "tfc:metal/ingot/" + metal;
        if (source.equals("minecraft:gold_nugget") || source.equals("iceandfire:silver_nugget")
            || source.equals("iceandfire:copper_nugget")) {
            return "tfc:ore/small_native_" + (source.contains("silver") ? "silver" : source.contains("copper") ? "copper" : "gold");
        }
        if (source.equals("minecraft:coal")) return "iaftfc:shared/coal_like";
        if (Set.of("minecraft:chain", "minecraft:cobweb", "minecraft:obsidian", "minecraft:packed_ice",
            "minecraft:flint").contains(source)) return "iaftfc:shared/utility";
        if (source.endsWith("_sword") && !source.contains("dragon")) return "iaftfc:shared/equipment/" + profile + "/sword";
        for (String part : List.of("helmet", "chestplate", "leggings", "boots")) {
            if (source.endsWith("_" + part)) return "iaftfc:shared/equipment/" + profile + "/" + part;
        }
        return source;
    }
}
