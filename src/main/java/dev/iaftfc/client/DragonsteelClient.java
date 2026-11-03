package dev.iaftfc.client;

import dev.iaftfc.metal.Dragonsteel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = "iaftfc", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DragonsteelClient {
    @SubscribeEvent
    public static void registerFluids(RegisterClientExtensionsEvent event) {
        for (String metal : Dragonsteel.METALS) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.fromNamespaceAndPath("iaftfc", "block/fluid/metal/" + metal + "_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return ResourceLocation.fromNamespaceAndPath("iaftfc", "block/fluid/metal/" + metal + "_flow");
                }
            }, Dragonsteel.METAL_PARTS.get(metal).fluid().getType());
        }
    }
}
