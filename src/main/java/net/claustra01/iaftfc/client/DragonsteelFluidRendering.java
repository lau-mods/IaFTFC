package net.claustra01.iaftfc.client;

import net.claustra01.iaftfc.IaFTFC;
import net.claustra01.iaftfc.common.DragonsteelMetal;
import net.claustra01.iaftfc.registry.ModFluids;
import net.dries007.tfc.client.extensions.FluidRendererExtension;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = IaFTFC.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DragonsteelFluidRendering {
    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath("tfc", "block/molten_still");
        ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath("tfc", "block/molten_flow");
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            event.registerFluidType(new FluidRendererExtension(0xFF000000 | metal.color(), still, flowing, null, null), ModFluids.METALS.get(metal).getType());
        }
    }

    private DragonsteelFluidRendering() {}
}
