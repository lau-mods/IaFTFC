package net.claustra01.iaftfc.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.claustra01.iaftfc.common.DragonsteelMetal;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.AnvilBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "iaftfc");
    public static final Map<String, DeferredHolder<Block, ? extends Block>> ALL = new LinkedHashMap<>();
    public static final List<DeferredHolder<Block, ? extends Block>> ANVILS = new ArrayList<>();

    static {
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            String base = "metal/block/" + metal.id();
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5, 6).requiresCorrectToolForDrops();
            DeferredHolder<Block, Block> solid = add(base, () -> new Block(properties));
            add(base + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(solid.get())));
            add(base + "_stairs", () -> new StairBlock(solid.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(solid.get())));
            ANVILS.add(add("metal/anvil/" + metal.id(), () -> new AnvilBlock(ExtendedProperties.of(MapColor.METAL).strength(5, 6).requiresCorrectToolForDrops().blockEntity(TFCBlockEntities.ANVIL), 7)));
        }
    }

    private static <T extends Block> DeferredHolder<Block, T> add(String id, Supplier<T> block) {
        DeferredHolder<Block, T> holder = BLOCKS.register(id, block);
        ALL.put(id, holder);
        return holder;
    }

    private ModBlocks() {}
}
