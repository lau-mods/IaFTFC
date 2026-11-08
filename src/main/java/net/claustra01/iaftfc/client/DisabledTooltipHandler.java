package net.claustra01.iaftfc.client;

import net.claustra01.iaftfc.IaFTFC;
import net.claustra01.iaftfc.disable.DisabledContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = IaFTFC.MOD_ID, value = Dist.CLIENT)
public final class DisabledTooltipHandler {
    @SubscribeEvent
    public static void addDisabledTooltip(ItemTooltipEvent event) {
        if (DisabledContent.isDisabled(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.iaftfc.disabled")
                .withStyle(ChatFormatting.RED));
        }
    }

    private DisabledTooltipHandler() {
    }
}
