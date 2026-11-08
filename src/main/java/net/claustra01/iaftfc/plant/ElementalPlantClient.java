package net.claustra01.iaftfc.plant;

import net.claustra01.iaftfc.IaFTFC;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = IaFTFC.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElementalPlantClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        for (var plant : IaFTFCPlants.PLANTS.values()) {
            ItemBlockRenderTypes.setRenderLayer(plant.get(), RenderType.cutout());
        }
    }

    private ElementalPlantClient() {
    }
}
