package net.claustra01.iaftfc_test.gametest;

import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.common.recipes.KnappingRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.util.data.KnappingPattern;
import net.dries007.tfc.util.data.KnappingType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class KnappingGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final List<ScaleColor> SCALE_COLORS = List.of(
        new ScaleColor("amethyst", "amythest"),
        new ScaleColor("black", "black"),
        new ScaleColor("blue", "blue"),
        new ScaleColor("bronze", "bronze"),
        new ScaleColor("copper", "copper"),
        new ScaleColor("electric", "electric"),
        new ScaleColor("gray", "gray"),
        new ScaleColor("green", "green"),
        new ScaleColor("red", "red"),
        new ScaleColor("sapphire", "sapphire"),
        new ScaleColor("silver", "silver"),
        new ScaleColor("white", "white"));
    private static final List<PatternResult> ARMOR = List.of(
        new PatternResult("helmet", List.of("#####", "#   #", "#   #")),
        new PatternResult("chestplate", List.of("#   #", "#####", "#####", "#####", "#####")),
        new PatternResult("leggings", List.of("#####", "#####", "## ##", "## ##", "## ##")),
        new PatternResult("boots", List.of("##   ", "##   ", "##   ", "#### ", "#####")));
    private static final List<ToolPart> TOOLS = List.of(
        new ToolPart("axe_head", "axe", List.of(" #   ", "#### ", "#####", "#### ", " #   "),
            List.of("MM", "MH", " H")),
        new ToolPart("hoe_head", "hoe", List.of("     ", "#####", "   ##", "     ", "     "),
            List.of("MM", " H", " H")),
        new ToolPart("pickaxe_head", "pickaxe", List.of("     ", " ### ", "#   #", "     ", "     "),
            List.of("MMM", " H ", " H ")),
        new ToolPart("shovel_head", "shovel", List.of(" ### ", " ### ", " ### ", " ### ", "  #  "),
            List.of("M", "H", "H")),
        new ToolPart("sword_blade", "sword", List.of("##   ", "###  ", " ### ", "  ## ", "    #"),
            List.of("M", "M", "H")));

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 400)
    public static void dragonScaleColorsConsumeFiveAndReplaceCrafting(GameTestHelper helper) {
        FakePlayer player = player(helper);
        for (int colorIndex = 0; colorIndex < SCALE_COLORS.size(); colorIndex++) {
            ScaleColor color = SCALE_COLORS.get(colorIndex);
            ScaleColor other = SCALE_COLORS.get((colorIndex + 1) % SCALE_COLORS.size());
            KnappingType type = type("iaftfc:dragonscales_" + color.id());
            helper.assertValueEqual(type.inputItem().count(), 5, "Scale knapping must require five scales");
            helper.assertValueEqual(type.amountToConsume(), 5, "Scale knapping must consume five scales");
            helper.assertTrue(type.consumeAfterComplete(), "Scale knapping must consume only after completion");
            helper.assertTrue(type.matches(stack(color.scale(), 5)), "The matching scale color must start knapping");
            helper.assertTrue(!type.matches(stack(other.scale(), 5)), "A different scale color must use another type");

            for (PatternResult armor : ARMOR) {
                String recipeId = "iceandfire:armor_" + color.recipeId() + "_" + armor.id();
                KnappingRecipe recipe = knappingRecipe(helper, recipeId);
                helper.assertValueEqual(recipe.getType(), TFCRecipeTypes.KNAPPING.get(),
                    "All scale armor must share the TFC knapping recipe category");
                helper.assertValueEqual(recipe.knappingType().id(),
                    ResourceLocation.parse("iaftfc:dragonscales_" + color.id()),
                    "Armor must use its scale color's knapping type");
                helper.assertTrue(!recipe.knappingType().id().equals(
                    ResourceLocation.parse("iaftfc:dragonscales_" + other.id())),
                    "Another scale color must use a different knapping type");
                assertPattern(helper, recipe.getPattern(), armor.pattern());

                ItemStack scales = stack(color.scale(), 5);
                ItemStack result = complete(player, type, scales, armor.pattern());
                helper.assertTrue(result.is(item("iceandfire:armor_" + color.id() + "_" + armor.id())),
                    "Knapping must produce the CE armor for " + color.id() + " " + armor.id());
                helper.assertValueEqual(scales.getCount(), 5,
                    "Scales must remain until the completed armor is taken");
                ((KnappingContainer) player.containerMenu).onSlotTake(player, 0, result);
                helper.assertTrue(scales.isEmpty(), "Taking one armor piece must consume exactly five scales");

                long outputRecipes = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                    .filter(holder -> holder.value().getResultItem(helper.getLevel().registryAccess())
                        .is(item("iceandfire:armor_" + color.id() + "_" + armor.id())))
                    .count();
                helper.assertValueEqual(outputRecipes, 1L,
                    "Scale armor must have only its replacement knapping recipe");
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 300)
    public static void dragonboneFirstClickMakesPartsAndTools(GameTestHelper helper) {
        FakePlayer player = player(helper);
        KnappingType type = type("iaftfc:dragonbone");
        helper.assertValueEqual(type.inputItem().count(), 2, "Dragonbone knapping must require two bones");
        helper.assertValueEqual(type.amountToConsume(), 1, "The first operation must consume one bone");
        helper.assertTrue(!type.consumeAfterComplete(), "Dragonbone must be consumed on the first operation");

        ItemStack firstClickBones = stack("iceandfire:dragonbone", 2);
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, firstClickBones);
        KnappingContainer firstClickMenu = KnappingContainer.create(firstClickBones, type,
            InteractionHand.MAIN_HAND, 0, player.getInventory(), 1);
        firstClickMenu.onButtonPress(0, new CompoundTag());
        helper.assertValueEqual(firstClickBones.getCount(), 1,
            "The first knapping operation must immediately consume one dragon bone");

        for (ToolPart tool : TOOLS) {
            KnappingRecipe partRecipe = knappingRecipe(helper,
                "iaftfc:knapping/dragonbone/" + tool.part());
            assertPattern(helper, partRecipe.getPattern(), tool.pattern());
            ItemStack bones = stack("iceandfire:dragonbone", 2);
            ItemStack part = complete(player, type, bones, tool.pattern());
            helper.assertValueEqual(bones.getCount(), 1,
                "The first knapping operation must consume one dragon bone");
            helper.assertTrue(part.is(item("iaftfc:dragonbone/" + tool.part())),
                "The correct dragonbone part must be formed");

            Recipe<?> loaded = recipe(helper, "iceandfire:dragonbone_" + tool.tool());
            helper.assertTrue(loaded instanceof CraftingRecipe,
                "The CE tool recipe must be replaced by the part assembly recipe");
            CraftingRecipe assembly = (CraftingRecipe) loaded;
            ItemStack witherBone = stack("iceandfire:witherbone", 1);
            CraftingInput input = CraftingInput.of(1, 2, List.of(part.copy(), witherBone));
            helper.assertTrue(assembly.matches(input, helper.getLevel()),
                "The part and #c:bones/wither must assemble the CE tool");
            helper.assertTrue(assembly.assemble(input, helper.getLevel().registryAccess())
                .is(item("iceandfire:dragonbone_" + tool.tool())),
                "Assembly must preserve the CE tool registry ID");
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                oldToolInput(tool.oldPattern()), helper.getLevel()).isEmpty(),
                "The original raw-dragonbone crafting pattern must no longer bypass knapping");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonFluteComesDirectlyFromKnapping(GameTestHelper helper) {
        FakePlayer player = player(helper);
        KnappingType type = type("iaftfc:dragonbone");
        List<String> pattern = List.of("#####", "# # #", "##  #", "#  ##", "#####");
        KnappingRecipe recipe = knappingRecipe(helper, "iceandfire:dragon_flute");
        assertPattern(helper, recipe.getPattern(), pattern);
        helper.assertTrue(recipe.getIngredient() == null,
            "The flute must not require an intermediate tool head");

        ItemStack bones = stack("iceandfire:dragonbone", 2);
        ItemStack flute = complete(player, type, bones, pattern);
        helper.assertValueEqual(bones.getCount(), 1, "Flute knapping must consume one dragon bone on first use");
        helper.assertTrue(flute.is(item("iceandfire:dragon_flute")),
            "The flute pattern must produce the CE Dragon Flute directly");
        helper.succeed();
    }

    private static ItemStack complete(FakePlayer player, KnappingType type, ItemStack input,
        List<String> rows) {
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, input);
        KnappingContainer menu = KnappingContainer.create(input, type, InteractionHand.MAIN_HAND, 0,
            player.getInventory(), 1);
        player.containerMenu = menu;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean keep = y < rows.size() && x < rows.getFirst().length()
                    && rows.get(y).charAt(x) != ' ';
                if (!keep) {
                    menu.onButtonPress(x + 5 * y, new CompoundTag());
                }
            }
        }
        return menu.getSlot(0).getItem().copy();
    }

    private static void assertPattern(GameTestHelper helper, KnappingPattern actual, List<String> expected) {
        helper.assertValueEqual(actual.getWidth(), expected.getFirst().length(), "Incorrect pattern width");
        helper.assertValueEqual(actual.getHeight(), expected.size(), "Incorrect pattern height");
        helper.assertValueEqual(actual.defaultIsOn(), false, "Incorrect pattern outside-state");
        for (int y = 0; y < expected.size(); y++) {
            for (int x = 0; x < expected.getFirst().length(); x++) {
                helper.assertValueEqual(actual.get(x, y), expected.get(y).charAt(x) != ' ',
                    "Incorrect pattern cell at " + x + "," + y);
            }
        }
    }

    private static CraftingInput oldToolInput(List<String> pattern) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String row : pattern) {
            for (int x = 0; x < row.length(); x++) {
                stacks.add(switch (row.charAt(x)) {
                    case 'M' -> stack("iceandfire:dragonbone", 1);
                    case 'H' -> stack("iceandfire:witherbone", 1);
                    default -> ItemStack.EMPTY;
                });
            }
        }
        return CraftingInput.of(pattern.getFirst().length(), pattern.size(), stacks);
    }

    private static KnappingRecipe knappingRecipe(GameTestHelper helper, String id) {
        Recipe<?> recipe = recipe(helper, id);
        helper.assertTrue(recipe instanceof KnappingRecipe, id + " must be a TFC knapping recipe");
        return (KnappingRecipe) recipe;
    }

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().getRecipeManager().byKey(ResourceLocation.parse(id))
            .orElseThrow(() -> new AssertionError("Missing recipe " + id)).value();
    }

    private static KnappingType type(String id) {
        return KnappingType.MANAGER.getOrThrow(ResourceLocation.parse(id));
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        if (!helper.getLevel().players().contains(player)) {
            helper.getLevel().addNewPlayer(player);
        }
        return player;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(item(id), count);
    }

    private record ScaleColor(String id, String recipeId) {
        String scale() {
            return "iceandfire:dragonscales_" + id;
        }
    }

    private record PatternResult(String id, List<String> pattern) {
    }

    private record ToolPart(String part, String tool, List<String> pattern, List<String> oldPattern) {
    }

    private KnappingGameTests() {
    }
}
