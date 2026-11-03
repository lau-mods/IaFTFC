package dev.iaftfc.gametest;

import com.iafenvoy.iceandfire.item.armor.DragonScaleArmorItem;
import com.iafenvoy.iceandfire.data.DragonColor;
import dev.iaftfc.knapping.DragonboneParts;
import java.util.List;
import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.util.data.KnappingType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class KnappingGameTests {
    private static final String[] COLORS = {
        "red", "green", "bronze", "gray", "blue", "white", "sapphire", "silver",
        "electric", "amethyst", "copper", "black"
    };
    private static final String[] ARMOR = {"helmet", "chestplate", "leggings", "boots"};
    private static final String[][] ARMOR_PATTERNS = {
        {"#####", "#   #", "#   #", "     ", "     "},
        {"#   #", "#####", "#####", "#####", "#####"},
        {"#####", "#####", "## ##", "## ##", "## ##"},
        {"##   ", "##   ", "##   ", "#### ", "#####"}
    };
    private static final String[][] TOOL_PATTERNS = {
        {"  ###", "   ##", "#   #", "##  #", "#### "},
        {"#####", "#   #", " ### ", "#####", "     "},
        {" #   ", "#### ", "#####", "#### ", " #   "},
        {"###  ", "###  ", "###  ", "###  ", " #   "},
        {"#####", "   ##", "     ", "     ", "     "}
    };

    @GameTest(template = "empty")
    public static void dragonboneKnapping(GameTestHelper helper) {
        for (int index = 0; index < DragonboneParts.TOOLS.length; index++) {
            String tool = DragonboneParts.TOOLS[index];
            String part = DragonboneParts.PARTS[index];
            ItemStack input = item("iceandfire:dragonbone").copyWithCount(2);
            KnappingContainer menu = menu(helper, input);
            helper.assertTrue(menu.getKnappingType() == KnappingType.get(item("iceandfire:dragonbone")),
                "Dragonbone knapping type was not selected");
            helper.assertTrue(menu.getKnappingType().amountToConsume() == 1
                && !menu.getKnappingType().consumeAfterComplete(), "Dragonbone consumption differs from rock");
            remove(menu, TOOL_PATTERNS[index]);
            helper.assertTrue(menu.getSlot(0).getItem().is(item("iaftfc:dragonbone/" + part).getItem())
                && menu.getSlot(0).getItem().getCount() == 1, "Wrong dragonbone part: " + part);
            helper.assertTrue(input.getCount() == 1, "Dragonbone did not consume exactly one item");
            String[] nearMiss = TOOL_PATTERNS[index].clone();
            char[] row = nearMiss[0].toCharArray();
            row[0] = row[0] == '#' ? ' ' : '#';
            nearMiss[0] = new String(row);
            KnappingContainer mismatched = menu(helper, item("iceandfire:dragonbone").copyWithCount(2));
            remove(mismatched, nearMiss);
            helper.assertTrue(mismatched.getSlot(0).getItem().isEmpty(),
                "Near-miss pattern produced a part: " + part);
            mismatched.removed(FakePlayerFactory.getMinecraft(helper.getLevel()));
            menu.removed(FakePlayerFactory.getMinecraft(helper.getLevel()));
        }
        ItemStack singleBone = item("iceandfire:dragonbone");
        KnappingContainer singleBoneMenu = menu(helper, singleBone);
        remove(singleBoneMenu, TOOL_PATTERNS[0]);
        helper.assertTrue(singleBone.isEmpty() && singleBoneMenu.getSlot(0).getItem()
            .is(item("iaftfc:dragonbone/sword_blade").getItem()),
            "One dragonbone could not finish knapping");
        KnappingContainer unmatched = menu(helper, item("iceandfire:dragonbone").copyWithCount(2));
        for (int cell = 0; cell < 25; cell++) {
            unmatched.onButtonPress(cell, new CompoundTag());
        }
        helper.assertTrue(unmatched.getSlot(0).getItem().isEmpty(), "Unspecified pattern produced a part");
        helper.assertTrue(KnappingType.get(item("minecraft:leather")) != unmatched.getKnappingType(),
            "Leather selected the dragonbone knapping type");
        KnappingContainer leather = menu(helper, item("minecraft:leather").copyWithCount(2));
        remove(leather, TOOL_PATTERNS[0]);
        helper.assertTrue(!leather.getSlot(0).getItem().is(item("iaftfc:dragonbone/sword_blade").getItem()),
            "Leather produced a dragonbone blade");
        var recipes = helper.getLevel().getRecipeManager()
            .getAllRecipesFor(net.dries007.tfc.common.recipes.TFCRecipeTypes.KNAPPING.get()).stream()
            .filter(recipe -> recipe.value().knappingType().id()
                .equals(ResourceLocation.parse("iaftfc:dragonbone"))).toList();
        helper.assertTrue(recipes.size() == DragonboneParts.PARTS.length,
            "Dragonbone has an unspecified knapping recipe");
        for (String part : DragonboneParts.PARTS) {
            helper.assertTrue(recipes.stream().filter(recipe -> recipe.value().assemble()
                .is(item("iaftfc:dragonbone/" + part).getItem())).count() == 1,
                "Dragonbone recipe set lacks exactly one " + part);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonboneAssembly(GameTestHelper helper) {
        for (int index = 0; index < DragonboneParts.TOOLS.length; index++) {
            String tool = DragonboneParts.TOOLS[index];
            String part = DragonboneParts.PARTS[index];
            ItemStack head = item("iaftfc:dragonbone/" + part);
            CraftingInput grid = CraftingInput.of(1, 2, List.of(head, item("minecraft:stick")));
            Recipe<CraftingInput> recipe = crafting(helper, "iaftfc:crafting/dragonbone/" + tool);
            helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Wooden rod did not assemble " + tool);
            helper.assertTrue(recipe.assemble(grid, helper.getLevel().registryAccess())
                .is(item("iceandfire:dragonbone_" + tool).getItem()), "Wrong assembled dragonbone tool");
            helper.assertTrue(helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.parse("iceandfire:dragonbone_" + tool)).isEmpty(),
                "Legacy direct crafting recipe remains: " + tool);
        }
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            Recipe<CraftingInput> recipe = crafting(helper, "iceandfire:dragonbone_sword_" + element);
            ItemStack blood = item("iceandfire:" + element + "_dragon_blood");
            CraftingInput sword = CraftingInput.of(2, 1, List.of(item("iceandfire:dragonbone_sword"), blood));
            CraftingInput blade = CraftingInput.of(2, 1, List.of(item("iaftfc:dragonbone/sword_blade"), blood));
            helper.assertTrue(recipe.matches(sword, helper.getLevel())
                && recipe.assemble(sword, helper.getLevel().registryAccess())
                    .is(item("iceandfire:dragonbone_sword_" + element).getItem()),
                "Finished sword cannot receive " + element + " blood");
            helper.assertTrue(!recipe.matches(blade, helper.getLevel()),
                "Unfinished sword blade accepted dragon blood");
            helper.assertTrue(helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, blade, helper.getLevel()).isEmpty(),
                "Another recipe accepted blade and blood");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonscaleKnapping(GameTestHelper helper) {
        for (String color : COLORS) {
            for (int part = 0; part < ARMOR.length; part++) {
                ItemStack input = item("iceandfire:dragonscales_" + color).copyWithCount(2);
                KnappingContainer menu = menu(helper, input);
                helper.assertTrue(menu.getKnappingType().amountToConsume() == 1
                    && !menu.getKnappingType().consumeAfterComplete(), "Scale consumption differs from leather");
                remove(menu, ARMOR_PATTERNS[part]);
                helper.assertTrue(menu.getSlot(0).getItem()
                    .is(item("iceandfire:armor_" + color + "_" + ARMOR[part]).getItem()),
                    "Wrong armor color or part: " + color + " " + ARMOR[part]);
                helper.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && input.getCount() == 1,
                    "Scale knapping did not exchange one scale for one armor item");
                menu.removed(FakePlayerFactory.getMinecraft(helper.getLevel()));
            }
        }
        ItemStack singleScale = item("iceandfire:dragonscales_red");
        KnappingContainer singleScaleMenu = menu(helper, singleScale);
        remove(singleScaleMenu, ARMOR_PATTERNS[0]);
        helper.assertTrue(singleScale.isEmpty() && singleScaleMenu.getSlot(0).getItem()
            .is(item("iceandfire:armor_red_helmet").getItem()),
            "One dragon scale could not finish knapping");
        KnappingContainer wrongColor = menu(helper, item("iceandfire:dragonscales_blue").copyWithCount(2));
        remove(wrongColor, ARMOR_PATTERNS[0]);
        helper.assertTrue(wrongColor.getSlot(0).getItem().is(item("iceandfire:armor_blue_helmet").getItem()),
            "A blue scale did not produce blue armor");
        var redHelmet = helper.getLevel().getRecipeManager().getAllRecipesFor(
            net.dries007.tfc.common.recipes.TFCRecipeTypes.KNAPPING.get()).stream()
            .filter(recipe -> recipe.value().assemble().is(item("iceandfire:armor_red_helmet").getItem()))
            .findFirst().orElseThrow();
        helper.assertTrue(!redHelmet.value().getIngredient().test(item("iceandfire:dragonscales_blue")),
            "Blue scales match the red helmet recipe");
        helper.assertTrue(KnappingType.get(item("minecraft:leather")) != wrongColor.getKnappingType(),
            "Leather matched the dragonscale knapping type");
        KnappingContainer leather = menu(helper, item("minecraft:leather").copyWithCount(2));
        remove(leather, ARMOR_PATTERNS[0]);
        helper.assertTrue(!leather.getSlot(0).getItem().is(item("iceandfire:armor_red_helmet").getItem()),
            "Leather produced dragon scale armor");
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("iaftfc:scale_plate")),
            "An unspecified scale plate was registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonscaleArmorLegacyRoutes(GameTestHelper helper) {
        String[][] oldPatterns = {
            {"###", "# #"}, {"# #", "###", "###"}, {"###", "# #", "# #"}, {"# #", "# #"}
        };
        for (String color : COLORS) {
            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            for (int index = 0; index < ARMOR.length; index++) {
                String part = ARMOR[index];
                String legacy = "iceandfire:armor_" + (color.equals("amethyst") ? "amythest" : color)
                    + "_" + part;
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(legacy)).isEmpty(),
                    "Old armor crafting route remains: " + legacy);
                helper.assertTrue(!item("iceandfire:armor_" + color + "_" + part).isEmpty(),
                    "IaF armor item is missing: " + color + " " + part);
                ItemStack armor = item("iceandfire:armor_" + color + "_" + part);
                helper.assertTrue(armor.getItem() instanceof DragonScaleArmorItem,
                    "Original IaF armor class was replaced");
                EquipmentSlot slot = switch (part) {
                    case "helmet" -> EquipmentSlot.HEAD;
                    case "chestplate" -> EquipmentSlot.CHEST;
                    case "leggings" -> EquipmentSlot.LEGS;
                    default -> EquipmentSlot.FEET;
                };
                DragonScaleArmorItem scaleArmor = (DragonScaleArmorItem) armor.getItem();
                DragonColor expectedColor = DragonColor.getById(color);
                helper.assertTrue(scaleArmor.getColor() == expectedColor
                    && ((ArmorItem) armor.getItem()).getMaterial().equals(expectedColor.getMaterial()),
                    "Original IaF armor material or color changed: " + color);
                player.setItemSlot(slot, armor);
                helper.assertTrue(player.getItemBySlot(slot).is(armor.getItem()),
                    "Original armor cannot be equipped in its slot");
                String[] rows = oldPatterns[index];
                List<ItemStack> ingredients = new java.util.ArrayList<>();
                for (String row : rows) {
                    for (int cell = 0; cell < 3; cell++) {
                        ingredients.add(row.charAt(cell) == '#'
                            ? item("iceandfire:dragonscales_" + color) : ItemStack.EMPTY);
                    }
                }
                CraftingInput grid = CraftingInput.of(3, rows.length, ingredients);
                helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                    grid, helper.getLevel()).map(recipe -> !recipe.value().assemble(grid,
                    helper.getLevel().registryAccess()).is(armor.getItem())).orElse(true),
                    "Old scale crafting grid still produces armor: " + color + " " + part);
            }
            double expectedArmor = 0;
            double expectedToughness = 0;
            for (String part : ARMOR) {
                ItemStack armor = item("iceandfire:armor_" + color + "_" + part);
                ArmorItem original = (ArmorItem) armor.getItem();
                expectedArmor += original.getDefense();
                expectedToughness += original.getToughness();
            }
            player.doTick();
            helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == expectedArmor
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == expectedToughness,
                "Equipped IaF armor attributes differ: " + color + " (armor "
                    + player.getAttributeValue(Attributes.ARMOR) + "/" + expectedArmor + ", toughness "
                    + player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) + "/" + expectedToughness + ")");
        }
        helper.succeed();
    }

    private static KnappingContainer menu(GameTestHelper helper, ItemStack stack) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        KnappingType type = KnappingType.get(stack);
        helper.assertTrue(type != null, "Input has no knapping type: " + stack);
        return KnappingContainer.create(stack, type, InteractionHand.MAIN_HAND,
            player.getInventory().selected, player.getInventory(), 0);
    }

    private static void remove(KnappingContainer menu, String[] rows) {
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (rows[y].charAt(x) == ' ') {
                    menu.onButtonPress(y * 5 + x, new CompoundTag());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(GameTestHelper helper, String id) {
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
        return (Recipe<CraftingInput>) holder.value();
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}
