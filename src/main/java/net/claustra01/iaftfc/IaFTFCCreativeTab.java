package net.claustra01.iaftfc;

import java.util.LinkedHashSet;
import net.claustra01.iaftfc.disable.DisabledContent;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IaFTFCCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IaFTFC.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.iaftfc.main"))
            .icon(() -> new ItemStack(Dragonsteel.item(Dragonsteel.FIRE, Metal.ItemType.INGOT).get()))
            .displayItems((parameters, output) -> BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(IaFTFC.MOD_ID))
                .map(item -> new ItemStack(item))
                .filter(stack -> !DisabledContent.isDisabled(stack))
                .forEach(output::accept))
            .build());

    public static void removeDisabled(BuildCreativeModeTabContentsEvent event) {
        var entries = new LinkedHashSet<ItemStack>();
        entries.addAll(event.getParentEntries());
        entries.addAll(event.getSearchEntries());
        for (ItemStack stack : entries) {
            if (DisabledContent.isDisabled(stack)) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    private IaFTFCCreativeTab() {
    }
}
