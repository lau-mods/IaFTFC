package net.claustra01.iaftfc.client;

import net.claustra01.iaftfc.IaFTFC;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.dries007.tfc.client.extensions.FluidRendererExtension;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = IaFTFC.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DragonsteelClient {
    private static final ResourceLocation MOLTEN_STILL = ResourceLocation.parse("tfc:block/molten_still");
    private static final ResourceLocation MOLTEN_FLOW = ResourceLocation.parse("tfc:block/molten_flow");

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (Dragonsteel metal : Dragonsteel.values()) {
            event.registerFluidType(new FluidRendererExtension(0xFF000000 | metal.color(),
                MOLTEN_STILL, MOLTEN_FLOW, null, null), Dragonsteel.fluid(metal).getType());
        }
    }

    private DragonsteelClient() {
    }
}
