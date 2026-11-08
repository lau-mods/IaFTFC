package net.claustra01.iaftfc.disable;

import net.claustra01.iaftfc.IaFTFC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DisabledContent {
    public static final TagKey<Item> ITEMS = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(IaFTFC.MOD_ID, "disabled"));

    public static boolean isDisabled(ItemStack stack) {
        return stack.is(ITEMS);
    }

    private DisabledContent() {
    }
}
