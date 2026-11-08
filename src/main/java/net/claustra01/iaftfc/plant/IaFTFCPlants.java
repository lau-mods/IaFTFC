package net.claustra01.iaftfc.plant;

import java.util.EnumMap;
import java.util.Map;
import net.claustra01.iaftfc.IaFTFC;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IaFTFCPlants {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IaFTFC.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IaFTFC.MOD_ID);
    public static final Map<IaFTFCPlant, DeferredBlock<PlantBlock>> PLANTS = new EnumMap<>(IaFTFCPlant.class);
    public static final Map<IaFTFCPlant, DeferredItem<BlockItem>> PLANT_ITEMS = new EnumMap<>(IaFTFCPlant.class);

    static {
        for (IaFTFCPlant plant : IaFTFCPlant.values()) {
            String id = "plant/" + plant.id();
            DeferredBlock<PlantBlock> block = BLOCKS.register(id, () -> PlantBlock.create(plant,
                ExtendedProperties.of(BlockBehaviour.Properties.of()
                    .instabreak()
                    .noOcclusion()
                    .randomTicks()
                    .replaceable()
                    .noCollission()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)
                    .offsetType(BlockBehaviour.OffsetType.XZ))
                    .flammable(60, 30)));
            PLANTS.put(plant, block);
            PLANT_ITEMS.put(plant, ITEMS.registerSimpleBlockItem(id, block));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private IaFTFCPlants() {
    }
}
