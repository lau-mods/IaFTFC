package net.claustra01.iaftfc;

import net.claustra01.iaftfc.registry.ModBlocks;
import net.claustra01.iaftfc.registry.ModFluids;
import net.claustra01.iaftfc.registry.ModItems;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

@Mod(IaFTFC.MOD_ID)
public final class IaFTFC {
    public static final String MOD_ID = "iaftfc";

    public IaFTFC(IEventBus bus) {
        ModItems.ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        bus.addListener((BlockEntityTypeAddBlocksEvent event) -> event.modify(
            TFCBlockEntities.ANVIL.get(), ModBlocks.ANVILS.stream().map(holder -> holder.get()).toArray(Block[]::new)));
        ModFluids.TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
    }
}
