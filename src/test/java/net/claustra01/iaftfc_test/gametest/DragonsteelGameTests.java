package net.claustra01.iaftfc_test.gametest;

import java.util.List;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.devices.AnvilBlock;
import net.dries007.tfc.common.component.forge.ForgingBonus;
import net.dries007.tfc.common.component.forge.ForgingBonusComponent;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.HeatDefinition;
import net.dries007.tfc.common.recipes.AdvancedShapedRecipe;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.WeldingRecipe;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.data.FluidHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonsteelGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final List<String> ANVIL_RECIPE_NAMES = List.of(
        "sheet", "rod", "axe", "hoe", "pickaxe", "shovel", "sword",
        "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots");
    private static final List<String> TOOL_NAMES = List.of("axe", "hoe", "pickaxe", "shovel", "sword");
    private static final List<String> ARMOR_NAMES = List.of("helmet", "chestplate", "leggings", "boots");

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonsteelHeatCastingAndFluidReferences(GameTestHelper helper) {
        for (Dragonsteel metal : Dragonsteel.values()) {
            String name = metal.getSerializedName();
            Fluid source = fluid("tfc:metal/" + name);
            Fluid flowing = fluid("tfc:metal/flowing_" + name);
            helper.assertValueEqual(source, Dragonsteel.fluid(metal).getSource(),
                "Molten source must retain the published TFC registry ID");
            helper.assertValueEqual(flowing, Dragonsteel.fluid(metal).getFlowing(),
                "Molten flowing fluid must retain the published TFC registry ID");
            helper.assertTrue(new FluidStack(source, 100).is(fluidTag("c:molten_" + name)),
                "Molten source must use its common metal tag");
            helper.assertTrue(new FluidStack(flowing, 100).is(fluidTag("c:molten_" + name)),
                "Flowing molten metal must use its common metal tag");
            helper.assertTrue(new FluidStack(source, 100).is(fluidTag("c:molten_metal")),
                "Molten source must use the aggregate common metal tag");
            helper.assertTrue(new FluidStack(source, 100).is(fluidTag("tfc:molten_metals")),
                "Molten source must use TFC's molten metal tag");
            FluidHeat fluidHeat = FluidHeat.get(source);
            helper.assertTrue(fluidHeat != null, "Molten Dragonsteel must have TFC fluid heat data");
            helper.assertTrue(Math.abs(fluidHeat.meltTemperature() - Dragonsteel.MELT_TEMPERATURE) < 0.01F,
                "Molten Dragonsteel must melt at 2000 C");

            for (Metal.ItemType type : Dragonsteel.ITEM_TYPES) {
                assertHeat(helper, new ItemStack(Dragonsteel.item(metal, type).get()));
                HeatingRecipe heating = heatingRecipe(helper,
                    "iaftfc:heating/" + name + "/" + type.name().toLowerCase());
                assertHeating(helper, heating, source, new ItemStack(Dragonsteel.item(metal, type).get()));
            }
            helper.assertTrue(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.INGOT).get())
                .is(itemTag("c:ingots")), "Dragonsteel ingot must use the aggregate common tag");
            helper.assertTrue(new ItemStack(Dragonsteel.block(metal, Metal.BlockType.BLOCK).get())
                .is(itemTag("c:storage_blocks/" + name)),
                "Dragonsteel plated block item must use its common storage tag");
            helper.assertTrue(Dragonsteel.block(metal, Metal.BlockType.BLOCK).get().defaultBlockState()
                .is(blockTag("c:storage_blocks/" + name)),
                "Dragonsteel plated block must use its common storage block tag");
            for (Metal.BlockType type : Dragonsteel.BLOCK_TYPES) {
                Block block = Dragonsteel.block(metal, type).get();
                assertHeat(helper, new ItemStack(block));
                HeatingRecipe heating = heatingRecipe(helper,
                    "iaftfc:heating/" + name + "/" + type.name().toLowerCase());
                assertHeating(helper, heating, source, new ItemStack(block));
            }

            for (String mold : List.of("ingot", "fire_ingot")) {
                CastingRecipe casting = castingRecipe(helper,
                    "iaftfc:casting/" + name + "_" + mold);
                helper.assertTrue(casting.getFluidIngredient().test(new FluidStack(source, 100)),
                    "Both TFC ingot molds must accept 100 mB of the matching molten metal");
                helper.assertTrue(casting.getResultItem().is(Dragonsteel.item(metal, Metal.ItemType.INGOT).get()),
                    "Casting must produce the matching IaFTFC ingot");
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void tierSixToTierSevenProgressionCannotBeBypassed(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int index = 0; index < Dragonsteel.values().length; index++) {
            Dragonsteel metal = Dragonsteel.values()[index];
            String name = metal.getSerializedName();
            WeldingRecipe firstDouble = weldingRecipe(helper,
                "iaftfc:welding/" + name + "/double_ingot");
            helper.assertValueEqual(firstDouble.getTier(), 6,
                "The first Dragonsteel double ingot must be weldable on tier 6");
            helper.assertTrue(firstDouble.isCorrectTier(6), "Tier 6 must allow the progression weld");
            WeldingInput doubleInput = new WeldingInput(
                new ItemStack(Dragonsteel.item(metal, Metal.ItemType.INGOT).get()),
                new ItemStack(Dragonsteel.item(metal, Metal.ItemType.INGOT).get()), 6);
            helper.assertTrue(firstDouble.matches(doubleInput), "Two matching ingots must weld together");
            helper.assertTrue(firstDouble.assemble(doubleInput)
                .is(Dragonsteel.item(metal, Metal.ItemType.DOUBLE_INGOT).get()),
                "The progression weld must produce a Dragonsteel double ingot");

            BlockPos tierSixPos = new BlockPos(1 + index * 2, 2, 1);
            helper.setBlock(tierSixPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/red_steel")));
            AnvilBlockEntity tierSix = (AnvilBlockEntity)helper.getBlockEntity(tierSixPos);
            helper.assertValueEqual(tierSix.getTier(), 6, "Red Steel anvil must be tier 6");
            tierSix.getInventory().setStackInSlot(0, hot(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.INGOT).get())));
            tierSix.getInventory().setStackInSlot(1, hot(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.INGOT).get())));
            tierSix.getInventory().setStackInSlot(3, new ItemStack(item("tfc:powder/flux")));
            helper.assertTrue(tierSix.weld(player) != InteractionResult.FAIL,
                "Tier 6 anvil must weld the first double ingot");
            ItemStack weldedIngot = tierSix.getInventory().getStackInSlot(0);
            helper.assertTrue(weldedIngot.is(Dragonsteel.item(metal, Metal.ItemType.DOUBLE_INGOT).get()),
                "The real Tier 6 anvil must output a double ingot");

            ShapedRecipe anvilCraft = (ShapedRecipe)recipe(helper, "iaftfc:crafting/metal/anvil/" + name);
            ItemStack doubleIngot = new ItemStack(Dragonsteel.item(metal, Metal.ItemType.DOUBLE_INGOT).get());
            CraftingInput anvilInput = CraftingInput.of(3, 3, List.of(
                doubleIngot.copy(), doubleIngot.copy(), doubleIngot.copy(),
                ItemStack.EMPTY, weldedIngot.copy(), ItemStack.EMPTY,
                doubleIngot.copy(), doubleIngot.copy(), doubleIngot.copy()));
            helper.assertTrue(anvilCraft.matches(anvilInput, helper.getLevel()),
                "Double ingots must form the Dragonsteel anvil recipe");
            helper.assertTrue(anvilCraft.assemble(anvilInput, helper.getLevel().registryAccess())
                .is(Dragonsteel.block(metal, Metal.BlockType.ANVIL).get().asItem()),
                "The crafted anvil must use the Dragonsteel block ID");

            Block anvilBlock = Dragonsteel.block(metal, Metal.BlockType.ANVIL).get();
            helper.assertTrue(anvilBlock instanceof AnvilBlock, "Dragonsteel Anvil must use TFC's AnvilBlock");
            helper.assertValueEqual(((AnvilBlock)anvilBlock).getTier(), Dragonsteel.ANVIL_TIER,
                "Dragonsteel Anvil must be tier 7");
            helper.assertTrue(TFCBlockEntities.ANVIL.get().isValid(anvilBlock.defaultBlockState()),
                "The TFC Anvil block entity type must accept Dragonsteel Anvils");
            BlockPos pos = new BlockPos(1 + index * 2, 2, 1);
            helper.setBlock(pos, anvilBlock);
            helper.assertTrue(helper.getBlockEntity(pos) instanceof AnvilBlockEntity,
                "Placed Dragonsteel Anvil must create the real TFC Anvil block entity");
            AnvilBlockEntity tierSeven = (AnvilBlockEntity)helper.getBlockEntity(pos);

            WeldingRecipe doubleSheet = weldingRecipe(helper,
                "iaftfc:welding/" + name + "/double_sheet");
            helper.assertValueEqual(doubleSheet.getTier(), Dragonsteel.ANVIL_TIER,
                "Double sheet welding must require tier 7");
            helper.assertTrue(!doubleSheet.isCorrectTier(6),
                "Tier 6 must not bypass double sheet welding");
            tierSix.getInventory().setStackInSlot(0, hot(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.SHEET).get())));
            tierSix.getInventory().setStackInSlot(1, hot(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.SHEET).get())));
            tierSix.getInventory().setStackInSlot(3, new ItemStack(item("tfc:powder/flux")));
            helper.assertValueEqual(tierSix.weld(player), InteractionResult.FAIL,
                "Tier 6 anvil must refuse the double sheet weld");
            tierSeven.getInventory().setStackInSlot(0, hot(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.SHEET).get())));
            tierSeven.getInventory().setStackInSlot(1, hot(new ItemStack(Dragonsteel.item(metal, Metal.ItemType.SHEET).get())));
            tierSeven.getInventory().setStackInSlot(3, new ItemStack(item("tfc:powder/flux")));
            helper.assertTrue(tierSeven.weld(player) != InteractionResult.FAIL,
                "Dragonsteel anvil must perform the tier 7 double sheet weld");
            helper.assertTrue(tierSeven.getInventory().getStackInSlot(0)
                .is(Dragonsteel.item(metal, Metal.ItemType.DOUBLE_SHEET).get()),
                "Tier 7 weld must produce a double sheet");
            for (String recipeName : ANVIL_RECIPE_NAMES) {
                AnvilRecipe recipe = anvilRecipe(helper,
                    "iaftfc:anvil/" + name + "/" + recipeName);
                helper.assertValueEqual(recipe.getMinTier(), Dragonsteel.ANVIL_TIER,
                    recipeName + " forging must require tier 7");
                helper.assertTrue(!recipe.isCorrectTier(6),
                    "Tier 6 must not bypass " + recipeName + " forging");
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void forgedPartsAssembleCeEquipmentWithBonus(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Dragonsteel metal : Dragonsteel.values()) {
            String name = metal.getSerializedName();
            BlockPos pos = new BlockPos(1 + metal.ordinal() * 2, 2, 4);
            helper.setBlock(pos, Dragonsteel.block(metal, Metal.BlockType.ANVIL).get());
            AnvilBlockEntity anvil = (AnvilBlockEntity)helper.getBlockEntity(pos);
            for (String toolName : TOOL_NAMES) {
                Recipe<?> loaded = recipe(helper, "iceandfire:" + name + "_" + toolName);
                helper.assertTrue(loaded instanceof AdvancedShapedRecipe,
                    "CE tool recipe must be the TFC forging-bonus assembly");
                AdvancedShapedRecipe assembly = (AdvancedShapedRecipe)loaded;
                Metal.ItemType partType = toolName.equals("sword")
                    ? Metal.ItemType.SWORD_BLADE
                    : Metal.ItemType.valueOf(toolName.toUpperCase() + "_HEAD");
                ItemStack part = new ItemStack(Dragonsteel.item(metal, partType).get());
                ForgingBonusComponent.set(part, ForgingBonus.PERFECT);
                CraftingInput input = CraftingInput.of(1, 2, List.of(part, new ItemStack(Items.STICK)));
                helper.assertTrue(assembly.matches(input, helper.getLevel()),
                    "A forged Dragonsteel part and wooden rod must match the tool assembly");
                ItemStack result = assembly.assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(result.is(item("iceandfire:" + name + "_" + toolName)),
                    "Tool assembly must preserve the CE registry ID");
                helper.assertValueEqual(ForgingBonusComponent.get(result), ForgingBonus.PERFECT,
                    "CE tool must inherit the TFC forging bonus");
                assertHeat(helper, result);
                assertHeating(helper, heatingRecipe(helper, "iaftfc:heating/" + name + "/finished_" + toolName),
                    Dragonsteel.fluid(metal).getSource(), result);
                assertOnlyReplacementRecipe(helper, result.getItem(), AdvancedShapedRecipe.class);
            }

            for (String armorName : ARMOR_NAMES) {
                Recipe<?> loaded = recipe(helper, "iceandfire:" + name + "_" + armorName);
                helper.assertTrue(loaded instanceof WeldingRecipe,
                    "CE armor recipe must be the tier 7 welding recipe");
                WeldingRecipe welding = (WeldingRecipe)loaded;
                ItemStack unfinished = new ItemStack(Dragonsteel.item(metal, unfinishedType(armorName)).get());
                ItemStack sheet = new ItemStack(Dragonsteel.item(metal,
                    armorName.equals("chestplate") ? Metal.ItemType.DOUBLE_SHEET : Metal.ItemType.SHEET).get());
                ForgingBonusComponent.set(unfinished, ForgingBonus.EXPERT);
                ForgingBonusComponent.set(sheet, ForgingBonus.PERFECT);
                anvil.getInventory().setStackInSlot(0, hot(unfinished));
                anvil.getInventory().setStackInSlot(1, hot(sheet));
                anvil.getInventory().setStackInSlot(3, new ItemStack(item("tfc:powder/flux")));
                helper.assertTrue(welding.matches(anvil.getInventory()), "Unfinished armor and matching sheet must weld");
                helper.assertTrue(!welding.isCorrectTier(6), "Tier 6 must not finish Dragonsteel armor");
                helper.assertTrue(anvil.weld(player) != InteractionResult.FAIL,
                    "Dragonsteel anvil must finish CE armor");
                ItemStack result = anvil.getInventory().getStackInSlot(0);
                helper.assertTrue(result.is(item("iceandfire:" + name + "_" + armorName)),
                    "Armor welding must preserve the CE registry ID");
                helper.assertValueEqual(ForgingBonusComponent.get(result), ForgingBonus.PERFECT,
                    "CE armor must inherit the best forging bonus");
                assertHeat(helper, result);
                assertHeating(helper, heatingRecipe(helper, "iaftfc:heating/" + name + "/finished_" + armorName),
                    Dragonsteel.fluid(metal).getSource(), result);
                assertOnlyReplacementRecipe(helper, result.getItem(), WeldingRecipe.class);
            }
        }
        helper.succeed();
    }

    private static void assertHeat(GameTestHelper helper, ItemStack stack) {
        HeatDefinition heat = HeatCapability.getDefinition(stack);
        helper.assertTrue(heat != null, "Every Dragonsteel form must have TFC item heat data");
        helper.assertTrue(Math.abs(heat.forgingTemperature() - Dragonsteel.FORGING_TEMPERATURE) < 0.01F,
            "Dragonsteel must be forgeable at 1200 C");
        helper.assertTrue(Math.abs(heat.weldingTemperature() - Dragonsteel.WELDING_TEMPERATURE) < 0.01F,
            "Dragonsteel must be weldable at 1600 C");
    }

    private static ItemStack hot(ItemStack stack) {
        HeatCapability.setTemperature(stack, Dragonsteel.FORGE_OUTPUT_TEMPERATURE);
        return stack;
    }

    private static void assertHeating(GameTestHelper helper, HeatingRecipe recipe, Fluid source,
        ItemStack input) {
        helper.assertTrue(recipe.matches(input), "Heating recipe must reference its registered form");
        helper.assertTrue(Math.abs(recipe.getTemperature() - Dragonsteel.MELT_TEMPERATURE) < 0.01F,
            "Heating recipe must melt Dragonsteel at 2000 C");
        helper.assertValueEqual(recipe.getDisplayOutputFluid().getFluid(), source,
            "Heating recipe must output the matching published fluid ID");
        helper.assertTrue(recipe.getDisplayOutputFluid().getAmount() > 0,
            "Heating recipe must output a positive metal amount");
    }

    private static void assertOnlyReplacementRecipe(GameTestHelper helper, Item result,
        Class<? extends Recipe<?>> expectedType) {
        List<? extends Recipe<?>> recipes = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
            .map(holder -> holder.value())
            .filter(recipe -> recipe.getResultItem(helper.getLevel().registryAccess()).is(result))
            .toList();
        helper.assertValueEqual(recipes.size(), 1,
            "CE equipment must have no raw-ingot crafting bypass");
        helper.assertTrue(expectedType.isInstance(recipes.getFirst()),
            "The only CE equipment recipe must be the TFC replacement");
    }

    private static Metal.ItemType unfinishedType(String armorName) {
        return switch (armorName) {
            case "helmet" -> Metal.ItemType.UNFINISHED_HELMET;
            case "chestplate" -> Metal.ItemType.UNFINISHED_CHESTPLATE;
            case "leggings" -> Metal.ItemType.UNFINISHED_GREAVES;
            case "boots" -> Metal.ItemType.UNFINISHED_BOOTS;
            default -> throw new IllegalArgumentException(armorName);
        };
    }

    private static HeatingRecipe heatingRecipe(GameTestHelper helper, String id) {
        Recipe<?> recipe = recipe(helper, id);
        helper.assertTrue(recipe instanceof HeatingRecipe, id + " must be a TFC heating recipe");
        return (HeatingRecipe)recipe;
    }

    private static CastingRecipe castingRecipe(GameTestHelper helper, String id) {
        Recipe<?> recipe = recipe(helper, id);
        helper.assertTrue(recipe instanceof CastingRecipe, id + " must be a TFC casting recipe");
        return (CastingRecipe)recipe;
    }

    private static WeldingRecipe weldingRecipe(GameTestHelper helper, String id) {
        Recipe<?> recipe = recipe(helper, id);
        helper.assertTrue(recipe instanceof WeldingRecipe, id + " must be a TFC welding recipe");
        return (WeldingRecipe)recipe;
    }

    private static AnvilRecipe anvilRecipe(GameTestHelper helper, String id) {
        Recipe<?> recipe = recipe(helper, id);
        helper.assertTrue(recipe instanceof AnvilRecipe, id + " must be a TFC anvil recipe");
        return (AnvilRecipe)recipe;
    }

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().getRecipeManager().byKey(ResourceLocation.parse(id))
            .orElseThrow(() -> new AssertionError("Missing recipe " + id)).value();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
    }

    private static TagKey<Fluid> fluidTag(String id) {
        return TagKey.create(Registries.FLUID, ResourceLocation.parse(id));
    }

    private static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    private static TagKey<Block> blockTag(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(id));
    }

    private record WeldingInput(ItemStack main, ItemStack secondary, int tier)
        implements WeldingRecipe.Inventory {
        @Override
        public ItemStack getMain() {
            return main;
        }

        @Override
        public ItemStack getSecondary() {
            return secondary;
        }

        @Override
        public int getTier() {
            return tier;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? main : secondary;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    private DragonsteelGameTests() {
    }
}
