package net.claustra01.iaftfc.gametest;

import java.util.HashSet;
import java.util.Set;
import net.claustra01.iaftfc.common.DragonsteelMetal;
import net.claustra01.iaftfc.registry.ModBlocks;
import net.claustra01.iaftfc.registry.ModFluids;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.blocks.devices.AnvilBlock;
import net.dries007.tfc.common.component.mold.IMold;
import net.dries007.tfc.util.data.FluidHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("iaftfc")
public final class DragonsteelTests {
    static {
        GameTestReports.configure();
    }

    @GameTest(template = "empty")
    public static void registeredMaterials(GameTestHelper test) {
        String[] parts = {"ingot", "double_ingot", "sheet", "double_sheet", "rod", "axe_head", "hoe_head", "pickaxe_head", "shovel_head", "sword_blade", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"};
        Set<ResourceLocation> expectedItems = new HashSet<>();
        Set<ResourceLocation> expectedMaterials = new HashSet<>();
        Set<ResourceLocation> expectedBlocks = new HashSet<>();
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            for (String part : parts) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("iaftfc", "metal/" + part + "/" + metal.id());
                expectedItems.add(id);
                expectedMaterials.add(id);
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                if (stack.isEmpty() || stack.getRarity() != Rarity.EPIC) test.fail("Missing Epic material " + id);
                String group = switch (part) {
                    case "ingot" -> "ingots";
                    case "double_ingot" -> "double_ingots";
                    case "sheet" -> "sheets";
                    case "double_sheet" -> "double_sheets";
                    case "rod" -> "rods";
                    default -> null;
                };
                if (group != null) {
                    for (DragonsteelMetal other : DragonsteelMetal.values()) {
                        boolean present = stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", group + "/" + other.id())));
                        if (present != (other == metal)) test.fail("Wrong material tag " + id);
                    }
                    if (!stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", group)))) test.fail("Missing general tag " + id);
                }
            }
            for (String suffix : new String[] {"", "_slab", "_stairs"}) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("iaftfc", "metal/block/" + metal.id() + suffix);
                expectedBlocks.add(id);
                expectedItems.add(id);
                if (!BuiltInRegistries.BLOCK.containsKey(id) || !BuiltInRegistries.ITEM.containsKey(id)) test.fail("Missing block item " + id);
                if (suffix.isEmpty()) {
                    Block block = BuiltInRegistries.BLOCK.get(id);
                    ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(id));
                    for (String tag : new String[] {"storage_blocks/" + metal.id(), "storage_blocks"}) {
                        ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath("c", tag);
                        if (!block.defaultBlockState().is(TagKey.create(Registries.BLOCK, tagId)) || !item.is(TagKey.create(Registries.ITEM, tagId))) test.fail("Missing storage tag " + tagId);
                    }
                }
            }
            ResourceLocation anvilId = ResourceLocation.fromNamespaceAndPath("iaftfc", "metal/anvil/" + metal.id());
            expectedBlocks.add(anvilId);
            expectedItems.add(anvilId);
            if (!BuiltInRegistries.BLOCK.containsKey(anvilId) || !BuiltInRegistries.ITEM.containsKey(anvilId)) test.fail("Missing anvil " + anvilId);
            if (!BuiltInRegistries.BLOCK.get(anvilId).defaultBlockState().is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("tfc", "anvils")))) test.fail("Missing TFC anvil tag " + anvilId);
            if (((AnvilBlock) BuiltInRegistries.BLOCK.get(anvilId)).getTier() != 7) test.fail("Wrong TFC anvil tier " + anvilId);
            for (String path : new String[] {"metal/" + metal.id(), "metal/flowing_" + metal.id()}) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tfc", path);
                if (!BuiltInRegistries.FLUID.containsKey(id)) test.fail("Missing molten metal " + id);
                Fluid fluid = BuiltInRegistries.FLUID.get(id);
                if (!fluid.is(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "molten_" + metal.id()))) || !fluid.is(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("tfc", "molten_metals")))) test.fail("Missing molten tag " + id);
                for (DragonsteelMetal other : DragonsteelMetal.values()) {
                    if (other != metal && fluid.is(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "molten_" + other.id())))) test.fail("Wrong molten attribute tag " + id);
                }
                if (!path.startsWith("metal/flowing_")) {
                    FluidHeat heat = FluidHeat.get(fluid);
                    if (heat == null || heat.fluid() != fluid || heat.meltTemperature() != 2000 || Math.abs(heat.specificHeatCapacity() - 0.00857f) > 0.000001f) test.fail("TFC does not recognize molten metal " + id);
                }
            }
        }
        for (String part : new String[] {"axe_head", "hoe_head", "pickaxe_head", "shovel_head", "sword_blade"}) expectedItems.add(ResourceLocation.fromNamespaceAndPath("iaftfc", "dragonbone/" + part));
        for (String metal : new String[] {"steel", "black_steel", "red_steel", "blue_steel"}) {
            for (String part : new String[] {"head", "neck", "body", "tail"}) expectedItems.add(ResourceLocation.fromNamespaceAndPath("iaftfc", "dragonarmor_" + metal + "_" + part));
        }
        Set<ResourceLocation> actualItems = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("iaftfc")) {
                actualItems.add(id);
                if (!expectedMaterials.contains(id) && new ItemStack(item).getRarity() == Rarity.EPIC) test.fail("Non-material is Epic " + id);
            }
        }
        Set<ResourceLocation> actualBlocks = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id.getNamespace().equals("iaftfc")) actualBlocks.add(id);
        }
        if (expectedItems.size() != 75 || !actualItems.equals(expectedItems) || expectedBlocks.size() != 12 || !actualBlocks.equals(expectedBlocks)) test.fail("Public registry IDs differ from the 75 items and 12 blocks");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void placedAndRecovered(GameTestHelper test) {
        BlockPos pos = new BlockPos(2, 1, 2);
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            String base = "metal/block/" + metal.id();
            Block solid = ModBlocks.ALL.get(base).get();
            Block slab = ModBlocks.ALL.get(base + "_slab").get();
            Block stairs = ModBlocks.ALL.get(base + "_stairs").get();
            Block anvil = ModBlocks.ALL.get("metal/anvil/" + metal.id()).get();
            for (Block block : new Block[] {solid, slab, stairs, anvil}) {
                BlockState state = block.defaultBlockState();
                test.setBlock(pos, state);
                test.assertBlockPresent(block, pos);
                if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE) || !new ItemStack(Items.NETHERITE_PICKAXE).isCorrectToolForDrops(state) || ItemStack.EMPTY.isCorrectToolForDrops(state)) test.fail("Block requires a pickaxe " + block);
                if (!NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(state)).equals(state)) test.fail("Saved block state changed");
                if (block == anvil) {
                    if (!(test.getBlockEntity(pos) instanceof AnvilBlockEntity) || ((AnvilBlock) anvil).getTier() != 7) test.fail("Anvil is not a Tier 7 TFC anvil");
                }
                int count = Block.getDrops(state, test.getLevel(), test.absolutePos(pos), test.getLevel().getBlockEntity(test.absolutePos(pos)), null, new ItemStack(Items.NETHERITE_PICKAXE)).stream().mapToInt(ItemStack::getCount).sum();
                if (count != 1) test.fail("Wrong normal drop count for " + block);
            }
            for (SlabType type : SlabType.values()) {
                test.setBlock(pos, slab.defaultBlockState().setValue(SlabBlock.TYPE, type));
                test.assertBlockProperty(pos, SlabBlock.TYPE, type);
                int drops = Block.getDrops(test.getBlockState(pos), test.getLevel(), test.absolutePos(pos), null, null, new ItemStack(Items.NETHERITE_PICKAXE)).stream().mapToInt(ItemStack::getCount).sum();
                if (drops != (type == SlabType.DOUBLE ? 2 : 1)) test.fail("Wrong slab drop count");
            }
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (Half half : Half.values()) {
                    for (StairsShape shape : StairsShape.values()) {
                        test.setBlock(pos, stairs.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half).setValue(StairBlock.SHAPE, shape));
                        test.assertBlockProperty(pos, StairBlock.FACING, facing);
                        test.assertBlockProperty(pos, StairBlock.HALF, half);
                        test.assertBlockProperty(pos, StairBlock.SHAPE, shape);
                        BlockState saved = test.getBlockState(pos);
                        if (!NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(saved)).equals(saved)) test.fail("Saved stair orientation changed");
                    }
                }
            }
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                test.setBlock(pos, anvil.defaultBlockState().setValue(AnvilBlock.FACING, facing));
                test.assertBlockProperty(pos, AnvilBlock.FACING, facing);
            }
            ItemStack input = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tfc", "metal/ingot/copper")));
            ((AnvilBlockEntity) test.getBlockEntity(pos)).setInventoryFromOutsideWorld(input, ItemStack.EMPTY, ItemStack.EMPTY);
            AnvilBlockEntity before = (AnvilBlockEntity) test.getBlockEntity(pos);
            CompoundTag saved = before.saveWithFullMetadata(test.getLevel().registryAccess());
            before.setInventoryFromOutsideWorld(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            before.loadWithComponents(saved, test.getLevel().registryAccess());
            test.getLevel().destroyBlock(test.absolutePos(pos), true);
            test.assertItemEntityCountIs(input.getItem(), pos, 2, 1);
            test.assertItemEntityCountIs(anvil.asItem(), pos, 2, 1);
            test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(test.absolutePos(pos)).inflate(2))
                .forEach(ItemEntity::discard);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void moltenMetalTransfers(GameTestHelper test) {
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            int expectedColor = switch (metal) {
                case FIRE -> 0x6E211B;
                case ICE -> 0x6BADC9;
                case LIGHTNING -> 0x4D3A69;
            };
            if (metal.color() != expectedColor) test.fail("Wrong molten metal color " + metal.id());
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfc", "metal/" + metal.id()));
            if (fluid.is(FluidTags.WATER) || ModFluids.METALS.get(metal).getFlowing().is(FluidTags.WATER)) test.fail("Molten metal is water");
            FluidStack metalStack = new FluidStack(fluid, 100);
            ItemStack sourceItem = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tfc", "ceramic/ingot_mold")));
            ItemStack targetItem = sourceItem.copy();
            IMold source = IMold.get(sourceItem);
            IMold target = IMold.get(targetItem);
            if (source == null || target == null || source.getTankCapacity(0) < 100) test.fail("TFC ingot mold has no usable fluid capability");
            int filled = source.fill(metalStack, IFluidHandler.FluidAction.EXECUTE);
            if (filled != 100 || source.getFluidInTank(0).getAmount() != 100) test.fail("Cannot fill TFC mold " + metal.id() + ": fluid=" + BuiltInRegistries.FLUID.getKey(fluid) + ", usable=" + fluid.is(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("tfc", "usable_in_ingot_mold"))) + ", heat=" + (FluidHeat.get(fluid) != null));
            source.setTemperature(2100);
            FluidStack drained = source.drain(100, IFluidHandler.FluidAction.EXECUTE);
            if (target.fill(drained, IFluidHandler.FluidAction.EXECUTE) != 100 || source.getFluidInTank(0).getAmount() != 0 || target.getFluidInTank(0).getAmount() != 100) test.fail("Transfer changed amount");
            if (!FluidStack.isSameFluidSameComponents(target.getFluidInTank(0), metalStack)) test.fail("Transfer changed metal type");
            for (DragonsteelMetal other : DragonsteelMetal.values()) {
                if (other != metal && target.fill(new FluidStack(ModFluids.METALS.get(other).getSource(), 1), IFluidHandler.FluidAction.EXECUTE) != 0) test.fail("Mixed two metals in a mold");
            }
            ItemStack moved = target.getContainer();
            ItemStack saved = ItemStack.parseOptional(test.getLevel().registryAccess(), (CompoundTag) moved.save(test.getLevel().registryAccess()));
            IMold restored = IMold.get(saved);
            if (restored == null || restored.getFluidInTank(0).getAmount() != 100 || !FluidStack.isSameFluidSameComponents(restored.getFluidInTank(0), metalStack)) test.fail("Saved mold changed molten metal");
        }
        test.succeed();
    }
}
