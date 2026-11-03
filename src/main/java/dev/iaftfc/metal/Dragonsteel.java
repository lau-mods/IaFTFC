package dev.iaftfc.metal;

import java.util.LinkedHashMap;
import java.util.Map;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.MoltenFluidBlock;
import net.dries007.tfc.common.blocks.devices.AnvilBlock;
import net.dries007.tfc.common.fluids.FluidHolder;
import net.dries007.tfc.common.fluids.MoltenFluid;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Dragonsteel {
    public static final String[] METALS = {"dragonsteel_fire", "dragonsteel_ice", "dragonsteel_lightning"};
    public static final String[] PARTS = {
        "ingot", "double_ingot", "sheet", "double_sheet", "rod", "sword_blade",
        "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "unfinished_helmet",
        "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"
    };
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("iaftfc");
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("iaftfc");
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "iaftfc");
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, "iaftfc");
    public static final Map<String, MetalParts> METAL_PARTS = new LinkedHashMap<>();

    static {
        for (String metal : METALS) {
            for (String part : PARTS) {
                ITEMS.register("metal/" + part + "/" + metal, () -> new Item(new Item.Properties()));
            }
            DeferredHolder<Block, Block> block = BLOCKS.register("metal/block/" + metal,
                () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).strength(5, 6)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
            DeferredHolder<Block, AnvilBlock> anvil = BLOCKS.register("metal/anvil/" + metal,
                () -> new AnvilBlock(ExtendedProperties.of(MapColor.METAL).sound(SoundType.METAL)
                    .strength(5, 10).requiresCorrectToolForDrops().blockEntity(TFCBlockEntities.ANVIL), 7));
            ITEMS.register("metal/block/" + metal, () -> new BlockItem(block.get(), new Item.Properties()));
            ITEMS.register("metal/anvil/" + metal, () -> new BlockItem(anvil.get(), new Item.Properties()));
            DeferredHolder<Item, Item> bucket = ITEMS.register("bucket/metal/" + metal,
                () -> new BucketItem(METAL_PARTS.get(metal).fluid().getSource(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
            FluidHolder<BaseFlowingFluid> fluid = RegistrationHelpers.registerFluid(FLUID_TYPES, FLUIDS,
                "metal/" + metal, "metal/" + metal, "metal/flowing_" + metal,
                properties -> properties.block(() -> METAL_PARTS.get(metal).fluidBlock().get())
                    .bucket(bucket).explosionResistance(100),
                () -> new FluidType(FluidType.Properties.create().adjacentPathType(PathType.LAVA)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .lightLevel(15).density(3000).viscosity(6000).temperature(1300)
                    .canConvertToSource(false).canDrown(false).canExtinguish(false)
                    .canHydrate(false).canPushEntity(false).canSwim(false)
                    .supportsBoating(false).fallDistanceModifier(0)
                    .descriptionId("fluid.iaftfc." + metal)),
                MoltenFluid.Source::new, MoltenFluid.Flowing::new);
            DeferredHolder<Block, MoltenFluidBlock> fluidBlock = BLOCKS.register("fluid/metal/" + metal,
                () -> new MoltenFluidBlock(fluid::getSource,
                    Block.Properties.ofFullCopy(Blocks.LAVA).noLootTable()));
            METAL_PARTS.put(metal, new MetalParts(anvil, fluid, fluidBlock));
        }
    }

    public static void addAnvilBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(TFCBlockEntities.ANVIL.get(), METAL_PARTS.values().stream()
            .map(parts -> parts.anvil().get()).toArray(Block[]::new));
    }

    public record MetalParts(DeferredHolder<Block, AnvilBlock> anvil,
        FluidHolder<BaseFlowingFluid> fluid, DeferredHolder<Block, MoltenFluidBlock> fluidBlock) {
    }
}
