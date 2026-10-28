package net.claustra01.iaftfc.gametest;

import java.util.List;
import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.common.recipes.KnappingRecipe;
import net.dries007.tfc.util.data.KnappingType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("iaftfc")
public final class KnappingTests {
    static {
        GameTestReports.configure();
    }

    private static final String[] COLORS = {"amethyst", "black", "blue", "bronze", "copper", "electric", "gray", "green", "red", "sapphire", "silver", "white"};
    private static final String[] PARTS = {"axe_head", "hoe_head", "pickaxe_head", "shovel_head", "sword_blade", "dragon_flute"};
    private static final String[] BONE = {".#.../####./#####/####./.#...", "...../#####/...##/...../.....", "...../.###./#...#/...../.....", ".###./.###./.###./.###./..#..", "##.../###../.###./..##./....#", "#####/#.#.#/##..#/#..##/#####"};
    private static final String[] ARMOR_PARTS = {"helmet", "chestplate", "leggings", "boots"};
    private static final String[] ARMOR = {"#####/#...#/#...#/...../.....", "#...#/#####/#####/#####/#####", "#####/#####/##.##/##.##/##.##", "##.../##.../##.../####./#####"};

    @GameTest(template = "empty")
    public static void oneBoneCannotStart(GameTestHelper test) {
        if (KnappingType.get(stack("iceandfire:dragonbone", 1)) != null) test.fail("One bone opened knapping");
        if (KnappingType.get(stack("iceandfire:dragonbone", 2)) != type("dragonbone")) test.fail("Two bones did not open knapping");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void boneOpenClose(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonbone", 2));
        menu(player, type("dragonbone")).removed(player);
        if (player.getOffhandItem().getCount() != 2) test.fail("Opening consumed bone");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void boneCutThenCancel(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonbone", 2));
        KnappingContainer menu = menu(player, type("dragonbone"));
        menu.onButtonPress(0, null);
        if (player.getOffhandItem().getCount() != 1) test.fail("First cut did not consume one bone");
        menu.removed(player);
        if (player.getOffhandItem().getCount() != 1) test.fail("Cancelled cut restored bone");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void boneFinishAndClose(GameTestHelper test) {
        KnappingType type = type("dragonbone");
        if (type.amountToConsume() != 1 || type.consumeAfterComplete()) test.fail("Bone consumption settings");
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonbone", 3));
        KnappingContainer menu = menu(player, type);
        cut(menu, BONE[0]);
        if (!menu.getSlot(0).getItem().is(stack("iaftfc:dragonbone/axe_head", 1).getItem())) test.fail("Finished bone result absent");
        menu.removed(player);
        if (player.getOffhandItem().getCount() != 2) test.fail("Finished bone consumed more than one");
        if (count(player, "iaftfc:dragonbone/axe_head") != 1) test.fail("Finished bone result not returned");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void allBonePatterns(GameTestHelper test) {
        for (int i = 0; i < PARTS.length; i++) {
            String id = "iaftfc:recipe/knapping/dragonbone_" + PARTS[i];
            if (!(recipe(test, id) instanceof KnappingRecipe)) test.fail("Missing bone recipe " + id);
            Player player = test.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonbone", 2));
            KnappingContainer menu = menu(player, type("dragonbone"));
            cut(menu, BONE[i]);
            String output = i == 5 ? "iceandfire:dragon_flute" : "iaftfc:dragonbone/" + PARTS[i];
            if (!menu.getSlot(0).getItem().is(stack(output, 1).getItem()) || menu.getSlot(0).getItem().getCount() != 1) test.fail("Bone pattern " + output);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonbone", 2));
            menu = menu(player, type("dragonbone"));
            cutExcept(menu, BONE[i], BONE[i].replace("/", "").indexOf('.'));
            if (!menu.getSlot(0).getItem().isEmpty()) test.fail("Incomplete bone pattern " + output);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void witherBoneHandles(GameTestHelper test) {
        for (int i = 0; i < 5; i++) {
            String tool = PARTS[i].replace("_head", "").replace("_blade", "");
            CraftingRecipe recipe = (CraftingRecipe) recipe(test, "iceandfire:dragonbone_" + tool);
            ItemStack head = stack("iaftfc:dragonbone/" + PARTS[i], 1);
            CraftingInput input = CraftingInput.of(1, 2, List.of(head, stack("iceandfire:witherbone", 1)));
            if (!recipe.matches(input, test.getLevel())) test.fail("Witherbone handle " + tool);
            if (recipe.matches(CraftingInput.of(1, 2, List.of(head, stack("minecraft:bone", 1))), test.getLevel()) || recipe.matches(CraftingInput.of(1, 2, List.of(head, stack("minecraft:stick", 1))), test.getLevel())) test.fail("Invalid handle " + tool);
            ItemStack output = recipe.assemble(input, test.getLevel().registryAccess());
            if (!output.is(stack("iceandfire:dragonbone_" + tool, 1).getItem()) || output.getCount() != 1) test.fail("Wrong assembled tool " + tool);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void scaleStartAndConsumption(GameTestHelper test) {
        KnappingType type = type("dragonscales_red");
        if (KnappingType.get(stack("iceandfire:dragonscales_red", 4)) != null) test.fail("Four scales opened knapping");
        if (KnappingType.get(stack("iceandfire:dragonscales_red", 5)) != type) test.fail("Five scales did not open knapping");
        if (type.matches(stack("iceandfire:dragonscales_blue", 5))) test.fail("Blue scales matched red type");
        if (type.matches(stack("iceandfire:dragonscales_red", 4)) || type.matches(stack("iceandfire:dragonscales_blue", 1))) test.fail("Mixed colors matched one type");
        if (type.amountToConsume() != 5 || !type.consumeAfterComplete()) test.fail("Scale consumption settings");
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonscales_red", 5));
        KnappingContainer menu = menu(player, type);
        menu.onButtonPress(0, null);
        menu.removed(player);
        if (player.getOffhandItem().getCount() != 5) test.fail("Incomplete scales consumed");
        menu = menu(player, type);
        cut(menu, ARMOR[0]);
        if (!menu.getSlot(0).getItem().is(stack("iceandfire:armor_red_helmet", 1).getItem())) test.fail("Scale result absent");
        menu.quickMoveStack(player, 0);
        if (!player.getOffhandItem().isEmpty()) test.fail("Result pickup did not consume five scales");
        if (count(player, "iceandfire:armor_red_helmet") != 1) test.fail("Result pickup did not give one helmet");
        menu.removed(player);
        if (count(player, "iceandfire:armor_red_helmet") != 1) test.fail("Closing duplicated helmet");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void scaleFinishOnClose(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonscales_red", 5));
        KnappingContainer menu = menu(player, type("dragonscales_red"));
        cut(menu, ARMOR[0]);
        menu.removed(player);
        if (!player.getOffhandItem().isEmpty()) test.fail("Finished close restored scales");
        if (count(player, "iceandfire:armor_red_helmet") != 1) test.fail("Finished close lost or duplicated helmet");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void boneRemovalDuringCut(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        Player observer = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonbone", 2));
        KnappingContainer menu = menu(player, type("dragonbone"));
        menu.onButtonPress(0, null);
        menu.removed(player);
        if (player.getOffhandItem().getCount() != 1) test.fail("Disconnect restored cut bone");
        if (count(player, "iaftfc:dragonbone/axe_head") != 0) test.fail("Disconnect created bone part");
        if (count(observer, "iaftfc:dragonbone/axe_head") != 0) test.fail("Observer received bone part");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void scaleRemovalAtCompletion(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        Player observer = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonscales_red", 5));
        KnappingContainer menu = menu(player, type("dragonscales_red"));
        cut(menu, ARMOR[0]);
        menu.removed(player);
        if (!player.getOffhandItem().isEmpty()) test.fail("Disconnect restored finished scales");
        if (count(player, "iceandfire:armor_red_helmet") != 1) test.fail("Disconnect lost or duplicated armor");
        if (count(observer, "iceandfire:armor_red_helmet") != 0) test.fail("Observer received armor");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void allScalePatterns(GameTestHelper test) {
        for (String color : COLORS) for (int i = 0; i < ARMOR_PARTS.length; i++) {
            String part = ARMOR_PARTS[i];
            String recipeColor = color.equals("amethyst") ? "amythest" : color;
            String id = "iceandfire:armor_" + recipeColor + "_" + part;
            if (!(recipe(test, id) instanceof KnappingRecipe)) test.fail("Missing scale recipe " + id);
            Player player = test.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("iceandfire:dragonscales_" + color, 5));
            KnappingContainer menu = menu(player, type("dragonscales_" + color));
            cut(menu, ARMOR[i]);
            String output = "iceandfire:armor_" + color + "_" + part;
            if (!menu.getSlot(0).getItem().is(stack(output, 1).getItem()) || menu.getSlot(0).getItem().getCount() != 1) test.fail("Scale pattern " + output);
            if (color.equals("silver") && menu.getSlot(0).getItem().is(stack("iceandfire:armor_silver_metal_" + part, 1).getItem())) test.fail("Silver scale made metal armor " + part);
            KnappingContainer incomplete = menu(player, type("dragonscales_" + color));
            if (i == 0) {
                cutExcept(incomplete, ARMOR[i], 15);
                if (!incomplete.getSlot(0).getItem().isEmpty()) test.fail("Helmet accepted extra lower cell " + color);
            } else {
                cutExcept(incomplete, ARMOR[i], ARMOR[i].replace("/", "").indexOf('.'));
                if (!incomplete.getSlot(0).getItem().isEmpty()) test.fail("Incomplete scale pattern " + output);
            }
            menu.quickMoveStack(player, 0);
            if (!player.getOffhandItem().isEmpty()) test.fail("Scales not consumed for " + output);
            if (count(player, output) != 1) test.fail("Wrong armor quantity for " + output);
        }
        test.succeed();
    }

    private static Object recipe(GameTestHelper test, String id) {
        return test.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();
    }

    private static KnappingType type(String id) {
        return KnappingType.MANAGER.getOrThrow(ResourceLocation.fromNamespaceAndPath("iaftfc", id));
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), count);
    }

    private static KnappingContainer menu(Player player, KnappingType type) {
        return KnappingContainer.create(player.getOffhandItem(), type, InteractionHand.OFF_HAND, 0, player.getInventory(), 0);
    }

    private static void cut(KnappingContainer menu, String rows) {
        cutExcept(menu, rows, -1);
    }

    private static void cutExcept(KnappingContainer menu, String rows, int except) {
        String pattern = rows.replace("/", "");
        for (int cell = 0; cell < 25; cell++) if (pattern.charAt(cell) == '.' && cell != except) menu.onButtonPress(cell, null);
    }

    private static int count(Player player, String id) {
        return player.getInventory().items.stream().filter(item -> item.is(stack(id, 1).getItem())).mapToInt(ItemStack::getCount).sum();
    }
}
