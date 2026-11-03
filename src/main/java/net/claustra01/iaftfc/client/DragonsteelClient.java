package net.claustra01.iaftfc.client;

import net.claustra01.iaftfc.IaFTFC;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.claustra01.iaftfc.metal.DragonsteelMetal;
import net.dries007.tfc.client.extensions.FluidRendererExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@EventBusSubscriber(modid = IaFTFC.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DragonsteelClient {
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath("tfc", "block/molten_still");
    private static final ResourceLocation FLOW = ResourceLocation.fromNamespaceAndPath("tfc", "block/molten_flow");

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), Dragonsteel.METAL_PARTS.values().stream()
            .map(parts -> parts.fluid().getSource().getBucket()).toArray(ItemLike[]::new));
    }

    @SubscribeEvent
    public static void registerFluids(RegisterClientExtensionsEvent event) {
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            event.registerFluidType(new FluidRendererExtension(metal.color(), STILL, FLOW, null, null),
                Dragonsteel.METAL_PARTS.get(metal.id()).fluid().getType());
        }
    }
}
