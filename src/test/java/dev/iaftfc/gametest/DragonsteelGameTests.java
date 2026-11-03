package dev.iaftfc.gametest;

import dev.iaftfc.metal.Dragonsteel;
import com.iafenvoy.iceandfire.item.tool.ActivePostHitSwordItem;
import com.iafenvoy.iceandfire.item.armor.DragonSteelArmorItem;
import com.iafenvoy.iceandfire.registry.IafTiers;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.blockentities.BellowsBlockEntity;
import net.dries007.tfc.common.blockentities.CharcoalForgeBlockEntity;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blocks.devices.BellowsBlock;
import net.dries007.tfc.common.blocks.devices.CharcoalForgeBlock;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.forge.ForgeStep;
import net.dries007.tfc.common.component.forge.ForgeRule;
import net.dries007.tfc.common.component.forge.ForgingBonus;
import net.dries007.tfc.common.component.forge.ForgingBonusComponent;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.mold.IMold;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.WeldingRecipe;
import net.dries007.tfc.util.data.FluidHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonsteelGameTests {
    @GameTest(template = "empty")
    public static void metalRegistryAndHeat(GameTestHelper helper) {
        Set<ResourceLocation> expectedItems = new HashSet<>();
        Set<ResourceLocation> expectedBlocks = new HashSet<>();
        Set<ResourceLocation> expectedFluids = new HashSet<>();
        for (String metal : Dragonsteel.METALS) {
            for (String part : Dragonsteel.PARTS) {
                expectedItems.add(ResourceLocation.parse("iaftfc:metal/" + part + "/" + metal));
                ItemStack stack = stack(part, metal);
                helper.assertTrue(!stack.isEmpty(), "Missing " + part + " " + metal);
                var heat = HeatCapability.getDefinition(stack);
                helper.assertTrue(heat != null && heat.forgingTemperature() == 1200
                    && heat.weldingTemperature() == 1600, "Wrong heat definition: " + part + " " + metal);
                helper.assertTrue(Math.abs(heat.heatCapacity() - 2.857143f
                    * HeatingRecipe.getRecipe(stack).assembleFluid(stack).getAmount() / 100) < 0.0001,
                    "Wrong heat capacity: " + part + " " + metal);
                helper.assertTrue(HeatingRecipe.getRecipe(stack) != null
                    && HeatingRecipe.getRecipe(stack).getTemperature() == 2000,
                    "Wrong melting point: " + part + " " + metal);
                ItemStack red = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                    "tfc:metal/" + part + "/red_steel")));
                helper.assertTrue(ItemSizeManager.get(stack).getSize(stack) == ItemSizeManager.get(red).getSize(red)
                    && ItemSizeManager.get(stack).getWeight(stack) == ItemSizeManager.get(red).getWeight(red),
                    "Size or weight differs from Red Steel " + part + " " + metal);
            }
            var parts = Dragonsteel.METAL_PARTS.get(metal);
            for (String part : List.of("block", "anvil")) {
                ResourceLocation id = ResourceLocation.parse("iaftfc:metal/" + part + "/" + metal);
                expectedItems.add(id);
                expectedBlocks.add(id);
                ItemStack heated = stack(part, metal);
                helper.assertTrue(Math.abs(HeatCapability.getDefinition(heated).heatCapacity() - 2.857143f
                    * HeatingRecipe.getRecipe(heated).assembleFluid(heated).getAmount() / 100) < 0.0001,
                    "Wrong heat capacity: " + part + " " + metal);
            }
            expectedItems.add(ResourceLocation.parse("iaftfc:bucket/metal/" + metal));
            expectedBlocks.add(ResourceLocation.parse("iaftfc:fluid/metal/" + metal));
            expectedFluids.add(ResourceLocation.parse("iaftfc:metal/" + metal));
            expectedFluids.add(ResourceLocation.parse("iaftfc:metal/flowing_" + metal));
            helper.assertTrue(parts.anvil().get().getTier() == 7, "Anvil tier is not 7");
            ItemStack ourAnvil = stack("anvil", metal);
            ItemStack redAnvil = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/anvil/red_steel")));
            helper.assertTrue(ItemSizeManager.get(ourAnvil).getSize(ourAnvil) == ItemSizeManager.get(redAnvil).getSize(redAnvil)
                && ItemSizeManager.get(ourAnvil).getWeight(ourAnvil) == ItemSizeManager.get(redAnvil).getWeight(redAnvil),
                "Anvil size differs from Red Steel");
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(ResourceLocation.parse("iaftfc:metal/" + metal)),
                "Missing molten source");
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(ResourceLocation.parse("iaftfc:metal/flowing_" + metal)),
                "Missing molten flowing fluid");
            FluidHeat fluidHeat = FluidHeat.get(parts.fluid().getSource());
            helper.assertTrue(fluidHeat != null && fluidHeat.meltTemperature() == 2000
                && Math.abs(fluidHeat.specificHeatCapacity() - 0.008571429f) < 0.000001,
                "Wrong fluid heat: " + metal);
        }
        helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals("iaftfc")
            && id.getPath().contains("dragonsteel_")).collect(java.util.stream.Collectors.toSet()).equals(expectedItems),
            "Dragonsteel item registry differs from specified set");
        helper.assertTrue(BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals("iaftfc")
            && id.getPath().contains("dragonsteel_")).collect(java.util.stream.Collectors.toSet()).equals(expectedBlocks),
            "Dragonsteel block registry differs from specified set");
        helper.assertTrue(BuiltInRegistries.FLUID.keySet().stream().filter(id -> id.getNamespace().equals("iaftfc")
            && id.getPath().contains("dragonsteel_")).collect(java.util.stream.Collectors.toSet()).equals(expectedFluids),
            "Dragonsteel fluid registry differs from specified set");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meltingAndAmounts(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            for (String part : Dragonsteel.PARTS) {
                int amount = switch (part) {
                    case "rod" -> 50;
                    case "double_ingot", "sheet", "sword_blade", "unfinished_boots" -> 200;
                    case "double_sheet", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves" -> 400;
                    default -> 100;
                };
                HeatingRecipe recipe = HeatingRecipe.getRecipe(stack(part, metal));
                helper.assertTrue(recipe.assembleFluid(stack(part, metal)).getAmount() == amount,
                    "Metal amount changed for " + part + " " + metal);
                helper.assertTrue(recipe.assembleFluid(stack(part, metal)).getFluid()
                    == Dragonsteel.METAL_PARTS.get(metal).fluid().getSource(), "Metal attribute crossed");
                helper.assertTrue(!recipe.isValidTemperature(1999) && recipe.isValidTemperature(2000),
                    "Melting temperature boundary changed");
            }
            for (String part : List.of("block", "anvil")) {
                ItemStack stack = stack(part, metal);
                HeatingRecipe recipe = HeatingRecipe.getRecipe(stack);
                helper.assertTrue(recipe != null && recipe.assembleFluid(stack).getAmount()
                    == (part.equals("anvil") ? 1400 : 100), "Block or anvil metal amount changed");
            }
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                ResourceLocation.parse("iaftfc:casting/" + metal + "_ingot")).isPresent(), "Missing ingot mold casting");
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                ResourceLocation.parse("iaftfc:casting/" + metal + "_fire_ingot")).isPresent(), "Missing fire ingot mold casting");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void metalBlocksDropWithPickaxe(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("tfc:metal/pickaxe/red_steel"))));
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("block", "anvil")) {
                BlockPos position = new BlockPos(1, 1, 1);
                helper.setBlock(position, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                    "iaftfc:metal/" + part + "/" + metal)));
                BlockPos absolute = helper.absolutePos(position);
                helper.assertTrue(player.gameMode.destroyBlock(absolute), "Pickaxe could not break " + part);
                List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(absolute).inflate(2));
                helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(stack(part, metal).getItem())
                    && drops.getFirst().getItem().getCount() == 1, "Wrong metal block drop: " + part + " " + metal);
                drops.forEach(ItemEntity::discard);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ingotMoldRecasting(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            for (String moldId : List.of("ingot_mold", "fire_ingot_mold")) {
                ItemStack moldStack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:ceramic/" + moldId)));
                IMold mold = IMold.get(moldStack);
                helper.assertTrue(mold != null, "TFC ingot mold has no fluid capability");
                FluidStack molten = new FluidStack(Dragonsteel.METAL_PARTS.get(metal).fluid().getSource(), 100);
                helper.assertTrue(mold.fill(molten, IFluidHandler.FluidAction.EXECUTE) == 100,
                    "TFC ingot mold rejected Dragonsteel fluid");
                mold.setTemperature(20);
                CastingRecipe casting = CastingRecipe.get(mold);
                helper.assertTrue(casting != null && casting.matches(mold), "No casting recipe for Dragonsteel");
                ItemStack cast = casting.assemble(mold);
                helper.assertTrue(cast.is(stack("ingot", metal).getItem()) && cast.getCount() == 1,
                    "Casting changed metal element or count");
                helper.assertTrue(HeatingRecipe.getRecipe(cast).assembleFluid(cast).is(molten.getFluid())
                    && HeatingRecipe.getRecipe(cast).assembleFluid(cast).getAmount() == 100,
                    "Recasting did not preserve 100 mB");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 4700)
    public static void charcoalForgeMelting(GameTestHelper helper) {
        int[] positions = {2, 5, 8, 11, 14};
        String[] metals = {Dragonsteel.METALS[0], Dragonsteel.METALS[1], Dragonsteel.METALS[2],
            Dragonsteel.METALS[0], Dragonsteel.METALS[1]};
        for (int index = 0; index < positions.length; index++) {
            int x = positions[index];
            BlockPos forgePos = new BlockPos(x, 2, 3);
            for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN)) {
                helper.setBlock(forgePos.relative(direction),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:rock/raw/basalt")));
            }
            helper.setBlock(forgePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:charcoal_forge")));
            helper.setBlock(forgePos.above(), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:crucible")));
            CharcoalForgeBlockEntity forge = (CharcoalForgeBlockEntity) helper.getBlockEntity(forgePos);
            CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(forgePos.above());
            helper.assertTrue(CharcoalForgeBlock.isValid(helper.getLevel(), helper.absolutePos(forgePos)),
                "Charcoal Forge multiblock is invalid");
            ItemStack fuel = new ItemStack(index == 3 ? Items.CHARCOAL : Items.COAL);
            for (int slot = 0; slot < 5; slot++) {
                forge.getInventory().setStackInSlot(slot, fuel.copy());
            }
            crucible.getInventory().setStackInSlot(0, stack("ingot", metals[index]));
            helper.assertTrue(forge.light(helper.getLevel().getBlockState(helper.absolutePos(forgePos))),
                "Charcoal Forge could not be lit");
            if (index < 4) {
                BlockPos bellowsPos = new BlockPos(x + 1, 3, 3);
                helper.setBlock(bellowsPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:bellows"))
                    .defaultBlockState().setValue(BellowsBlock.FACING, Direction.WEST));
                BellowsBlockEntity bellows = (BellowsBlockEntity) helper.getBlockEntity(bellowsPos);
                pushBellows(helper, bellows, 230);
            }
        }
        helper.runAfterDelay(2400, () -> {
            for (int index = 0; index < positions.length; index++) {
                int x = positions[index];
                CharcoalForgeBlockEntity forge = (CharcoalForgeBlockEntity) helper.getBlockEntity(new BlockPos(x, 2, 3));
                CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(new BlockPos(x, 3, 3));
                if (index < 3) {
                    helper.assertTrue(forge.getTemperature() > 2000 && crucible.getTemperature() >= 2000,
                        "Coal plus Bellows did not heat Crucible above melting point: forge="
                            + forge.getTemperature() + " crucible=" + crucible.getTemperature()
                            + " air=" + forge.getAirTicks());
                    helper.assertTrue(crucible.getAlloy().getAmount() == 100
                        && crucible.getAlloyResult().getFluid() == Dragonsteel.METAL_PARTS.get(metals[index]).fluid().getSource(),
                        "Crucible did not melt the ingot into 100 mB of its own metal");
                    CompoundTag saved = crucible.saveWithFullMetadata(helper.getLevel().registryAccess());
                    BlockEntity reloaded = BlockEntity.loadStatic(crucible.getBlockPos(), crucible.getBlockState(),
                        saved, helper.getLevel().registryAccess());
                    helper.assertTrue(reloaded instanceof CrucibleBlockEntity, "Crucible NBT did not reload");
                    CrucibleBlockEntity restored = (CrucibleBlockEntity) reloaded;
                    helper.assertTrue(restored.getAlloy().getAmount() == 100
                        && restored.getTemperature() == crucible.getTemperature()
                        && restored.getAlloy().getContent().containsKey(Dragonsteel.METAL_PARTS.get(metals[index]).fluid().getSource()),
                        "Crucible NBT lost heat or molten metal amount");
                } else {
                    helper.assertTrue(crucible.getAlloy().getAmount() == 0
                        && crucible.getInventory().getStackInSlot(0).is(stack("ingot", metals[index]).getItem()),
                        "Charcoal or unblown Coal melted Dragonsteel");
                    helper.assertTrue(forge.getTemperature() < 2000,
                        "Lower heat source exceeded Dragonsteel melting point");
                    helper.assertTrue(forge.getTemperature() <= (index == 3 ? 1950 : 1415),
                        "TFC fuel or Bellows exceeded its specified target temperature");
                    helper.assertTrue(forge.getTemperature() >= (index == 3 ? 1850 : 1350),
                        "Forge did not approach its specified fuel target temperature");
                    helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, 2, 3)))
                        .getValue(CharcoalForgeBlock.HEAT) > 0, "Forge went out before the negative check");
                }
            }
            for (int index = 0; index < 3; index++) {
                CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(new BlockPos(positions[index], 3, 3));
                IFluidHandler output = crucible.getSidedFluidInventory(Direction.NORTH);
                FluidStack poured = output.drain(100, IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(poured.getAmount() == 100
                    && poured.getFluid() == Dragonsteel.METAL_PARTS.get(metals[index]).fluid().getSource(),
                    "Crucible could not pour 100 mB into its own mold");
                IMold mold = IMold.get(new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:ceramic/ingot_mold"))));
                helper.assertTrue(mold.fill(poured, IFluidHandler.FluidAction.EXECUTE) == 100,
                    "Crucible output did not fill the TFC ingot mold");
                mold.setTemperature(20);
                CastingRecipe casting = CastingRecipe.get(mold);
                helper.assertTrue(casting != null && casting.matches(mold), "Poured mold could not cool and cast");
                ItemStack cast = casting.assemble(mold);
                helper.assertTrue(cast.is(stack("ingot", metals[index]).getItem()) && cast.getCount() == 1,
                    "Poured mold returned the wrong ingot");
                crucible.getInventory().setStackInSlot(0, cast);
            }
            helper.runAfterDelay(1000, () -> {
                for (int index = 0; index < 3; index++) {
                    CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(new BlockPos(positions[index], 3, 3));
                    helper.assertTrue(crucible.getAlloy().getAmount() == 100
                        && crucible.getAlloyResult().getFluid() == Dragonsteel.METAL_PARTS.get(metals[index]).fluid().getSource(),
                        "Cast ingot did not re-melt into 100 mB of its own fluid: amount="
                            + crucible.getAlloy().getAmount() + " temperature=" + crucible.getTemperature()
                            + " item=" + crucible.getInventory().getStackInSlot(0)
                            + " itemTemperature=" + HeatCapability.getTemperature(crucible.getInventory().getStackInSlot(0)));
                }
                for (int index = 0; index < 2; index++) {
                    CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(new BlockPos(positions[index], 3, 3));
                    helper.assertTrue(crucible.getSidedFluidInventory(Direction.NORTH).drain(100,
                        IFluidHandler.FluidAction.EXECUTE).getAmount() == 100, "Could not empty re-melted ingot");
                    if (index == 0) {
                        @SuppressWarnings("unchecked")
                        Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) helper.getLevel().getServer().getRecipeManager()
                            .byKey(ResourceLocation.parse("iaftfc:crafting/metal/block/" + metals[index])).orElseThrow().value();
                        ItemStack sheet = stack("sheet", metals[index]);
                        CraftingInput input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, sheet.copy(),
                            new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/hammer/red_steel"))),
                            sheet.copy(), new ItemStack(Items.OAK_PLANKS), sheet.copy(), ItemStack.EMPTY,
                            sheet.copy(), ItemStack.EMPTY));
                        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Block recipe did not match four sheets");
                        ItemStack crafted = recipe.assemble(input, helper.getLevel().registryAccess());
                        helper.assertTrue(crafted.getCount() == 8, "Block recipe did not make eight blocks");
                        for (int slot = 0; slot < 8; slot++) {
                            ItemStack heated = crafted.copyWithCount(1);
                            HeatCapability.setTemperature(heated, 1999);
                            crucible.getInventory().setStackInSlot(slot, heated);
                        }
                    } else {
                        ItemStack heated = stack("anvil", metals[index]);
                        HeatCapability.setTemperature(heated, 1999);
                        crucible.getInventory().setStackInSlot(0, heated);
                    }
                }
                helper.runAfterDelay(1000, () -> {
                    for (int index = 0; index < 2; index++) {
                        CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(new BlockPos(positions[index], 3, 3));
                        helper.assertTrue(crucible.getAlloy().getAmount() == (index == 0 ? 800 : 1400)
                            && crucible.getAlloyResult().getFluid()
                                == Dragonsteel.METAL_PARTS.get(metals[index]).fluid().getSource(),
                            "Crucible did not preserve block or anvil metal amount: amount="
                                + crucible.getAlloy().getAmount() + " temperature=" + crucible.getTemperature()
                                + " input=" + crucible.getInventory().getStackInSlot(0) + " inputTemperature="
                                + HeatCapability.getTemperature(crucible.getInventory().getStackInSlot(0)));
                    }
                    helper.succeed();
                });
            });
        });
    }

    private static void pushBellows(GameTestHelper helper, BellowsBlockEntity bellows, int remaining) {
        if (remaining == 0) {
            return;
        }
        helper.runAfterDelay(20, () -> {
            bellows.onRightClick();
            pushBellows(helper, bellows, remaining - 1);
        });
    }

    @GameTest(template = "empty")
    public static void itemHeatPersistence(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            for (String part : Dragonsteel.PARTS) {
                ItemStack stack = stack(part, metal);
                var heat = HeatCapability.get(stack);
                helper.assertTrue(heat != null, "Missing heat capability");
                HeatCapability.setStaticTemperature(stack, 1199);
                helper.assertTrue(!HeatCapability.view(stack).canWork(), "Forging below 1200");
                HeatCapability.setStaticTemperature(stack, 1200);
                helper.assertTrue(HeatCapability.view(stack).canWork(), "Forging at 1200");
                HeatCapability.setStaticTemperature(stack, 1599);
                helper.assertTrue(!HeatCapability.view(stack).canWeld(), "Welding below 1600");
                HeatCapability.setStaticTemperature(stack, 1600);
                helper.assertTrue(HeatCapability.view(stack).canWeld(), "Welding at 1600");
                CompoundTag saved = (CompoundTag) stack.save(helper.getLevel().registryAccess());
                ItemStack loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
                helper.assertTrue(loaded.is(stack.getItem()) && HeatCapability.getTemperature(loaded) == 1600,
                    "Saved item lost metal or heat");
                helper.assertTrue(HeatingRecipe.getRecipe(loaded).assembleFluid(loaded).getAmount()
                    == HeatingRecipe.getRecipe(stack).assembleFluid(stack).getAmount(), "Saved item lost metal amount");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tierSixToSeven(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            WeldingRecipe weld = recipe(helper, "welding/metal/double_ingot/" + metal, WeldingRecipe.class);
            helper.assertTrue(weld.getTier() == 6 && weld.isCorrectTier(6), "First double ingot needs tier 6");
            AnvilRecipe sheet = recipe(helper, "anvil/metal/sheet/" + metal, AnvilRecipe.class);
            helper.assertTrue(!sheet.isCorrectTier(6) && sheet.isCorrectTier(7), "Sheet tier is wrong");
            Block block = Dragonsteel.METAL_PARTS.get(metal).anvil().get();
            helper.setBlock(new BlockPos(1, 1, 1), block);
            helper.assertTrue(helper.getBlockEntity(new BlockPos(1, 1, 1)) instanceof AnvilBlockEntity,
                "Dragonsteel anvil lacks TFC block entity");
            AnvilBlockEntity anvil = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
            helper.assertTrue(anvil.getTier() == 7, "Dragonsteel block entity cannot forge tier 7");
            var crafting = helper.getLevel().getServer().getRecipeManager().byKey(
                ResourceLocation.parse("iaftfc:crafting/metal/anvil/" + metal));
            helper.assertTrue(crafting.isPresent() && crafting.get().value().getResultItem(helper.getLevel().registryAccess()).is(block.asItem()),
                "Seven double ingots do not yield the anvil");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilAndBlockCrafting(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            @SuppressWarnings("unchecked")
            Recipe<CraftingInput> anvil = (Recipe<CraftingInput>) helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.parse("iaftfc:crafting/metal/anvil/" + metal)).orElseThrow().value();
            ItemStack doubleIngot = stack("double_ingot", metal);
            CraftingInput anvilInput = CraftingInput.of(3, 3, List.of(doubleIngot.copy(), doubleIngot.copy(),
                doubleIngot.copy(), ItemStack.EMPTY, doubleIngot.copy(), ItemStack.EMPTY,
                doubleIngot.copy(), doubleIngot.copy(), doubleIngot.copy()));
            helper.assertTrue(anvil.matches(anvilInput, helper.getLevel()), "Seven double ingots did not match anvil recipe");
            ItemStack anvilOutput = anvil.assemble(anvilInput, helper.getLevel().registryAccess());
            helper.assertTrue(anvilOutput.is(stack("anvil", metal).getItem()) && anvilOutput.getCount() == 1,
                "Anvil recipe gave wrong output");

            @SuppressWarnings("unchecked")
            Recipe<CraftingInput> block = (Recipe<CraftingInput>) helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.parse("iaftfc:crafting/metal/block/" + metal)).orElseThrow().value();
            ItemStack sheet = stack("sheet", metal);
            ItemStack hammer = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/hammer/red_steel")));
            CraftingInput blockInput = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, sheet.copy(), hammer,
                sheet.copy(), new ItemStack(Items.OAK_PLANKS), sheet.copy(), ItemStack.EMPTY, sheet.copy(), ItemStack.EMPTY));
            helper.assertTrue(block.matches(blockInput, helper.getLevel()), "Four sheets did not match block recipe");
            ItemStack blockOutput = block.assemble(blockInput, helper.getLevel().registryAccess());
            helper.assertTrue(blockOutput.is(stack("block", metal).getItem()) && blockOutput.getCount() == 8,
                "Four sheets did not make eight blocks");
            ItemStack returnedHammer = block.getRemainingItems(blockInput).get(2);
            helper.assertTrue(returnedHammer.is(hammer.getItem()) && returnedHammer.getDamageValue() > hammer.getDamageValue(),
                "Block recipe did not damage and return hammer");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weldSevenIngots(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 1),
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/black_steel")));
        AnvilBlockEntity tooLow = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(3, 1, 1));
        for (String anvilMetal : List.of("red_steel", "blue_steel")) {
            helper.setBlock(new BlockPos(1, 1, 1),
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/" + anvilMetal)));
            AnvilBlockEntity anvil = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
            helper.assertTrue(anvil.getTier() == 6 && tooLow.getTier() == 5, "Anvil tier boundary changed");
            for (String metal : Dragonsteel.METALS) {
                ItemStack rejected = stack("ingot", metal);
                HeatCapability.setStaticTemperature(rejected, 1600);
                tooLow.getInventory().setStackInSlot(0, rejected);
                tooLow.getInventory().setStackInSlot(1, rejected.copy());
                tooLow.getInventory().setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:metal/hammer/red_steel"))));
                tooLow.getInventory().setStackInSlot(3, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:powder/flux"))));
                helper.assertTrue(!tooLow.canWeldNow(), "Tier 5 anvil accepted Dragonsteel welding");
                List<ItemStack> welded = new java.util.ArrayList<>();
                for (int index = 0; index < 7; index++) {
                    ItemStack first = stack("ingot", metal);
                    ItemStack second = stack("ingot", metal);
                    HeatCapability.setStaticTemperature(first, 1600);
                    HeatCapability.setStaticTemperature(second, 1600);
                    anvil.getInventory().setStackInSlot(0, first);
                    anvil.getInventory().setStackInSlot(1, second);
                    anvil.getInventory().setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse("tfc:metal/hammer/red_steel"))));
                    anvil.getInventory().setStackInSlot(3, new ItemStack(BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse("tfc:powder/flux"))));
                    helper.assertTrue(anvil.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
                        "Tier 6 anvil did not weld Dragonsteel ingots");
                    helper.assertTrue(anvil.getInventory().getStackInSlot(0).is(stack("double_ingot", metal).getItem())
                        && anvil.getInventory().getStackInSlot(1).isEmpty(), "Welding did not consume two ingots");
                    welded.add(anvil.getInventory().getStackInSlot(0).copy());
                }
                @SuppressWarnings("unchecked")
                Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) helper.getLevel().getServer().getRecipeManager()
                    .byKey(ResourceLocation.parse("iaftfc:crafting/metal/anvil/" + metal)).orElseThrow().value();
                CraftingInput input = CraftingInput.of(3, 3, List.of(welded.get(0), welded.get(1), welded.get(2),
                    ItemStack.EMPTY, welded.get(3), ItemStack.EMPTY, welded.get(4), welded.get(5), welded.get(6)));
                helper.assertTrue(recipe.matches(input, helper.getLevel()), "Welded double ingots cannot craft anvil");
                ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(output.is(stack("anvil", metal).getItem()) && output.getCount() == 1,
                    "Seven welded ingots did not produce one anvil");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toolHeadRules(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("pickaxe_head", "shovel_head", "axe_head", "hoe_head", "sword_blade")) {
                AnvilRecipe recipe = recipe(helper, "anvil/metal/" + part + "/" + metal, AnvilRecipe.class);
                AnvilRecipe red = tfcRecipe(helper, "anvil/metal/" + part + "/red_steel", AnvilRecipe.class);
                helper.assertTrue(!recipe.isCorrectTier(6) && recipe.isCorrectTier(7), "Wrong head forging tier");
                helper.assertTrue(recipe.getRules().equals(red.getRules()) && recipe.shouldApplyForgingBonus()
                    == red.shouldApplyForgingBonus(), "Head rules or bonus differ from Red Steel");
                helper.assertTrue(recipe.getInput().test(stack(part.equals("sword_blade") ? "double_ingot" : "ingot", metal)),
                    "Wrong head material");
                helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(stack(part, metal).getItem()),
                    "Wrong head output");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeHeadsAtTierSeven(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Dragonsteel.METAL_PARTS.get("dragonsteel_fire").anvil().get());
        helper.setBlock(new BlockPos(2, 1, 1),
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/red_steel")));
        AnvilBlockEntity anvil = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        AnvilBlockEntity low = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("pickaxe_head", "shovel_head", "axe_head", "hoe_head", "sword_blade")) {
                ItemStack input = stack(part.equals("sword_blade") ? "double_ingot" : "ingot", metal);
                HeatCapability.setStaticTemperature(input, 1600);
                anvil.getInventory().setStackInSlot(0, input);
                anvil.getInventory().setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:metal/hammer/red_steel"))));
                ResourceLocation id = ResourceLocation.parse("iaftfc:anvil/metal/" + part + "/" + metal);
                low.getInventory().setStackInSlot(0, input.copy());
                low.getInventory().setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:metal/hammer/red_steel"))));
                low.chooseRecipe(id);
                helper.assertTrue(low.getTier() == 6 && anvil.getTier() == 7,
                    "Anvil tiers do not straddle the recipe tier");
                helper.assertTrue(!low.workRemotely(ForgeStep.DRAW, 1, false)
                    && low.getInventory().getStackInSlot(0).is(input.getItem()),
                    "Tier 6 forged a Dragonsteel head");
                anvil.chooseRecipe(id);
                int target = anvil.getMainInputForging().target();
                helper.assertTrue(target >= 0 && target <= 150, "Invalid forging target");
                ForgeStep[] steps = switch (part) {
                    case "pickaxe_head" -> new ForgeStep[] {ForgeStep.BEND, ForgeStep.DRAW, ForgeStep.PUNCH};
                    case "shovel_head" -> new ForgeStep[] {ForgeStep.HIT_LIGHT, ForgeStep.PUNCH};
                    case "axe_head" -> new ForgeStep[] {ForgeStep.UPSET, ForgeStep.HIT_LIGHT, ForgeStep.PUNCH};
                    case "hoe_head" -> new ForgeStep[] {ForgeStep.BEND, ForgeStep.HIT_LIGHT, ForgeStep.PUNCH};
                    default -> new ForgeStep[] {ForgeStep.BEND, ForgeStep.BEND, ForgeStep.HIT_LIGHT};
                };
                helper.assertTrue(anvil.workRemotely(ForgeStep.DRAW, target - steps[steps.length - 1].step(),
                    steps[steps.length - 1].step() > 0),
                    "Could not advance forging");
                for (int index = 0; index < steps.length - 1; index++) {
                    anvil.workRemotely(steps[index], 0, false);
                }
                var forging = anvil.getMainInputForging();
                anvil.work(FakePlayerFactory.getMinecraft(helper.getLevel()), steps[steps.length - 1]);
                ItemStack output = anvil.getInventory().getStackInSlot(0);
                helper.assertTrue(output.is(stack(part, metal).getItem()) && output.getCount() == 1,
                    "Tier 7 anvil did not produce " + part + " " + metal + ": work="
                        + anvil.getMainInputForging().work() + " target=" + target + " last="
                        + anvil.getMainInputForging().lastSteps() + " output=" + output);
                AnvilRecipe recipe = recipe(helper, "anvil/metal/" + part + "/" + metal, AnvilRecipe.class);
                ForgingBonus expected = ForgingBonus.byRatio((float) forging.totalWorked()
                    / ForgeRule.calculateOptimalStepsToTarget(target, recipe.getRules()));
                helper.assertTrue(ForgingBonusComponent.get(output) == expected,
                    "Tier 7 output has the wrong forging bonus");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorWelding(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("helmet", "chestplate", "greaves", "boots")) {
                String unfinished = "unfinished_" + part;
                AnvilRecipe forge = recipe(helper, "anvil/metal/" + unfinished + "/" + metal, AnvilRecipe.class);
                AnvilRecipe redForge = tfcRecipe(helper, "anvil/metal/" + unfinished + "/red_steel", AnvilRecipe.class);
                helper.assertTrue(forge.isCorrectTier(7) && !forge.isCorrectTier(6)
                    && forge.getRules().equals(redForge.getRules())
                    && forge.shouldApplyForgingBonus() == redForge.shouldApplyForgingBonus(),
                    "Unfinished armor forging differs from Red Steel");
                WeldingRecipe weld = recipe(helper, "welding/metal/" + part + "/" + metal, WeldingRecipe.class);
                helper.assertTrue(weld.getTier() == 6 && weld.getFirstInput().test(stack(unfinished, metal))
                    && weld.getSecondInput().test(stack(part.equals("chestplate") ? "double_sheet" : "sheet", metal)),
                    "Armor welding inputs or tier differ");
                String finalPart = part.equals("greaves") ? "leggings" : part;
                helper.assertTrue(weld.getResultItem(helper.getLevel().registryAccess()).is(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + metal + "_" + finalPart))),
                    "Armor welding does not produce the IaF item");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeAndWeldArmor(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Dragonsteel.METAL_PARTS.get("dragonsteel_fire").anvil().get());
        helper.setBlock(new BlockPos(2, 1, 1),
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/red_steel")));
        AnvilBlockEntity high = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        AnvilBlockEntity low = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("helmet", "chestplate", "greaves", "boots")) {
                String inputPart = part.equals("boots") ? "sheet" : "double_sheet";
                ItemStack input = stack(inputPart, metal);
                HeatCapability.setStaticTemperature(input, 1600);
                high.getInventory().setStackInSlot(0, input);
                high.getInventory().setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:metal/hammer/red_steel"))));
                high.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/metal/unfinished_" + part + "/" + metal));
                int target = high.getMainInputForging().target();
                helper.assertTrue(high.workRemotely(ForgeStep.DRAW, target, true), "Cannot start armor forging");
                ForgeStep[] steps = switch (part) {
                    case "helmet" -> new ForgeStep[] {ForgeStep.BEND, ForgeStep.BEND, ForgeStep.HIT_LIGHT};
                    case "chestplate" -> new ForgeStep[] {ForgeStep.UPSET, ForgeStep.HIT_LIGHT, ForgeStep.HIT_LIGHT};
                    case "greaves" -> new ForgeStep[] {ForgeStep.HIT_LIGHT, ForgeStep.DRAW, ForgeStep.BEND};
                    default -> new ForgeStep[] {ForgeStep.SHRINK, ForgeStep.BEND, ForgeStep.BEND};
                };
                for (ForgeStep step : steps) {
                    high.workRemotely(step, 0, false);
                }
                ItemStack unfinished = high.getInventory().getStackInSlot(0);
                helper.assertTrue(unfinished.is(stack("unfinished_" + part, metal).getItem()),
                    "Tier 7 did not forge unfinished " + part);
                ItemStack second = stack(part.equals("chestplate") ? "double_sheet" : "sheet", metal);
                HeatCapability.setStaticTemperature(unfinished, 1650);
                HeatCapability.setStaticTemperature(second, 1700);
                ForgingBonusComponent.set(unfinished, ForgingBonus.MODEST);
                ForgingBonusComponent.set(second, ForgingBonus.EXPERT);
                low.getInventory().setStackInSlot(0, unfinished.copy());
                low.getInventory().setStackInSlot(1, second);
                low.getInventory().setStackInSlot(3, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:powder/flux"))));
                helper.assertTrue(low.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
                    "Tier 6 failed to weld unfinished " + part);
                String finalPart = part.equals("greaves") ? "leggings" : part;
                ItemStack output = low.getInventory().getStackInSlot(0);
                helper.assertTrue(output.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + metal + "_" + finalPart)))
                    && output.getCount() == 1 && low.getInventory().getStackInSlot(1).isEmpty()
                    && low.getInventory().getStackInSlot(3).isEmpty(), "Welding duplicated armor materials or flux");
                helper.assertTrue(ForgingBonusComponent.get(output) == ForgingBonus.EXPERT,
                    "Armor welding did not copy the best forging bonus");
                helper.assertTrue(HeatCapability.getTemperature(output) == 1700,
                    "Armor welding did not preserve the warmer input temperature");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assembleDragonsteelTools(GameTestHelper helper) {
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) {
                String headPart = part.equals("sword") ? "sword_blade" : part + "_head";
                @SuppressWarnings("unchecked")
                Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) helper.getLevel().getServer().getRecipeManager()
                    .byKey(ResourceLocation.parse("iaftfc:crafting/metal/" + part + "/" + metal)).orElseThrow().value();
                ItemStack head = stack(headPart, metal);
                ForgingBonusComponent.set(head, ForgingBonus.EXPERT);
                ItemStack woodenRod = new ItemStack(Items.STICK);
                CraftingInput input = CraftingInput.of(1, 2, List.of(head, woodenRod));
                helper.assertTrue(recipe.matches(input, helper.getLevel()), "Wooden rod did not assemble " + part);
                ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(output.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:" + metal + "_" + part)))
                    && output.getCount() == 1 && ForgingBonusComponent.get(output) == ForgingBonus.EXPERT,
                    "Tool assembly lost its IaF item or forging bonus");
                helper.assertTrue(!recipe.matches(CraftingInput.of(1, 2, List.of(head,
                    stack("rod", metal))), helper.getLevel()), "Metal rod assembled the tool");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void finishedEquipmentMelts(GameTestHelper helper) {
        String[] parts = {"sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"};
        int[] amounts = {200, 100, 100, 100, 100, 600, 800, 600, 400};
        for (String metal : Dragonsteel.METALS) {
            for (int index = 0; index < parts.length; index++) {
                String part = parts[index];
                ItemStack equipment = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("iceandfire:" + metal + "_" + part)));
                HeatingRecipe heating = HeatingRecipe.getRecipe(equipment);
                var heat = HeatCapability.getDefinition(equipment);
                helper.assertTrue(heat != null && Math.abs(heat.heatCapacity()
                    - 2.857143f * amounts[index] / 100) < 0.0001,
                    "Completed equipment has no matching heat definition");
                helper.assertTrue(heating != null && heating.getTemperature() == 2000
                    && heating.assembleFluid(equipment).getAmount() == amounts[index]
                    && heating.assembleFluid(equipment).getFluid() == Dragonsteel.METAL_PARTS.get(metal).fluid().getSource(),
                    "Completed equipment has wrong metal amount or melting point");
                ItemStack worn = equipment.copy();
                if (worn.isDamageableItem()) {
                    worn.setDamageValue(worn.getMaxDamage() - 1);
                }
                ForgingBonusComponent.set(worn, ForgingBonus.EXPERT);
                worn.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.UNBREAKING), 1);
                helper.assertTrue(heating.assembleFluid(worn).getAmount() == amounts[index],
                    "Equipment wear changed recovered metal");
                ItemStack red = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("tfc:metal/" + (part.equals("leggings") ? "greaves" : part) + "/red_steel")));
                helper.assertTrue(HeatingRecipe.getRecipe(red).assembleFluid(red).getAmount() == amounts[index],
                    "Red Steel reference amount differs for " + part);
                helper.assertTrue(ItemSizeManager.get(equipment).getSize(equipment) == ItemSizeManager.get(red).getSize(red)
                    && ItemSizeManager.get(equipment).getWeight(equipment) == ItemSizeManager.get(red).getWeight(red),
                    "Completed equipment size differs from Red Steel " + part);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonsteelCombatAndMining(GameTestHelper helper) {
        var target = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        for (int index = 0; index < Dragonsteel.METALS.length; index++) {
            String metal = Dragonsteel.METALS[index];
            IafTiers tier = switch (index) {
                case 0 -> IafTiers.DRAGONSTEEL_FIRE;
                case 1 -> IafTiers.DRAGONSTEEL_ICE;
                default -> IafTiers.DRAGONSTEEL_LIGHTNING;
            };
            for (String part : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) {
                ItemStack tool = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                    "iceandfire:" + metal + "_" + part)));
                helper.assertTrue(tool.getItem() instanceof TieredItem tiered && tiered.getTier() == tier
                    && tool.getMaxDamage() == tier.getUses(), "IaF Dragonsteel tool stats or tier changed");
                double baseDamage = switch (part) {
                    case "sword" -> 3;
                    case "axe" -> 5;
                    case "shovel" -> 1.5;
                    case "hoe" -> -4;
                    default -> 1;
                };
                double attack = tool.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
                    .filter(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE))
                    .mapToDouble(entry -> entry.modifier().amount()).sum();
                helper.assertTrue(Math.abs(attack - tier.getAttackDamageBonus() - baseDamage) < 0.001,
                    "IaF Dragonsteel attack damage changed for " + part);
            }
            for (String part : List.of("pickaxe", "axe", "shovel")) {
                ItemStack tool = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                    "iceandfire:" + metal + "_" + part)));
                for (String blockId : switch (part) {
                    case "pickaxe" -> List.of("tfc:rock/raw/basalt", "tfc:rock/raw/granite",
                        "tfc:ore/normal_native_copper/basalt", "minecraft:obsidian");
                    case "axe" -> List.of("tfc:wood/log/oak", "tfc:wood/planks/oak");
                    default -> List.of("tfc:soil/dirt/loam", "tfc:soil/gravel/basalt");
                }) {
                    var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId)).defaultBlockState();
                    for (String steel : List.of("red_steel", "blue_steel")) {
                        ItemStack reference = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                            "tfc:metal/" + part + "/" + steel)));
                        helper.assertTrue(!reference.isCorrectToolForDrops(state) || tool.isCorrectToolForDrops(state),
                            "Dragonsteel " + part + " cannot harvest " + blockId + " at " + steel + " tier");
                    }
                }
            }
            for (String part : List.of("helmet", "chestplate", "leggings", "boots")) {
                ItemStack armor = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                    "iceandfire:" + metal + "_" + part)));
                helper.assertTrue(armor.getItem() instanceof DragonSteelArmorItem,
                    "Original IaF armor class was replaced");
                DragonSteelArmorItem original = (DragonSteelArmorItem) armor.getItem();
                int defense = switch (part) {
                    case "helmet" -> IafCommonConfig.INSTANCE.armors.dragonsteelHelmetArmor.getValue();
                    case "chestplate" -> IafCommonConfig.INSTANCE.armors.dragonsteelChestplateArmor.getValue();
                    case "leggings" -> IafCommonConfig.INSTANCE.armors.dragonsteelLeggingsArmor.getValue();
                    default -> IafCommonConfig.INSTANCE.armors.dragonsteelBootsArmor.getValue();
                };
                int durability = switch (part) {
                    case "helmet" -> IafCommonConfig.INSTANCE.armors.dragonsteelHelmetDurability.getValue();
                    case "chestplate" -> IafCommonConfig.INSTANCE.armors.dragonsteelChestplateDurability.getValue();
                    case "leggings" -> IafCommonConfig.INSTANCE.armors.dragonsteelLeggingsDurability.getValue();
                    default -> IafCommonConfig.INSTANCE.armors.dragonsteelBootsDurability.getValue();
                };
                helper.assertTrue(original.getDefense() == defense && armor.getMaxDamage() == durability
                    && original.getEnchantmentValue() == IafCommonConfig.INSTANCE.armors.dragonsteelArmorEnchantability.getValue(),
                    "IaF Dragonsteel armor defense, durability or enchantability changed");
            }
        }
        ItemStack sword = new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:dragonsteel_fire_sword")));
        helper.assertTrue(sword.getItem() instanceof ActivePostHitSwordItem,
            "Original fire ability class was replaced");
        sword.hurtEnemy(target, FakePlayerFactory.getMinecraft(helper.getLevel()));
        helper.assertTrue(target.isOnFire(), "Fire Dragonsteel sword lost its on-hit ability");
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getKey().location().getNamespace().equals("iaftfc")) {
                helper.assertTrue(!(entry.getValue() instanceof TieredItem),
                    "IaFTFC registered an extra finished tool: " + entry.getKey().location());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonsteelElementalCombat(GameTestHelper helper) {
        var attacker = FakePlayerFactory.getMinecraft(helper.getLevel());
        var frozenTarget = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        ItemStack iceSword = new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:dragonsteel_ice_sword")));
        iceSword.hurtEnemy(frozenTarget, attacker);
        helper.assertTrue(frozenTarget.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
            "Ice Dragonsteel sword lost its freezing ability");

        var lightningTarget = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 2));
        var chainedTarget = helper.spawn(EntityType.PIG, new BlockPos(5, 2, 2));
        BlockPos attackerPos = helper.absolutePos(new BlockPos(4, 2, 1));
        attacker.setPos(attackerPos.getX() + 0.5, attackerPos.getY(), attackerPos.getZ() + 0.5);
        float chainedHealth = chainedTarget.getHealth();
        ItemStack lightningSword = new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:dragonsteel_lightning_sword")));
        lightningSword.hurtEnemy(lightningTarget, attacker);
        helper.assertTrue(chainedTarget.getHealth() < chainedHealth,
            "Lightning Dragonsteel sword lost its nearby target ability");

        DragonBaseEntity dragon = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
            ResourceLocation.parse("iceandfire:ice_dragon")).create(helper.getLevel());
        dragon.setAgeInDays(30);
        BlockPos pos = helper.absolutePos(new BlockPos(8, 2, 2));
        dragon.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(dragon);
        float health = dragon.getHealth();
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:dragonsteel_fire_sword"))));
        attacker.attack(dragon);
        helper.assertTrue(dragon.getHealth() < health,
            "Existing Dragonsteel sword cannot damage an IaF dragon");
        dragon.discard();
        helper.succeed();
    }

    private static ItemStack stack(String part, String metal) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iaftfc:metal/" + part + "/" + metal)));
    }

    private static <T> T recipe(GameTestHelper helper, String path, Class<T> type) {
        return type.cast(helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse("iaftfc:" + path)).orElseThrow().value());
    }

    private static <T> T tfcRecipe(GameTestHelper helper, String path, Class<T> type) {
        return type.cast(helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse("tfc:" + path)).orElseThrow().value());
    }
}
