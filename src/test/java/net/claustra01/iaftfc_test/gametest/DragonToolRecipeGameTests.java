package net.claustra01.iaftfc_test.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonToolRecipeGameTests {
    @GameTest(template = "empty", batch = "m11", timeoutTicks = 200)
    public static void dragonToolsUseTfcCraftingPaths(GameTestHelper helper) {
        assertCrafts(helper, "dragon_horn", grid(List.of("  B", " BB", "IB "),
            'B', "iceandfire:dragonbone", 'I', "tfc:wood/twig/oak"), "dragon_horn");
        assertCrafts(helper, "dragon_stick", grid(List.of("S", "T", "T"),
            'S', "iceandfire:dragon_skull_fire", 'T', "tfc:wood/twig/oak"), "dragon_stick");
        assertCrafts(helper, "dragonseeker", grid(List.of("a", "x", "x"),
            'a', "iceandfire:dragon_skull_fire", 'x', "tfc:metal/ingot/steel"), "dragon_seeker");
        assertCrafts(helper, "epic_dragon_seeker", CraftingInput.of(3, 1, List.of(
            stack("iceandfire:dragon_seeker"), stack("tfc:metal/ingot/red_steel"),
            stack("tfc:metal/ingot/blue_steel"))), "epic_dragon_seeker");
        for (String metal : List.of("fire", "ice", "lightning")) {
            assertCrafts(helper, "legendary_dragon_seeker", grid(List.of("a", "x", "x"),
                'a', "iceandfire:dragon_skull_fire", 'x', "iaftfc:metal/ingot/dragonsteel_" + metal),
                "legendary_dragon_seeker");
        }

        String[][] crystals = {
            {"fire", "ruby"}, {"ice", "sapphire"}, {"lightning", "topaz"}
        };
        for (String[] crystal : crystals) {
            String id = "summoning_crystal_" + crystal[0];
            assertCrafts(helper, id, CraftingInput.of(4, 1, List.of(
                stack("iceandfire:" + crystal[0] + "_dragon_blood"), stack("tfc:gem/" + crystal[1]),
                stack("tfc:gem/diamond"), stack("tfc:gem/emerald"))), id);
        }

        CraftingRecipe crystal = recipe(helper, "summoning_crystal_fire");
        helper.assertTrue(!crystal.matches(CraftingInput.of(4, 1, List.of(
            stack("iceandfire:fire_dragon_blood"), stack("minecraft:ender_pearl"),
            stack("minecraft:diamond"), stack("minecraft:emerald"))), helper.getLevel()),
            "The unavailable Vanilla summoning-crystal inputs still bypass TFC materials");
        CraftingRecipe seeker = recipe(helper, "dragonseeker");
        helper.assertTrue(!seeker.matches(grid(List.of("a", "x", "x"),
            'a', "iceandfire:dragon_skull_fire", 'x', "minecraft:netherite_ingot"), helper.getLevel()),
            "Netherite still bypasses the TFC Dragon Seeker recipe");

        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, String recipeId, CraftingInput input,
        String resultId) {
        CraftingRecipe recipe = recipe(helper, recipeId);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "TFC inputs did not match " + recipeId);
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(stack("iceandfire:" + resultId).getItem()) && result.getCount() == 1,
            recipeId + " did not retain its CE output ID");
    }

    private static CraftingRecipe recipe(GameTestHelper helper, String id) {
        var holder = helper.getLevel().getRecipeManager().byKey(
            ResourceLocation.fromNamespaceAndPath("iceandfire", id)).orElseThrow();
        helper.assertTrue(holder.value() instanceof CraftingRecipe, id + " must be a crafting recipe");
        return (CraftingRecipe) holder.value();
    }

    private static CraftingInput grid(List<String> pattern, char firstKey, String firstItem,
        char secondKey, String secondItem) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String row : pattern) {
            for (int x = 0; x < row.length(); x++) {
                char key = row.charAt(x);
                stacks.add(key == firstKey ? stack(firstItem) : key == secondKey ? stack(secondItem) : ItemStack.EMPTY);
            }
        }
        return CraftingInput.of(pattern.getFirst().length(), pattern.size(), stacks);
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private DragonToolRecipeGameTests() {
    }
}
